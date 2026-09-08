package com.jurisfacil.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.model.AuditEventEntity;
import com.jurisfacil.audit.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
public class AuditService {

    private static final int MAX_PAYLOAD_BYTES = 4096;
    private static final String TRUNCATED_PAYLOAD = "{\"truncated\":true}";

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(AuditEvent event) {
        try {
            String payload = serializePayload(event.payload());
            RequestContext requestContext = requestContext();
            repository.save(new AuditEventEntity(event, payload, requestContext.ipAddress(), requestContext.userAgent()));
        } catch (RuntimeException exception) {
            log.error("Unable to persist audit event action={} resourceType={}",
                    event == null ? null : event.action(),
                    event == null ? null : event.resourceType(), exception);
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            String serialized = objectMapper.writeValueAsString(payload);
            return serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= MAX_PAYLOAD_BYTES
                    ? serialized : TRUNCATED_PAYLOAD;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize audit payload", exception);
        }
    }

    private RequestContext requestContext() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return RequestContext.EMPTY;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        return new RequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    private record RequestContext(String ipAddress, String userAgent) {
        private static final RequestContext EMPTY = new RequestContext(null, null);
    }
}
