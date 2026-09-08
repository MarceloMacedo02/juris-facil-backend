package com.jurisfacil.processes.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.processes.model.entity.ProcessMovementEntity;
import com.jurisfacil.processes.model.enums.MovementSource;
import com.jurisfacil.processes.model.enums.MovementType;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovementMapperTest {

    @Test
    void mapsMovementContractWithoutExposingEntity() {
        UUID id = UUID.randomUUID();
        OffsetDateTime movementDate = OffsetDateTime.parse("2026-02-01T10:00:00Z");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-02-01T11:00:00Z");
        ProcessMovementEntity movement = ProcessMovementEntity.builder()
                .id(id)
                .movementDate(movementDate)
                .movementType(MovementType.DECISAO)
                .title("Decisão")
                .description("Descrição")
                .source(MovementSource.MANUAL)
                .createdAt(createdAt)
                .build();

        var response = new MovementMapper().toResponse(movement, "Dra. Ana");

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.movementDate()).isEqualTo(movementDate);
        assertThat(response.movementType()).isEqualTo("DECISAO");
        assertThat(response.source()).isEqualTo("MANUAL");
        assertThat(response.authorName()).isEqualTo("Dra. Ana");
    }
}
