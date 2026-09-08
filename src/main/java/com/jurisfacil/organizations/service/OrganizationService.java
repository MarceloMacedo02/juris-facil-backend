package com.jurisfacil.organizations.service;

import java.util.UUID;

import com.jurisfacil.organizations.controller.dto.response.OrganizationResponse;

public interface OrganizationService {

    OrganizationResponse getWorkspaceOrganization(UUID organizationId);
}
