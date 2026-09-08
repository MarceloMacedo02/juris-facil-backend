package com.jurisfacil.processes.service;

import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.processes.controller.dto.request.CreateProcessRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessMutationValidator {

    private static final String CNJ_FORMAT = "\\d{20}|\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}";

    private final MembershipRepository membershipRepository;

    public String normalizeCnj(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.matches(CNJ_FORMAT)) {
            throw new ProcessCreateService.InvalidCnjException();
        }
        return value.replaceAll("\\D", "");
    }

    public void validateParties(List<CreateProcessRequest.PartyInput> parties) {
        if (parties.stream().filter(CreateProcessRequest.PartyInput::isClient).count() > 1) {
            throw new ProcessCreateService.MultipleClientPartiesException();
        }
    }

    public void validateResponsible(UUID organizationId, UUID responsibleMemberId) {
        if (responsibleMemberId != null && !membershipRepository.findById(responsibleMemberId)
                .filter(member -> organizationId.equals(member.getOrganizationId()))
                .filter(member -> member.getRole() == MembershipRole.LAWYER)
                .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
                .isPresent()) {
            throw new ProcessCreateService.InvalidResponsibleMemberException();
        }
    }
}
