package com.jurisfacil.processes.service;

import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.model.enums.ModuleCode;
import com.jurisfacil.organizations.service.EntitlementService;
import com.jurisfacil.processes.controller.dto.request.CreateProcessRequest;
import com.jurisfacil.processes.controller.dto.response.ProcessResponse;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessCreateService {

    private final EntitlementService entitlementService;
    private final ProcessMutationValidator mutationValidator;
    private final ProcessRepository processRepository;
    private final ProcessPartyRepository processPartyRepository;
    private final AuditService auditService;

    @Transactional
    public ProcessResponse create(UUID organizationId, UUID actorId, CreateProcessRequest request) {
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        String cnjNumber = mutationValidator.normalizeCnj(request.cnjNumber());
        if (cnjNumber != null && processRepository.existsByOrganizationIdAndCnjNumberAndStatusNot(
                organizationId, cnjNumber, ProcessStatus.CLOSED)) {
            throw new DuplicateProcessException();
        }
        mutationValidator.validateParties(request.parties());
        mutationValidator.validateResponsible(organizationId, request.responsibleMemberId());

        ProcessEntity process = processRepository.save(ProcessEntity.builder()
                .organizationId(organizationId)
                .cnjNumber(cnjNumber)
                .title(request.title().trim())
                .court(request.court().trim())
                .courtUnit(request.courtUnit().trim())
                .location(request.location().trim())
                .status(request.status() == null ? ProcessStatus.ACTIVE : request.status())
                .area(request.area())
                .claimValue(request.claimValue())
                .notes(request.notes())
                .secretJustice(Boolean.TRUE.equals(request.secretJustice()))
                .responsibleMemberId(request.responsibleMemberId())
                .build());

        List<ProcessPartyEntity> parties = request.parties().stream()
                .map(party -> ProcessPartyEntity.builder()
                        .organizationId(organizationId)
                        .processId(process.getId())
                        .name(party.name().trim())
                        .role(party.role())
                        .client(party.isClient())
                        .build())
                .toList();
        processPartyRepository.saveAll(parties);
        auditService.record(AuditEvent.builder()
                .action(AuditAction.PROCESS_CREATED)
                .actorId(actorId)
                .organizationId(organizationId)
                .resourceType("PROCESS")
                .resourceId(process.getId().toString())
                .payload(Map.of("cnjNumber", cnjNumber == null ? "" : cnjNumber))
                .build());
        return ProcessResponse.from(process);
    }


    public static final class DuplicateProcessException extends AbstractBusinessException {
        public DuplicateProcessException() {
            super(ErrorCode.DUPLICATE_PROCESS_IN_TENANT.name(), "CNJ already exists in this organization.");
        }
    }

    public static final class MultipleClientPartiesException extends AbstractBusinessException {
        public MultipleClientPartiesException() {
            super(ErrorCode.VALIDATION_ERROR.name(), "Only one client party is allowed.");
        }
    }

    public static final class InvalidResponsibleMemberException extends AbstractBusinessException {
        public InvalidResponsibleMemberException() {
            super(ErrorCode.VALIDATION_ERROR.name(), "Responsible member must be an active lawyer in the organization.");
        }
    }

    public static final class InvalidCnjException extends AbstractBusinessException {
        public InvalidCnjException() {
            super(ErrorCode.VALIDATION_FAILED.name(), "CNJ must contain 20 digits.");
        }
    }
}
