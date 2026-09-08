package com.jurisfacil.iam.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.security.JwtClaims;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.iam.service.AuthService;
import com.jurisfacil.iam.service.RefreshService;
import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.service.TenantStatusService;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long ACCESS_TOKEN_SECONDS = 900;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshService refreshService;
    private final MembershipRepository membershipRepository;
    private final TenantStatusService tenantStatusService;
    private final AuditService auditService;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, RefreshService refreshService,
            MembershipRepository membershipRepository, TenantStatusService tenantStatusService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshService = refreshService;
        this.membershipRepository = membershipRepository;
        this.tenantStatusService = tenantStatusService;
        this.auditService = auditService;
    }

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, RefreshService refreshService) {
        this(userRepository, passwordEncoder, jwtService, refreshService, null, null, null);
    }

    @Override
    public UserEntity authenticate(String email, String password) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException();
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            LOGGER.info("AUDIT_LOGIN_FAILED userId={}", user.getId());
            if (auditService != null) auditService.record(AuditEvent.builder().action(AuditAction.LOGIN_FAILED)
                    .actorId(user.getId()).resourceType("AUTH").build());
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Override
    public AuthenticatedUser login(String email, String password, boolean rememberMe, String ipAddress,
            String userAgent) {
        UserEntity user = authenticate(email, password);
        if (user.getPlatformRole() != null) {
            LOGGER.info("AUDIT_LOGIN_FAILED userId={}", user.getId());
            if (auditService != null) auditService.record(AuditEvent.builder().action(AuditAction.LOGIN_FAILED)
                    .actorId(user.getId()).resourceType("AUTH").build());
            throw new InvalidCredentialsException();
        }

        UUID organizationId = null;
        String role = null;
        if (membershipRepository != null) {
            MembershipEntity membership = membershipRepository.findByUserIdAndStatusNot(user.getId(), MembershipStatus.INACTIVE)
                    .stream().findFirst().orElse(null);
            if (membership != null) {
                organizationId = membership.getOrganizationId();
                role = membership.getRole().name();
                if (tenantStatusService != null && !tenantStatusService.isActive(organizationId)) {
                    throw new TenantSuspendedException();
                }
            }
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String accessToken = jwtService.issueAccessToken(new JwtClaims(
                user.getId(), user.getName(), user.getEmail(), organizationId, role, List.of(), List.of(),
                now.toInstant(), now.plusSeconds(ACCESS_TOKEN_SECONDS).toInstant(), UUID.randomUUID().toString()));
        RefreshService.IssuedSession session = refreshService.issueSession(user, rememberMe, organizationId, ipAddress,
                userAgent);
        if (auditService != null) auditService.record(AuditEvent.builder().action(AuditAction.LOGIN_SUCCESS)
                .actorId(user.getId()).organizationId(organizationId).resourceType("AUTH").build());
        return new AuthenticatedUser(user, accessToken, session.refreshToken());
    }

    public static class InvalidCredentialsException extends AbstractBusinessException {
        public InvalidCredentialsException() {
            super("INVALID_CREDENTIALS", "Invalid credentials.");
        }
    }

    public static class AccessDeniedException extends AbstractBusinessException {
        public AccessDeniedException() {
            super("ACCESS_DENIED", "Access is denied.");
        }
    }

    public static class TenantSuspendedException extends AbstractBusinessException {
        public TenantSuspendedException() {
            super(ErrorCode.TENANT_SUSPENDED.name(), "Workspace is suspended.");
        }
    }
}
