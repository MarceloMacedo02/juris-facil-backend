package com.jurisfacil.organizations.service;

import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.controller.dto.request.UpdateEntitlementRequest;
import com.jurisfacil.organizations.controller.dto.response.EntitlementResponse;
import com.jurisfacil.organizations.model.entity.ModuleEntitlementEntity;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.repository.ModuleEntitlementRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminEntitlementService {

    private final ModuleEntitlementRepository entitlementRepository;
    private final EntitlementService entitlementService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EntitlementResponse> list(UUID organizationId) {
        return entitlementRepository.findByOrganizationId(organizationId).stream()
                .map(item -> new EntitlementResponse(ModuleCode.valueOf(item.getModuleCode()), item.isEnabled()))
                .toList();
    }

    public EntitlementResponse update(UUID organizationId, UpdateEntitlementRequest request, UUID actorId) {
        ModuleEntitlementEntity entitlement = entitlementRepository
                .findByOrganizationIdAndModuleCode(organizationId, request.moduleCode().name())
                .orElseGet(() -> ModuleEntitlementEntity.builder()
                        .organizationId(organizationId).moduleCode(request.moduleCode().name()).build());
        entitlement.setEnabled(request.enabled());
        entitlement = entitlementRepository.save(entitlement);
        entitlementService.evict(organizationId);
        auditService.record(AuditEvent.builder().action(AuditAction.ENTITLEMENT_CHANGED).actorId(actorId)
                .organizationId(organizationId).resourceType("ENTITLEMENT")
                .resourceId(request.moduleCode().name())
                .payload(java.util.Map.of("moduleCode", request.moduleCode().name(), "enabled", request.enabled()))
                .build());
        return new EntitlementResponse(request.moduleCode(), entitlement.isEnabled());
    }
}
