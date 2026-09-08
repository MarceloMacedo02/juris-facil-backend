package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.organizations.controller.dto.request.CreateOrganizationRequest;
import com.jurisfacil.organizations.controller.dto.response.CreateOrganizationResponse;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.organizations.service.impl.AdminOrganizationServiceImpl;
import com.jurisfacil.shared.email.EmailGateway;
import com.jurisfacil.shared.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock EntitlementSeeder entitlementSeeder;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock EmailGateway emailGateway;
    private AdminOrganizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminOrganizationServiceImpl(organizationRepository, subscriptionRepository,
                entitlementSeeder, membershipRepository, userRepository, emailGateway);
    }

    @Test
    void createsOrganizationSubscriptionEntitlementAndPendingOwnerInOneFlow() {
        UUID organizationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.empty());
        when(organizationRepository.existsByCnpjCpf("04252011000110")).thenReturn(false);
        when(organizationRepository.save(any(OrganizationEntity.class))).thenAnswer(invocation -> {
            OrganizationEntity value = invocation.getArgument(0);
            value.setId(organizationId);
            return value;
        });
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity value = invocation.getArgument(0);
            value.setId(ownerId);
            return value;
        });
        when(membershipRepository.save(any(MembershipEntity.class))).thenAnswer(invocation -> {
            MembershipEntity value = invocation.getArgument(0);
            value.setId(membershipId);
            return value;
        });

        CreateOrganizationResponse result = service.createOrganization(new CreateOrganizationRequest(
                "Escritório Teste", "04.252.011/0001-10", "contato@example.com", "Owner Teste",
                "owner@example.com", "PROFISSIONAL", null, "Fortaleza", "CE"));

        assertThat(result.organizationId()).isEqualTo(organizationId);
        assertThat(result.ownerMembershipId()).isEqualTo(membershipId);
        assertThat(result.activationToken()).isNotBlank();
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
        verify(entitlementSeeder).seed(organizationId, PlanTier.PROFISSIONAL);
        verify(emailGateway).send("owner@example.com", "Organization activation", result.activationToken());
    }

    @Test
    void rejectsDuplicateOwnerEmailBeforeWritingOrganization() {
        when(userRepository.findByEmailIgnoreCase("owner@example.com"))
                .thenReturn(Optional.of(UserEntity.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> service.createOrganization(new CreateOrganizationRequest(
                "Escritório Teste", "04.252.011/0001-10", "contato@example.com", "Owner Teste",
                "owner@example.com", "PROFISSIONAL", null, null, null)))
                .extracting("code").isEqualTo(ErrorCode.EMAIL_ALREADY_IN_USE.name());
    }

    @Test
    void rejectsInvalidDocumentWithContractValidationCode() {
        assertThatThrownBy(() -> service.createOrganization(new CreateOrganizationRequest(
                "Escritório Teste", "04.252.011/0001-11", "contato@example.com", "Owner Teste",
                "owner@example.com", "PROFISSIONAL", null, null, null)))
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }
}
