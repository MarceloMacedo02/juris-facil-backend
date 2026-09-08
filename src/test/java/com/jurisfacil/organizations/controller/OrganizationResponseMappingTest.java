package com.jurisfacil.organizations.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jurisfacil.organizations.controller.dto.response.OrganizationResponse;
import com.jurisfacil.organizations.mapper.OrganizationMapper;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;

class OrganizationResponseMappingTest {

    @Test
    void mapsOrganizationFieldsWithoutExposingEntity() {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-07T10:15:30Z");
        OrganizationEntity entity = OrganizationEntity.builder()
                .id(id)
                .name("Escritório Juris-Fácil")
                .cnpjCpf("12.345.678/0001-90")
                .contactEmail("contato@example.com")
                .phone("+55 85 99999-0000")
                .city("Fortaleza")
                .state("CE")
                .status(OrganizationStatus.ACTIVE)
                .createdAt(createdAt)
                .build();

        OrganizationResponse response = new OrganizationMapper().toResponse(entity);

        assertThat(response)
                .extracting(OrganizationResponse::id, OrganizationResponse::name,
                        OrganizationResponse::cnpjCpf, OrganizationResponse::contactEmail,
                        OrganizationResponse::phone, OrganizationResponse::city,
                        OrganizationResponse::state, OrganizationResponse::status,
                        OrganizationResponse::createdAt)
                .containsExactly(id, "Escritório Juris-Fácil", "12.345.678/0001-90",
                        "contato@example.com", "+55 85 99999-0000", "Fortaleza", "CE",
                        "ACTIVE", createdAt);
    }
}
