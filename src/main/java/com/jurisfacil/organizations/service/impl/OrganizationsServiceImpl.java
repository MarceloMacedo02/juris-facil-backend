package com.jurisfacil.organizations.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.RefreshSessionRepository;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.organizations.controller.dto.request.InviteMemberRequest;
import com.jurisfacil.organizations.controller.dto.request.UpdateRoleRequest;
import com.jurisfacil.organizations.controller.dto.response.InviteMemberResponse;
import com.jurisfacil.organizations.controller.dto.response.MemberResponse;
import com.jurisfacil.organizations.mapper.MembershipMapper;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.organizations.service.OrganizationsService;
import com.jurisfacil.organizations.service.OwnerInvariantGuard;
import com.jurisfacil.shared.email.EmailGateway;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationsServiceImpl implements OrganizationsService {

    private static final int INVITE_TOKEN_BYTES = 32;
    private static final int INVITE_TTL_DAYS = 7;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailGateway emailGateway;
    private final MembershipMapper membershipMapper;
    private final OwnerInvariantGuard ownerInvariantGuard;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID organizationId) {
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(membership -> membershipMapper.toResponse(membership, user(membership.getUserId())))
                .toList();
    }

    @Override
    public InviteMemberResponse invite(UUID organizationId, InviteMemberRequest request) {
        MembershipRole role = parseInvitableRole(request.role());
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .name(request.name().trim())
                        .email(request.email().trim().toLowerCase())
                        .passwordHash(passwordEncoder.encode(generateToken()))
                        .status(UserStatus.INACTIVE)
                        .build()));
        if (membershipRepository.findByUserIdAndStatusNot(user.getId(), MembershipStatus.INACTIVE).stream()
                .findAny().isPresent()) {
            throw new MembersBusinessException(ErrorCode.EMAIL_ALREADY_IN_USE, "Email is already linked to a workspace.");
        }
        int limit = planLimit(subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new MembersBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Subscription not found."))
                .getPlanTier());
        if (membershipRepository.countByOrganizationIdAndStatusNot(organizationId, MembershipStatus.INACTIVE) >= limit) {
            throw new MembersBusinessException(ErrorCode.PLAN_LIMIT_EXCEEDED, "Workspace member limit reached.");
        }
        MembershipEntity membership = membershipRepository.findByUserIdAndOrganizationId(user.getId(), organizationId)
                .orElseGet(() -> MembershipEntity.builder().userId(user.getId()).organizationId(organizationId).build());
        String token = generateToken();
        membership.setRole(role);
        membership.setStatus(MembershipStatus.PENDING);
        membership.setInviteTokenHash(sha256(token));
        membership.setInviteExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(INVITE_TTL_DAYS));
        membership = membershipRepository.save(membership);
        emailGateway.send(user.getEmail(), "Workspace invitation", token);
        audit(AuditAction.MEMBER_INVITED, organizationId, membership.getId(), user.getId());
        return new InviteMemberResponse(membership.getId(), membership.getStatus().name(), token);
    }

    @Override
    public MemberResponse updateRole(UUID organizationId, UUID membershipId, UpdateRoleRequest request) {
        MembershipEntity membership = membership(organizationId, membershipId);
        if (membership.getRole() == MembershipRole.OWNER) {
            ownerInvariantGuard.assertOwnerInvariant(organizationId, membershipId);
        }
        membership.setRole(parseInvitableRole(request.role()));
        MemberResponse response = response(membershipRepository.save(membership));
        audit(AuditAction.MEMBER_ROLE_CHANGED, organizationId, membershipId, membership.getUserId());
        return response;
    }

    @Override
    public void deactivate(UUID organizationId, UUID membershipId) {
        MembershipEntity membership = membership(organizationId, membershipId);
        if (membership.getRole() == MembershipRole.OWNER) {
            ownerInvariantGuard.assertOwnerInvariant(organizationId, membershipId);
        }
        membership.setStatus(MembershipStatus.INACTIVE);
        membership.setDeactivatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        membershipRepository.save(membership);
        refreshSessionRepository.findAllByUser_IdAndRevokedAtIsNull(membership.getUserId()).forEach(session -> {
            if (organizationId.equals(session.getOrganizationId())) {
                session.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
                refreshSessionRepository.save(session);
            }
        });
        audit(AuditAction.MEMBER_DEACTIVATED, organizationId, membershipId, membership.getUserId());
    }

    @Override
    public MemberResponse resendInvite(UUID organizationId, UUID membershipId) {
        MembershipEntity membership = membership(organizationId, membershipId);
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new MembersBusinessException(ErrorCode.VALIDATION_FAILED, "Only pending invitations can be resent.");
        }
        String token = generateToken();
        membership.setInviteTokenHash(sha256(token));
        membership.setInviteExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(INVITE_TTL_DAYS));
        membershipRepository.save(membership);
        UserEntity user = user(membership.getUserId());
        emailGateway.send(user.getEmail(), "Workspace invitation", token);
        return membershipMapper.toResponse(membership, user);
    }

    @Override
    public MemberResponse transferOwnership(UUID organizationId, UUID membershipId) {
        MembershipEntity target = membership(organizationId, membershipId);
        if (target.getStatus() != MembershipStatus.ACTIVE) {
            throw new MembersBusinessException(ErrorCode.VALIDATION_FAILED, "Ownership target must be active.");
        }
        MembershipEntity owner = membershipRepository.findByOrganizationIdAndRoleAndStatus(
                organizationId, MembershipRole.OWNER, MembershipStatus.ACTIVE)
                .orElseThrow(this::ownerInvariant);
        if (owner.getId().equals(target.getId())) return response(target);
        owner.setRole(MembershipRole.ADMIN);
        target.setRole(MembershipRole.OWNER);
        membershipRepository.save(owner);
        MemberResponse response = response(membershipRepository.save(target));
        audit(AuditAction.OWNERSHIP_TRANSFERRED, organizationId, membershipId, target.getUserId());
        return response;
    }

    private void audit(AuditAction action, UUID organizationId, UUID resourceId, UUID actorId) {
        if (auditService != null) {
            auditService.record(AuditEvent.builder().action(action).actorId(actorId)
                    .organizationId(organizationId).resourceType("MEMBERSHIP")
                    .resourceId(resourceId == null ? null : resourceId.toString()).build());
        }
    }

    private MemberResponse response(MembershipEntity membership) {
        return membershipMapper.toResponse(membership, user(membership.getUserId()));
    }

    private MembershipEntity membership(UUID organizationId, UUID membershipId) {
        return membershipRepository.findById(membershipId)
                .filter(item -> organizationId.equals(item.getOrganizationId()))
                .orElseThrow(() -> new MembersBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Member not found."));
    }

    private UserEntity user(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MembersBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Member user not found."));
    }

    private MembershipRole parseInvitableRole(String role) {
        try {
            MembershipRole parsed = MembershipRole.valueOf(role);
            if (parsed == MembershipRole.OWNER) throw ownerInvariant();
            return parsed;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new MembersBusinessException(ErrorCode.VALIDATION_FAILED, "Invalid member role.");
        }
    }

    private int planLimit(PlanTier tier) {
        return switch (tier) {
            case BASICO -> 2;
            case PROFISSIONAL -> 5;
            case ENTERPRISE -> 20;
        };
    }

    private String generateToken() {
        byte[] bytes = new byte[INVITE_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private MembersBusinessException ownerInvariant() {
        return new MembersBusinessException(ErrorCode.OWNER_INVARIANT_VIOLATION, "The active owner cannot be changed.");
    }

    public static final class MembersBusinessException extends AbstractBusinessException {
        public MembersBusinessException(ErrorCode code, String detail) {
            super(code.name(), detail);
        }
    }
}
