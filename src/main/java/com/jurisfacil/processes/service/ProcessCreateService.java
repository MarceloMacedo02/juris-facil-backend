package com.jurisfacil.processes.service;

import com.jurisfacil.audit.model.AuditAction;
import com.jurisfacil.audit.model.AuditEvent;
import com.jurisfacil.audit.service.AuditService;
import com.jurisfacil.organizations.model.enums.MembershipRole;
import com.jurisfacil.organizations.model.enums.MembershipStatus;
import com.jurisfacil.organizations.repository.MembershipRepository;
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

    private static final String CNJ_DIGITS_REGEX = "\\d{20}";

    private final EntitlementService entitlementService;
    private final MembershipRepository membershipRepository;
    private final ProcessRepository processRepository;
    private final ProcessPartyRepository processPartyRepository;
    private final AuditService auditService;

    @Transactional
    public ProcessResponse create(UUID organizationId, UUID actorId, CreateProcessRequest request) {
        entitlementService.assertEnabled(organizationId, ModuleCode.PROCESS);
        String cnjNumber = normalizeCnj(request.cnjNumber());
        if (cnjNumber != null && processRepository.existsByOrganizationIdAndCnjNumberAndStatusNot(
                organizationId, cnjNumber, ProcessStatus.CLOSED)) {
            throw new DuplicateProcessException();
        }
        if (request.parties().stream().filter(CreateProcessRequest.PartyInput::isClient).count() > 1) {
            throw new MultipleClientPartiesException();
        }
        if (request.responsibleMemberId() != null && !membershipRepository
                .findById(request.responsibleMemberId())
                .filter(member -> organizationId.equals(member.getOrganizationId()))
                .filter(member -> member.getRole() == MembershipRole.LAWYER)
                .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
                .isPresent()) {
            throw new InvalidResponsibleMemberException();
        }

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

    private String normalizeCnj(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.matches("\\d{20}|\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}")) {
            throw new InvalidCnjException();
        }
        String digits = value.replaceAll("\\D", "");
        if (!digits.matches(CNJ_DIGITS_REGEX)) {
            throw new InvalidCnjException();
        }
        return digits;
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
            super(ErrorCode.VALIDATION_ERROR.name(), "CNJ must contain 20 digits.");
        }
    }
}
