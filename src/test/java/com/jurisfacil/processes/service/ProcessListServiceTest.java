package com.jurisfacil.processes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.tenant.TenantContext;
import com.jurisfacil.shared.tenant.TenantContextHolder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProcessListServiceTest {

    @Mock
    private ProcessRepository processRepository;

    @Mock
    private ProcessPartyRepository processPartyRepository;

    private final UUID organizationId = UUID.randomUUID();

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void mapsPageAndClientNameWithTheAllowedDefaultSort() {
        TenantContextHolder.set(new TenantContext(organizationId, "OWNER", List.of("PROCESS")));
        UUID processId = UUID.randomUUID();
        ProcessEntity process = ProcessEntity.builder()
                .id(processId)
                .organizationId(organizationId)
                .cnjNumber("00000000000000000000")
                .title("Ação de cobrança")
                .court("TJCE")
                .courtUnit("1ª Vara")
                .location("Fortaleza")
                .status(ProcessStatus.ACTIVE)
                .updatedAt(OffsetDateTime.now())
                .build();
        when(processRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(process)));
        when(processPartyRepository.findClientNamesByProcessIds(anyList()))
                .thenReturn(List.of(new ProcessListService.ClientNameProjection() {
                    @Override
                    public UUID processId() {
                        return processId;
                    }

                    @Override
                    public String name() {
                        return "Cliente Teste";
                    }
                }));

        var response = new ProcessListService(processRepository, processPartyRepository)
                .list("cobrança", ProcessStatus.ACTIVE, 0, 25);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(processId);
            assertThat(item.clientName()).isEqualTo("Cliente Teste");
            assertThat(item.status()).isEqualTo("ACTIVE");
        });
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
        verify(processRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void rejectsPageSizesOutsideTheContract() {
        var service = new ProcessListService(processRepository, processPartyRepository);

        assertThatThrownBy(() -> service.list(null, null, 0, 20))
                .isInstanceOf(ProcessListService.InvalidPageSizeException.class);
    }
}
