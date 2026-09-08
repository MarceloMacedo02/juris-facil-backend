package com.jurisfacil.processes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import com.jurisfacil.processes.model.enums.PartyRole;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.tenant.TenantContext;
import com.jurisfacil.shared.tenant.TenantContextHolder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProcessDetailServiceTest {

    @Mock
    private ProcessRepository processRepository;
    @Mock
    private ProcessPartyRepository processPartyRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void returnsCompleteTenantScopedDetail() {
        UUID organizationId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(organizationId, "LAWYER", List.of("PROCESS")));
        ProcessEntity process = ProcessEntity.builder()
                .id(processId).organizationId(organizationId).cnjNumber("00000000000000000000")
                .title("Ação").court("TJCE").courtUnit("Vara").location("Fortaleza")
                .status(ProcessStatus.ACTIVE).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .version(0).build();
        ProcessPartyEntity party = ProcessPartyEntity.builder()
                .id(UUID.randomUUID()).organizationId(organizationId).processId(processId)
                .name("Cliente").role(PartyRole.AUTOR).client(true).createdAt(OffsetDateTime.now()).build();
        when(processRepository.findOne(any(Specification.class))).thenReturn(Optional.of(process));
        when(processPartyRepository.findByOrganizationIdAndProcessIdOrderByCreatedAtAsc(organizationId, processId))
                .thenReturn(List.of(party));

        var response = new ProcessDetailService(processRepository, processPartyRepository,
                membershipRepository, userRepository).get(processId);

        assertThat(response.id()).isEqualTo(processId);
        assertThat(response.parties()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("Cliente");
            assertThat(item.isClient()).isTrue();
        });
        assertThat(response.lastMovement()).isNull();
    }

    @Test
    void hidesCrossTenantProcessAsNotFound() {
        TenantContextHolder.set(new TenantContext(UUID.randomUUID(), "LAWYER", List.of("PROCESS")));
        when(processRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ProcessDetailService(processRepository, processPartyRepository,
                membershipRepository, userRepository).get(UUID.randomUUID()))
                .isInstanceOf(ProcessDetailService.ProcessNotFoundException.class);
    }
}
