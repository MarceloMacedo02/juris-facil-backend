package com.jurisfacil.processes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.service.EntitlementService;
import com.jurisfacil.processes.controller.dto.request.CreateProcessRequest;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.enums.PartyRole;
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

@ExtendWith(MockitoExtension.class)
class ProcessCreateServiceTest {

    @Mock
    private EntitlementService entitlementService;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private ProcessRepository processRepository;
    @Mock
    private ProcessPartyRepository processPartyRepository;
    @Mock
    private AuditService auditService;

    @Test
    void createsProcessAndAuditsIt() {
        UUID organizationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        CreateProcessRequest request = request(List.of(new CreateProcessRequest.PartyInput(
                "Cliente", PartyRole.AUTOR, true)));
        when(processRepository.save(any(ProcessEntity.class))).thenAnswer(invocation -> {
            ProcessEntity process = invocation.getArgument(0);
            process.setId(processId);
            process.setCreatedAt(OffsetDateTime.now());
            return process;
        });

        var response = new ProcessCreateService(entitlementService, new ProcessMutationValidator(membershipRepository),
                processRepository, processPartyRepository, auditService)
                .create(organizationId, actorId, request);

        assertThat(response.id()).isEqualTo(processId);
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(processPartyRepository).saveAll(any());
        verify(auditService).record(any());
    }

    @Test
    void rejectsDuplicateCnjBeforePersisting() {
        UUID organizationId = UUID.randomUUID();
        when(processRepository.existsByOrganizationIdAndCnjNumberAndStatusNot(any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> new ProcessCreateService(entitlementService, new ProcessMutationValidator(membershipRepository),
                processRepository, processPartyRepository, auditService)
                .create(organizationId, UUID.randomUUID(), request(List.of(clientParty()))))
                .isInstanceOf(ProcessCreateService.DuplicateProcessException.class);
        verify(processRepository, never()).save(any());
    }

    @Test
    void rejectsMoreThanOneClientParty() {
        var parties = List.of(clientParty(), clientParty());

        assertThatThrownBy(() -> new ProcessCreateService(entitlementService, new ProcessMutationValidator(membershipRepository),
                processRepository, processPartyRepository, auditService)
                .create(UUID.randomUUID(), UUID.randomUUID(), request(parties)))
                .isInstanceOf(ProcessCreateService.MultipleClientPartiesException.class);
    }

    @Test
    void acceptsOnlyActiveLawyerAsResponsibleMember() {
        UUID memberId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CreateProcessRequest request = new CreateProcessRequest(null, "Título", "TJCE", "Vara", "Fortaleza",
                null, null, null, null, null, memberId, List.of(clientParty()));
        when(membershipRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ProcessCreateService(entitlementService, new ProcessMutationValidator(membershipRepository),
                processRepository, processPartyRepository, auditService)
                .create(organizationId, UUID.randomUUID(), request))
                .isInstanceOf(ProcessCreateService.InvalidResponsibleMemberException.class);
    }

    @Test
    void rejectsInvalidCnjWithValidationFailure() {
        CreateProcessRequest invalid = new CreateProcessRequest("invalid", "Título", "TJCE", "Vara",
                "Fortaleza", null, null, null, null, null, null, List.of(clientParty()));
        assertThatThrownBy(() -> new ProcessCreateService(entitlementService,
                new ProcessMutationValidator(membershipRepository), processRepository,
                processPartyRepository, auditService)
                .create(UUID.randomUUID(), UUID.randomUUID(), invalid))
                .isInstanceOf(ProcessCreateService.InvalidCnjException.class);
    }

    private CreateProcessRequest request(List<CreateProcessRequest.PartyInput> parties) {
        return new CreateProcessRequest("00000000000000000000", "Título", "TJCE", "Vara", "Fortaleza",
                null, null, null, null, null, null, parties);
    }

    private CreateProcessRequest.PartyInput clientParty() {
        return new CreateProcessRequest.PartyInput("Cliente", PartyRole.AUTOR, true);
    }
}
