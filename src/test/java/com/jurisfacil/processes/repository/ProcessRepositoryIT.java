package com.jurisfacil.processes.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import com.jurisfacil.organizations.model.enums.OrganizationStatus;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import com.jurisfacil.processes.model.enums.PartyRole;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.support.BaseIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProcessRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ProcessRepository processRepository;
    @Autowired
    private ProcessPartyRepository partyRepository;

    @Test
    void persistsTenantAwareProcessAndPartyAndQueriesByTenant() {
        OrganizationEntity organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Process Office")
                .contactEmail(UUID.randomUUID() + "@example.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        ProcessEntity process = processRepository.save(ProcessEntity.builder()
                .organizationId(organization.getId())
                .cnjNumber("00000000000000000000")
                .title("Tenant process")
                .court("TJCE")
                .courtUnit("Unit")
                .location("Fortaleza")
                .build());
        ProcessPartyEntity party = partyRepository.save(ProcessPartyEntity.builder()
                .organizationId(organization.getId())
                .processId(process.getId())
                .name("Client")
                .role(PartyRole.AUTOR)
                .client(true)
                .build());

        assertThat(process.getVersion()).isZero();
        assertThat(processRepository.existsByOrganizationIdAndCnjNumberAndStatusNot(
                organization.getId(), process.getCnjNumber(), ProcessStatus.CLOSED)).isTrue();
        assertThat(partyRepository.findByOrganizationIdAndProcessId(organization.getId(), process.getId()))
                .extracting(ProcessPartyEntity::getId).containsExactly(party.getId());
    }
}
