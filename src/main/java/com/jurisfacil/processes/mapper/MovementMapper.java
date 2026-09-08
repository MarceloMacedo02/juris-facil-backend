package com.jurisfacil.processes.mapper;

import com.jurisfacil.processes.controller.dto.response.MovementResponse;
import com.jurisfacil.processes.model.entity.ProcessMovementEntity;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    public MovementResponse toResponse(ProcessMovementEntity movement, String authorName) {
        return new MovementResponse(
                movement.getId(),
                movement.getMovementDate(),
                movement.getMovementType().name(),
                movement.getTitle(),
                movement.getDescription(),
                movement.getSource().name(),
                authorName,
                movement.getCreatedAt());
    }
}
