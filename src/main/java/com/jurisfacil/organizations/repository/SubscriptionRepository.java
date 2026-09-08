package com.jurisfacil.organizations.repository;

import com.jurisfacil.organizations.model.entity.SubscriptionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Optional<SubscriptionEntity> findByOrganizationId(UUID organizationId);
}
