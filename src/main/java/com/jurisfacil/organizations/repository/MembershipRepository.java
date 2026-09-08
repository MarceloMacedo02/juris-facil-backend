package com.jurisfacil.organizations.repository;

import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface MembershipRepository extends JpaRepository<MembershipEntity, UUID> {

    Optional<MembershipEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    List<MembershipEntity> findByOrganizationIdAndStatus(UUID organizationId, MembershipStatus status);

    List<MembershipEntity> findByOrganizationId(UUID organizationId);

    List<MembershipEntity> findByOrganizationIdAndIdIn(UUID organizationId, Collection<UUID> ids);

    long countByOrganizationIdAndStatusNot(UUID organizationId, MembershipStatus status);

    List<MembershipEntity> findByUserIdAndStatusNot(UUID userId, MembershipStatus status);

    boolean existsByOrganizationIdAndRoleAndStatus(
            UUID organizationId, MembershipRole role, MembershipStatus status);

    Optional<MembershipEntity> findByOrganizationIdAndRoleAndStatus(
            UUID organizationId, MembershipRole role, MembershipStatus status);

    long countByOrganizationIdAndRoleAndStatusAndIdNot(UUID organizationId, MembershipRole role,
            MembershipStatus status, UUID membershipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MembershipEntity> findByInviteTokenHash(String inviteTokenHash);
}
