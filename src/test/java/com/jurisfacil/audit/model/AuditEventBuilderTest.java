package com.jurisfacil.audit.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventBuilderTest {

    @Test
    void buildsFluentAuditEvent() {
        UUID actorId = UUID.randomUUID();
        AuditEvent event = AuditEvent.builder()
                .action(AuditAction.LOGIN_SUCCESS)
                .actorId(actorId)
                .resourceType("AUTH")
                .payload(Map.of("method", "password"))
                .build();

        assertThat(event.action()).isEqualTo(AuditAction.LOGIN_SUCCESS);
        assertThat(event.actorId()).isEqualTo(actorId);
        assertThat(event.payload()).containsEntry("method", "password");
    }

    @Test
    void requiresActionAndResourceType() {
        assertThatThrownBy(() -> AuditEvent.builder().resourceType("AUTH").build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEvent.builder().action(AuditAction.LOGIN_SUCCESS).build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
