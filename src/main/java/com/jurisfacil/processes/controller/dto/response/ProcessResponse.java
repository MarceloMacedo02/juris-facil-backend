package com.jurisfacil.processes.controller.dto.response;

import com.jurisfacil.processes.model.entity.ProcessEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcessResponse(UUID id, String cnjNumber, String title, String status, OffsetDateTime createdAt) {

    public static ProcessResponse from(ProcessEntity process) {
        return new ProcessResponse(process.getId(), process.getCnjNumber(), process.getTitle(),
                process.getStatus().name(), process.getCreatedAt());
    }
}
