package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.controller.dto.request.UpdateEntitlementRequest;
import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminEntitlementServiceTest {

    @Mock
    private ModuleEntitlementRepository repository;
    @Mock
    private EntitlementService entitlementService;
    @Mock
    private AuditService auditService;

    private AdminEntitlementService service;

    @BeforeEach
    void setUp() {
        service = new AdminEntitlementService(repository, entitlementService, auditService);
    }

    @Test
    void updatesEntitlementEvictsCacheAndAuditsChange() {
        UUID organizationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ModuleEntitlementEntity entity = ModuleEntitlementEntity.builder()
                .organizationId(organizationId).moduleCode(ModuleCode.PROCESS.name()).enabled(false).build();
        when(repository.findByOrganizationIdAndModuleCode(organizationId, "PROCESS"))
                .thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var response = service.update(organizationId,
                new UpdateEntitlementRequest(ModuleCode.PROCESS, true), actorId);

        assertThat(response.enabled()).isTrue();
        verify(entitlementService).evict(organizationId);
        verify(auditService).record(any(AuditEvent.class));
    }
}
