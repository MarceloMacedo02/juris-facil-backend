package com.jurisfacil.organizations.service;

import com.jurisfacil.organizations.controller.dto.request.CreateOrganizationRequest;
import com.jurisfacil.organizations.controller.dto.response.CreateOrganizationResponse;

public interface AdminOrganizationService {

    CreateOrganizationResponse createOrganization(CreateOrganizationRequest request);
}
