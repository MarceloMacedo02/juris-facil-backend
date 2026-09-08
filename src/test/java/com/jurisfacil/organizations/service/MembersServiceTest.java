package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.model.enums.UserStatus;
import com.jurisfacil.iam.repository.RefreshSessionRepository;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.organizations.controller.dto.request.InviteMemberRequest;
import com.jurisfacil.organizations.controller.dto.response.InviteMemberResponse;
import com.jurisfacil.organizations.mapper.MembershipMapper;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.organizations.service.impl.OrganizationsServiceImpl;
import com.jurisfacil.organizations.service.OwnerInvariantGuard;
import com.jurisfacil.shared.email.EmailGateway;
import com.jurisfacil.shared.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class MembersServiceTest {

    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock RefreshSessionRepository refreshSessionRepository;
    @Mock EmailGateway emailGateway;
    private OrganizationsServiceImpl service;
    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OrganizationsServiceImpl(membershipRepository, userRepository, subscriptionRepository,
                refreshSessionRepository, new BCryptPasswordEncoder(), emailGateway, new MembershipMapper(),
                mock(OwnerInvariantGuard.class));
    }

    @Test
    void invitesNewUserWithOpaqueTokenAndPendingMembership() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("new@example.com")
                .name("New User").status(UserStatus.INACTIVE).build();
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(membershipRepository.findByUserIdAndStatusNot(user.getId(), MembershipStatus.INACTIVE))
                .thenReturn(java.util.List.of());
        when(subscriptionRepository.findByOrganizationId(organizationId))
                .thenReturn(Optional.of(SubscriptionEntity.builder().planTier(PlanTier.BASICO).build()));
        when(membershipRepository.countByOrganizationIdAndStatusNot(organizationId, MembershipStatus.INACTIVE))
                .thenReturn(1L);
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), organizationId))
                .thenReturn(Optional.empty());
        when(membershipRepository.save(any(MembershipEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InviteMemberResponse result = service.invite(organizationId,
                new InviteMemberRequest("new@example.com", "New User", "LAWYER"));

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.inviteToken()).isNotBlank();
        verify(emailGateway).send("new@example.com", "Workspace invitation", result.inviteToken());
    }

    @Test
    void rejectsUserWithAnotherActiveMembership() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("used@example.com").build();
        when(userRepository.findByEmailIgnoreCase("used@example.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatusNot(user.getId(), MembershipStatus.INACTIVE))
                .thenReturn(java.util.List.of(MembershipEntity.builder().status(MembershipStatus.ACTIVE).build()));

        assertThatThrownBy(() -> service.invite(organizationId,
                new InviteMemberRequest("used@example.com", "Used User", "LAWYER")))
                .isInstanceOf(OrganizationsServiceImpl.MembersBusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.EMAIL_ALREADY_IN_USE.name());
    }
}
