package com.jurisfacil.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.model.AuditEventEntity;
import com.jurisfacil.audit.repository.AuditEventRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    private AuditService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AuditService(repository, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void recordsEventWithoutRequestContext() {
        service.record(AuditEvent.builder()
                .action(AuditAction.LOGIN_SUCCESS)
                .resourceType("AUTH")
                .payload(Map.of("source", "test"))
                .build());

        verify(repository).save(any(AuditEventEntity.class));
    }

    @Test
    void absorbsRepositoryFailure() {
        doThrow(new RuntimeException("database unavailable")).when(repository).save(any(AuditEventEntity.class));

        service.record(AuditEvent.builder()
                .action(AuditAction.LOGIN_FAILED)
                .resourceType("AUTH")
                .build());

        verify(repository).save(any(AuditEventEntity.class));
    }

    @Test
    void replacesOversizedPayloadWithValidTruncationMarker() {
        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        service.record(AuditEvent.builder()
                .action(AuditAction.PROCESS_UPDATED)
                .resourceType("PROCESS")
                .payload(Map.of("details", "x".repeat(5000)))
                .build());

        verify(repository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPayload())
                .isEqualTo("{\"truncated\":true}");
    }
}
