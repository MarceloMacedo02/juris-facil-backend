package com.jurisfacil.processes.controller.dto.request;

import com.jurisfacil.processes.model.enums.PartyRole;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProcessRequest(
        String cnjNumber,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50) String court,
        @NotBlank @Size(max = 100) String courtUnit,
        @NotBlank @Size(max = 255) String location,
        ProcessStatus status,
        @Size(max = 50) String area,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal claimValue,
        String notes,
        Boolean secretJustice,
        UUID responsibleMemberId,
        @NotEmpty @Valid List<PartyInput> parties) {

    public record PartyInput(
            @NotBlank @Size(max = 255) String name,
            @NotNull PartyRole role,
            boolean isClient) {
    }
}
