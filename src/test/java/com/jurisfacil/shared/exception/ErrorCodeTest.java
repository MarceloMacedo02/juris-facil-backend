package com.jurisfacil.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void mapsEveryCanonicalCodeToItsContractStatus() {
        assertThat(ErrorCode.VALIDATION_FAILED.getStatus()).isEqualTo(400);
        assertThat(ErrorCode.INVALID_CREDENTIALS.getStatus()).isEqualTo(401);
        assertThat(ErrorCode.EXPIRED_TOKEN.getStatus()).isEqualTo(401);
        assertThat(ErrorCode.ACCESS_DENIED.getStatus()).isEqualTo(403);
        assertThat(ErrorCode.MODULE_DISABLED.getStatus()).isEqualTo(403);
        assertThat(ErrorCode.ACCOUNT_SUSPENDED.getStatus()).isEqualTo(403);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getStatus()).isEqualTo(404);
        assertThat(ErrorCode.DUPLICATE_PROCESS_IN_TENANT.getStatus()).isEqualTo(409);
        assertThat(ErrorCode.PLAN_LIMIT_EXCEEDED.getStatus()).isEqualTo(422);
        assertThat(ErrorCode.INTERNAL_ERROR.getStatus()).isEqualTo(500);
        assertThat(ErrorCode.UNEXPECTED_DATABASE_FAILURE.getStatus()).isEqualTo(500);
        assertThat(ErrorCode.OWNER_INVARIANT_VIOLATION.getStatus()).isEqualTo(400);
        assertThat(ErrorCode.EMAIL_ALREADY_IN_USE.getStatus()).isEqualTo(409);
        assertThat(ErrorCode.CNPJ_ALREADY_REGISTERED.getStatus()).isEqualTo(409);
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED.getStatus()).isEqualTo(429);
        assertThat(ErrorCode.RESET_TOKEN_INVALID.getStatus()).isEqualTo(400);
        assertThat(ErrorCode.INVITE_INVALID_OR_EXPIRED.getStatus()).isEqualTo(400);
        assertThat(ErrorCode.FORBIDDEN_ROLE.getStatus()).isEqualTo(403);
        assertThat(ErrorCode.TENANT_SUSPENDED.getStatus()).isEqualTo(403);
        assertThat(ErrorCode.OPTIMISTIC_LOCK.getStatus()).isEqualTo(409);
        assertThat(ErrorCode.IDEMPOTENCY_CONFLICT.getStatus()).isEqualTo(409);
    }

    @Test
    void resolvesUnknownCodeAsInternalError() {
        assertThat(ErrorCode.fromCode("UNKNOWN_CODE")).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}
