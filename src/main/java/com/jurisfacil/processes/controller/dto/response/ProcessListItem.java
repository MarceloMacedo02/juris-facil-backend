package com.jurisfacil.processes.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcessListItem(
        UUID id,
        String cnjNumber,
        String title,
        String court,
        String courtUnit,
        String status,
        String clientName,
        OffsetDateTime lastMovementDate,
        OffsetDateTime updatedAt) {
}
