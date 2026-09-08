package com.jurisfacil.organizations.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.service.impl.OrganizationsServiceImpl.MembersBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OwnerInvariantGuard {

    private final MembershipRepository membershipRepository;

    public void assertOwnerInvariant(UUID organizationId, UUID ignoredMembershipId) {
        if (membershipRepository.countByOrganizationIdAndRoleAndStatusAndIdNot(
                organizationId, MembershipRole.OWNER, MembershipStatus.ACTIVE, ignoredMembershipId) == 0) {
            throw new MembersBusinessException(ErrorCode.OWNER_INVARIANT_VIOLATION,
                    "The organization must retain an active owner.");
        }
    }
}
