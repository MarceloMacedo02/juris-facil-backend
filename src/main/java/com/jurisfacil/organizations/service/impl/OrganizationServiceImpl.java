package com.jurisfacil.organizations.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jurisfacil.organizations.controller.dto.response.OrganizationResponse;
import com.jurisfacil.organizations.mapper.OrganizationMapper;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.service.OrganizationService;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    @Override
    public OrganizationResponse getWorkspaceOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(organizationMapper::toResponse)
                .orElseThrow(OrganizationNotFoundException::new);
    }

    private static final class OrganizationNotFoundException extends AbstractBusinessException {

        private OrganizationNotFoundException() {
            super(ErrorCode.RESOURCE_NOT_FOUND.name(), "Organization not found.");
        }
    }
}
