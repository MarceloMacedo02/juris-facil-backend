package com.jurisfacil.shared.exception;

public abstract class AbstractBusinessException extends RuntimeException implements BusinessException {

    private final String code;

    protected AbstractBusinessException(String code, String detail) {
        super(detail);
        this.code = code;
    }

    protected AbstractBusinessException(String code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }

    @Override
    public final String getCode() {
        return code;
    }
}
