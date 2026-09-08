package com.jurisfacil.organizations.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.iam.service.RecoveryService;
import com.jurisfacil.iam.service.RefreshService;
import com.jurisfacil.organizations.controller.dto.request.AcceptInviteRequest;
import com.jurisfacil.organizations.controller.dto.response.AcceptInviteResponse;
import com.jurisfacil.organizations.controller.dto.response.MemberResponse;
import com.jurisfacil.organizations.mapper.MembershipMapper;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.service.InviteAcceptanceService;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InviteAcceptanceServiceImpl implements InviteAcceptanceService {

    private static final long ACCESS_TOKEN_SECONDS = 900;
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshService refreshService;
    private final MembershipMapper membershipMapper;

    @Override
    public InviteAcceptanceService.AcceptedInvite accept(AcceptInviteRequest request, String ipAddress, String userAgent) {
        MembershipEntity membership = membershipRepository.findByInviteTokenHash(RecoveryService.sha256(request.token()))
                .orElseThrow(InviteAcceptanceException::new);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (membership.getStatus() != MembershipStatus.PENDING
                || membership.getInviteExpiresAt() == null
                || !membership.getInviteExpiresAt().isAfter(now)) {
            throw new InviteAcceptanceException();
        }
        OrganizationEntity organization = organizationRepository.findById(membership.getOrganizationId())
                .orElseThrow(InviteAcceptanceException::new);
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new InviteAcceptanceException(ErrorCode.TENANT_SUSPENDED, "Workspace is suspended.");
        }
        UserEntity user = userRepository.findById(membership.getUserId())
                .orElseThrow(InviteAcceptanceException::new);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordSetAt(now);
        user.setStatus(com.jurisfacil.iam.model.enums.UserStatus.ACTIVE);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setAcceptedAt(now);
        membership.setInviteTokenHash(null);
        membership.setInviteExpiresAt(null);
        userRepository.save(user);
        membershipRepository.save(membership);
        RefreshService.IssuedSession session = refreshService.issueSession(user, false,
                membership.getOrganizationId(), ipAddress, userAgent);
        String accessToken = jwtService.issueAccessToken(new JwtClaims(user.getId(), user.getName(), user.getEmail(),
                membership.getOrganizationId(), membership.getRole().name(), List.of(), List.of(), now.toInstant(),
                now.plusSeconds(ACCESS_TOKEN_SECONDS).toInstant(), java.util.UUID.randomUUID().toString()));
        MemberResponse member = membershipMapper.toResponse(membership, user);
        return new InviteAcceptanceService.AcceptedInvite(
                new AcceptInviteResponse(accessToken, "Bearer", ACCESS_TOKEN_SECONDS, member),
                session.refreshToken());
    }

    public static class InviteAcceptanceException extends AbstractBusinessException {
        public InviteAcceptanceException() {
            this(ErrorCode.INVITE_INVALID_OR_EXPIRED, "Invite is invalid or expired.");
        }

        public InviteAcceptanceException(ErrorCode code, String detail) {
            super(code.name(), detail);
        }
    }
}
