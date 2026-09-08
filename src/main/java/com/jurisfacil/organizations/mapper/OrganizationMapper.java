package com.jurisfacil.organizations.mapper;

import org.springframework.stereotype.Component;

import com.jurisfacil.organizations.controller.dto.response.OrganizationResponse;
import com.jurisfacil.organizations.model.entity.OrganizationEntity;

@Component
public class OrganizationMapper {

    public OrganizationResponse toResponse(OrganizationEntity organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCnpjCpf(),
                organization.getContactEmail(),
                organization.getPhone(),
                organization.getCity(),
                organization.getState(),
                organization.getStatus().name(),
                organization.getCreatedAt());
    }
}
