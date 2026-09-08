package com.jurisfacil.processes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.service.EntitlementService;
import com.jurisfacil.processes.controller.dto.request.CreateProcessRequest;
import com.jurisfacil.processes.controller.dto.request.UpdateProcessRequest;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.enums.PartyRole;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProcessUpdateServiceTest {

    @Mock private EntitlementService entitlementService;
    @Mock private ProcessRepository processRepository;
    @Mock private ProcessPartyRepository processPartyRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuditService auditService;

    @Test
    void updatesMetadataAndReplacesParties() {
        UUID organizationId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        ProcessEntity process = process(organizationId, processId, 0);
        when(processRepository.findOne(any(Specification.class))).thenReturn(Optional.of(process));
        when(processRepository.saveAndFlush(process)).thenReturn(process);
        UpdateProcessRequest request = new UpdateProcessRequest(
                "00000000000000000000", "Novo título", "TJCE", "Vara 2", "Fortaleza",
                ProcessStatus.SUSPENDED, null, null, "notas", true, null,
                List.of(new CreateProcessRequest.PartyInput("Cliente", PartyRole.AUTOR, true)));

        ProcessResponseAssertions response = new ProcessResponseAssertions(new ProcessUpdateService(
                entitlementService, processRepository, processPartyRepository,
                new ProcessMutationValidator(membershipRepository), auditService)
                .update(organizationId, UUID.randomUUID(), processId, 0L, request));

        assertThat(response.status()).isEqualTo("SUSPENDED");
        verify(processPartyRepository).deleteByOrganizationIdAndProcessId(organizationId, processId);
        verify(processPartyRepository).saveAll(any());
    }

    @Test
    void rejectsStaleIfMatchVersion() {
        UUID organizationId = UUID.randomUUID();
        ProcessEntity process = process(organizationId, UUID.randomUUID(), 3);
        when(processRepository.findOne(any(Specification.class))).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> new ProcessUpdateService(entitlementService, processRepository,
                processPartyRepository, new ProcessMutationValidator(membershipRepository), auditService)
                .update(organizationId, UUID.randomUUID(), process.getId(), 2L,
                        new UpdateProcessRequest(null, "Título", "TJCE", "Vara", "Fortaleza", null,
                                null, null, null, null, null, null)))
                .isInstanceOf(ProcessUpdateService.OptimisticLockBusinessException.class);
    }

    private ProcessEntity process(UUID organizationId, UUID id, long version) {
        return ProcessEntity.builder().id(id).organizationId(organizationId).title("Título")
                .court("TJCE").courtUnit("Vara").location("Fortaleza").status(ProcessStatus.ACTIVE)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).version(version).build();
    }

    private record ProcessResponseAssertions(String id, String cnjNumber, String title, String status,
            OffsetDateTime createdAt) {
        ProcessResponseAssertions(com.jurisfacil.processes.controller.dto.response.ProcessResponse response) {
            this(response.id() == null ? null : response.id().toString(), response.cnjNumber(), response.title(),
                    response.status(), response.createdAt());
        }
    }
}
