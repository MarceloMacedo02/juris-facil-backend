package com.jurisfacil.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_TYPE = "about:blank";
    private static final String GENERIC_INTERNAL_DETAIL = "An unexpected internal error occurred.";
    private static final String GENERIC_ACCESS_DETAIL = "Access is denied.";
    private static final String GENERIC_AUTHENTICATION_DETAIL = "Invalid credentials.";

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<InvalidParam> invalidParams = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toInvalidParam)
                .toList();
        return response(
                ErrorCode.VALIDATION_FAILED,
                "Validation failed.",
                request,
                invalidParams);
    }

    @ExceptionHandler(AbstractBusinessException.class)
    public ResponseEntity<ProblemDetails> handleBusiness(
            AbstractBusinessException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.fromCode(exception.getCode());
        String detail = errorCode == ErrorCode.INTERNAL_ERROR
                ? GENERIC_INTERNAL_DETAIL
                : exception.getMessage();
        return response(errorCode, detail, request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetails> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return response(ErrorCode.ACCESS_DENIED, GENERIC_ACCESS_DETAIL, request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetails> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {
        return response(ErrorCode.INVALID_CREDENTIALS, GENERIC_AUTHENTICATION_DETAIL, request, List.of());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetails> handleNotFound(
            NoHandlerFoundException exception,
            HttpServletRequest request) {
        return response(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found.", request, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<InvalidParam> invalidParams = exception.getConstraintViolations().stream()
                .map(violation -> new InvalidParam(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return response(
                ErrorCode.VALIDATION_FAILED,
                "Validation failed.",
                request,
                invalidParams);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        return response(ErrorCode.INTERNAL_ERROR, GENERIC_INTERNAL_DETAIL, request, List.of());
    }

    private InvalidParam toInvalidParam(FieldError fieldError) {
        return new InvalidParam(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetails> response(
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request,
            List<InvalidParam> invalidParams) {
        ProblemDetails problemDetails = new ProblemDetails(
                PROBLEM_TYPE,
                errorCode.name(),
                errorCode.getStatus(),
                errorCode.name(),
                addCorrelationId(detail, request),
                request.getRequestURI(),
                invalidParams,
                OffsetDateTime.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problemDetails, headers, HttpStatus.valueOf(errorCode.getStatus()));
    }

    private String addCorrelationId(String detail, HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (!environment.acceptsProfiles(Profiles.of("dev")) || correlationId == null || correlationId.isBlank()) {
            return detail;
        }
        return detail + " [correlation-id: " + correlationId + "]";
    }
}
