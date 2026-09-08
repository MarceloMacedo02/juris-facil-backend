package com.jurisfacil.shared.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurisfacil.shared.exception.ErrorCode;
import com.jurisfacil.shared.exception.ProblemDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RETRY_AFTER = "Retry-After";
    private static final String PROBLEM_TYPE = "about:blank";
    private static final String DETAIL = "Rate limit exceeded.";

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || rateLimitConfig.getPaths().stream()
                        .noneMatch(path -> pathMatcher.match(path, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var probe = rateLimitConfig.bucketForIp(request.getRemoteAddr()).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
            response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getStatus());
            response.setHeader(RETRY_AFTER, Long.toString(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            ProblemDetails problemDetails = new ProblemDetails(
                    PROBLEM_TYPE,
                    ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                    ErrorCode.RATE_LIMIT_EXCEEDED.getStatus(),
                    ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                    DETAIL,
                    request.getRequestURI(),
                    List.of(),
                    OffsetDateTime.now());
            objectMapper.writeValue(response.getWriter(), problemDetails);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
