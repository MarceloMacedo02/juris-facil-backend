package com.jurisfacil.processes.service;

import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.service.EntitlementService;
import com.jurisfacil.processes.controller.dto.request.CreateProcessRequest;
import com.jurisfacil.processes.controller.dto.request.ProcessStatusRequest;
import com.jurisfacil.processes.controller.dto.request.UpdateProcessRequest;
import com.jurisfacil.processes.controller.dto.response.ProcessResponse;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;
import com.jurisfacil.shared.tenant.TenantAwareSpecification;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessUpdateService {

    private final EntitlementService entitlementService;
    private final ProcessRepository processRepository;
    private final ProcessPartyRepository processPartyRepository;
    private final ProcessMutationValidator mutationValidator;
    private final AuditService auditService;

    @Transactional
    public ProcessResponse update(UUID organizationId, UUID actorId, UUID processId,
            Long expectedVersion, UpdateProcessRequest request) {
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        ProcessEntity process = find(organizationId, processId);
        assertVersion(process, expectedVersion);
        String cnjNumber = mutationValidator.normalizeCnj(request.cnjNumber());
        if (cnjNumber != null && !cnjNumber.equals(process.getCnjNumber())
                && processRepository.existsByOrganizationIdAndCnjNumberAndStatusNot(
                        organizationId, cnjNumber, ProcessStatus.CLOSED)) {
            throw new ProcessCreateService.DuplicateProcessException();
        }
        mutationValidator.validateResponsible(organizationId, request.responsibleMemberId());
        if (request.parties() != null) {
            mutationValidator.validateParties(request.parties());
        }
        process.setCnjNumber(cnjNumber);
        process.setTitle(request.title().trim());
        process.setCourt(request.court().trim());
        process.setCourtUnit(request.courtUnit().trim());
        process.setLocation(request.location().trim());
        process.setStatus(request.status() == null ? process.getStatus() : request.status());
        process.setArea(request.area());
        process.setClaimValue(request.claimValue());
        process.setNotes(request.notes());
        process.setSecretJustice(Boolean.TRUE.equals(request.secretJustice()));
        process.setResponsibleMemberId(request.responsibleMemberId());
        if (request.parties() != null) {
            replaceParties(organizationId, processId, request.parties());
        }
        try {
            ProcessEntity saved = processRepository.saveAndFlush(process);
            auditService.record(AuditEvent.builder().action(AuditAction.PROCESS_UPDATED)
                    .actorId(actorId).organizationId(organizationId).resourceType("PROCESS")
                    .resourceId(processId.toString()).payload(Map.of("version", saved.getVersion())).build());
            return ProcessResponse.from(saved);
        } catch (OptimisticLockException exception) {
            throw new OptimisticLockBusinessException();
        }
    }

    @Transactional
    public ProcessResponse changeStatus(UUID organizationId, UUID actorId, UUID processId,
            ProcessStatusRequest request) {
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        ProcessEntity process = find(organizationId, processId);
        ProcessStatus previous = process.getStatus();
        process.setStatus(request.status());
        ProcessEntity saved = processRepository.saveAndFlush(process);
        AuditAction action = switch (request.status()) {
            case SUSPENDED -> AuditAction.PROCESS_SUSPENDED;
            case CLOSED -> AuditAction.PROCESS_CLOSED;
            case ACTIVE -> AuditAction.PROCESS_REACTIVATED;
        };
        auditService.record(AuditEvent.builder().action(action).actorId(actorId)
                .organizationId(organizationId).resourceType("PROCESS").resourceId(processId.toString())
                .payload(Map.of("previousStatus", previous.name(), "status", saved.getStatus().name())).build());
        return ProcessResponse.from(saved);
    }

    @Transactional
    public ProcessResponse archive(UUID organizationId, UUID actorId, UUID processId) {
        return changeStatus(organizationId, actorId, processId,
                new ProcessStatusRequest(ProcessStatus.CLOSED));
    }

    public Long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(ifMatch.replace("W/", "").replace("\"", "").trim());
        } catch (NumberFormatException exception) {
            throw new InvalidIfMatchException();
        }
    }

    private ProcessEntity find(UUID organizationId, UUID processId) {
        return processRepository.findOne(TenantAwareSpecification.<ProcessEntity>byCurrentTenant()
                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), processId)))
                .orElseThrow(ProcessDetailService.ProcessNotFoundException::new);
    }

    private void assertVersion(ProcessEntity process, Long expectedVersion) {
        if (expectedVersion != null && expectedVersion.longValue() != process.getVersion()) {
            throw new OptimisticLockBusinessException();
        }
    }

    private void replaceParties(UUID organizationId, UUID processId, List<CreateProcessRequest.PartyInput> parties) {
        processPartyRepository.deleteByOrganizationIdAndProcessId(organizationId, processId);
        processPartyRepository.saveAll(parties.stream().map(party -> ProcessPartyEntity.builder()
                .organizationId(organizationId).processId(processId).name(party.name().trim())
                .role(party.role()).client(party.isClient()).build()).toList());
    }

    public static final class OptimisticLockBusinessException extends AbstractBusinessException {
        public OptimisticLockBusinessException() {
            super(ErrorCode.OPTIMISTIC_LOCK.name(), "Process was changed by another request.");
        }
    }

    public static final class InvalidIfMatchException extends AbstractBusinessException {
        public InvalidIfMatchException() {
            super(ErrorCode.VALIDATION_ERROR.name(), "If-Match must contain a process version.");
        }
    }
}
