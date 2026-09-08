package com.jurisfacil.organizations.service.impl;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.service.RecoveryService;
import com.jurisfacil.organizations.controller.dto.request.CreateOrganizationRequest;
import com.jurisfacil.organizations.controller.dto.response.CreateOrganizationResponse;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.model.enums.SubscriptionStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.organizations.service.AdminOrganizationService;
import com.jurisfacil.organizations.service.EntitlementSeeder;
import com.jurisfacil.shared.email.EmailGateway;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;
import com.jurisfacil.shared.util.CnpjCpfValidator;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

    private static final int ACTIVATION_TOKEN_BYTES = 32;
    private static final int ACTIVATION_TTL_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementSeeder entitlementSeeder;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final EmailGateway emailGateway;

    @Autowired(required = false)
    private AuditService auditService;

    @Override
    public CreateOrganizationResponse createOrganization(CreateOrganizationRequest request) {
        CnpjCpfValidator.assertValid(request.cnpjCpf());
        String email = request.ownerEmail().trim().toLowerCase();
        String document = CnpjCpfValidator.normalize(request.cnpjCpf());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new AdminOrganizationException(ErrorCode.EMAIL_ALREADY_IN_USE, "Email is already in use.");
        }
        if (organizationRepository.existsByCnpjCpf(document)) {
            throw new AdminOrganizationException(ErrorCode.CNPJ_ALREADY_REGISTERED,
                    "CNPJ/CPF is already registered.");
        }

        PlanTier plan = parsePlan(request.planTier());
        OrganizationEntity organization = organizationRepository.save(OrganizationEntity.builder()
                .name(request.name().trim())
                .cnpjCpf(document)
                .contactEmail(request.contactEmail().trim().toLowerCase())
                .phone(request.phone())
                .city(request.city())
                .state(request.state())
                .status(OrganizationStatus.ACTIVE)
                .build());
        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(organization.getId())
                .planTier(plan)
                .status(SubscriptionStatus.ACTIVE)
                .build());
        entitlementSeeder.seed(organization.getId(), plan);

        UserEntity owner = userRepository.save(UserEntity.builder()
                .name(request.ownerName().trim())
                .email(email)
                .status(UserStatus.INACTIVE)
                .build());
        String activationToken = generateToken();
        MembershipEntity membership = membershipRepository.save(MembershipEntity.builder()
                .organizationId(organization.getId())
                .userId(owner.getId())
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.PENDING)
                .inviteTokenHash(RecoveryService.sha256(activationToken))
                .inviteExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(ACTIVATION_TTL_DAYS))
                .build());
        emailGateway.send(email, "Organization activation", activationToken);
        if (auditService != null) {
            auditService.record(AuditEvent.builder().action(AuditAction.ORGANIZATION_CREATED)
                    .organizationId(organization.getId()).resourceType("ORGANIZATION")
                    .resourceId(organization.getId().toString()).build());
        }
        return new CreateOrganizationResponse(organization.getId(), membership.getId(), activationToken);
    }

    private PlanTier parsePlan(String value) {
        try {
            return PlanTier.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AdminOrganizationException(ErrorCode.VALIDATION_FAILED, "Invalid plan tier.");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[ACTIVATION_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static final class AdminOrganizationException extends AbstractBusinessException {
        public AdminOrganizationException(ErrorCode code, String detail) {
            super(code.name(), detail);
        }
    }
}
