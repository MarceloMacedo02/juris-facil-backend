package com.jurisfacil.shared.exception;

import java.util.Arrays;

public enum ErrorCode {
    VALIDATION_FAILED(400),
    INVALID_CREDENTIALS(401),
    EXPIRED_TOKEN(401),
    ACCESS_DENIED(403),
    MODULE_DISABLED(403),
    ACCOUNT_SUSPENDED(403),
    RESOURCE_NOT_FOUND(404),
    DUPLICATE_PROCESS_IN_TENANT(409),
    PLAN_LIMIT_EXCEEDED(422),
    INTERNAL_ERROR(500),
    UNEXPECTED_DATABASE_FAILURE(500),
    OWNER_INVARIANT_VIOLATION(400),
    EMAIL_ALREADY_IN_USE(409),
    CNPJ_ALREADY_REGISTERED(409),
    RATE_LIMIT_EXCEEDED(429),
    RESET_TOKEN_INVALID(400),
    INVITE_INVALID_OR_EXPIRED(400),
    FORBIDDEN_ROLE(403),
    TENANT_SUSPENDED(403),
    OPTIMISTIC_LOCK(409),
    IDEMPOTENCY_CONFLICT(409);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static ErrorCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.name().equals(code))
                .findFirst()
                .orElse(INTERNAL_ERROR);
    }
}
