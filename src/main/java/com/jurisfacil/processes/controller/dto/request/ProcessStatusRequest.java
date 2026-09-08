package com.jurisfacil.processes.controller.dto.request;

import com.jurisfacil.processes.model.enums.ProcessStatus;
import jakarta.validation.constraints.NotNull;

public record ProcessStatusRequest(@NotNull ProcessStatus status) {
}
