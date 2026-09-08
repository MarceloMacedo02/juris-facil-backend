package com.jurisfacil.processes.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        OffsetDateTime movementDate,
        String movementType,
        String title,
        String description,
        String source,
        String authorName,
        OffsetDateTime createdAt) {
}
