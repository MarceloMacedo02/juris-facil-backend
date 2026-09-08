package com.jurisfacil.organizations.repository;

import com.jurisfacil.organizations.model.entity.OrganizationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID>,
        JpaSpecificationExecutor<OrganizationEntity> {

    boolean existsByCnpjCpf(String cnpjCpf);
}
