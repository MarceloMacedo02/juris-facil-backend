package com.jurisfacil.audit.model;

import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        AuditAction action,
        UUID actorId,
        UUID organizationId,
        String resourceType,
        String resourceId,
        Map<String, Object> payload) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AuditAction action;
        private UUID actorId;
        private UUID organizationId;
        private String resourceType;
        private String resourceId;
        private Map<String, Object> payload;

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder actorId(UUID actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public AuditEvent build() {
            if (action == null) {
                throw new IllegalArgumentException("action is required");
            }
            if (resourceType == null || resourceType.isBlank()) {
                throw new IllegalArgumentException("resourceType is required");
            }
            return new AuditEvent(action, actorId, organizationId, resourceType, resourceId, payload);
        }
    }
}
