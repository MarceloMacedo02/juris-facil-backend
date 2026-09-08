package com.jurisfacil.organizations.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.model.enums.PlanTier;
import com.jurisfacil.organizations.model.enums.SubscriptionStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.repository.SubscriptionRepository;
import com.jurisfacil.support.BaseIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class OrganizationEntityPersistenceIT extends BaseIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ModuleEntitlementRepository entitlementRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void persistsOrganizationAggregateMappingsAndEnumValues() {
        UUID userId = UUID.randomUUID();
        jdbcInsertUser(userId);
        OrganizationEntity organization = organizationRepository.saveAndFlush(OrganizationEntity.builder()
                .name("Persisted Organization")
                .cnpjCpf("33.333.333/0001-33")
                .contactEmail("organization@example.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        SubscriptionEntity subscription = subscriptionRepository.saveAndFlush(SubscriptionEntity.builder()
                .organizationId(organization.getId())
                .planTier(PlanTier.PROFISSIONAL)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodEnd(LocalDate.of(2026, 12, 31))
                .build());
        ModuleEntitlementEntity entitlement = entitlementRepository.saveAndFlush(ModuleEntitlementEntity.builder()
                .organizationId(organization.getId())
                .moduleCode("PROCESS")
                .enabled(true)
                .build());
        MembershipEntity membership = membershipRepository.saveAndFlush(MembershipEntity.builder()
                .organizationId(organization.getId())
                .userId(userId)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        assertThat(organizationRepository.findById(organization.getId())).get()
                .extracting(OrganizationEntity::getStatus, OrganizationEntity::getVersion)
                .containsExactly(OrganizationStatus.ACTIVE, 0L);
        assertThat(subscriptionRepository.findByOrganizationId(organization.getId())).get()
                .extracting(SubscriptionEntity::getPlanTier)
                .isEqualTo(PlanTier.PROFISSIONAL);
        assertThat(entitlementRepository.findByOrganizationIdAndModuleCode(organization.getId(), "PROCESS"))
                .get().extracting(ModuleEntitlementEntity::isEnabled).isEqualTo(true);
        assertThat(membershipRepository.findByUserIdAndOrganizationId(userId, organization.getId())).get()
                .extracting(MembershipEntity::getRole, MembershipEntity::getStatus)
                .containsExactly(MembershipRole.OWNER, MembershipStatus.ACTIVE);
        assertThat(subscription.getId()).isNotNull();
        assertThat(entitlement.getId()).isNotNull();
        assertThat(membership.getId()).isNotNull();
    }

    private void jdbcInsertUser(UUID userId) {
        jdbcTemplate.update("INSERT INTO users (user_id, email, name) VALUES (?, ?, ?)",
                userId, "organization-" + userId + "@example.com", "Organization User");
    }
}
