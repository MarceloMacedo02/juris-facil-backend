package com.jurisfacil.processes.controller.dto.response;

import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProcessDetailResponse(
        UUID id, String cnjNumber, String title, String court, String courtUnit, String location,
        String status, String notes, boolean secretJustice, BigDecimal claimValue, String area,
        ResponsibleMember responsibleMember, List<Party> parties, LastMovement lastMovement,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {

    public static ProcessDetailResponse from(
            ProcessEntity process, ResponsibleMember responsibleMember, List<ProcessPartyEntity> parties) {
        return new ProcessDetailResponse(
                process.getId(), process.getCnjNumber(), process.getTitle(), process.getCourt(),
                process.getCourtUnit(), process.getLocation(), process.getStatus().name(), process.getNotes(),
                process.isSecretJustice(), process.getClaimValue(), process.getArea(), responsibleMember,
                parties.stream().map(Party::from).toList(), null, process.getCreatedAt(),
                process.getUpdatedAt(), process.getVersion());
    }

    public record ResponsibleMember(UUID id, String name) {
    }

    public record Party(UUID id, String name, String role, boolean isClient, OffsetDateTime createdAt) {
        static Party from(ProcessPartyEntity party) {
            return new Party(party.getId(), party.getName(), party.getRole().name(), party.isClient(),
                    party.getCreatedAt());
        }
    }

    public record LastMovement(UUID id, String title, OffsetDateTime occurredAt) {
    }
}
