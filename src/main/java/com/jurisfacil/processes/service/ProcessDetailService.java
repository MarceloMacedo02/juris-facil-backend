package com.jurisfacil.processes.service;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.processes.controller.dto.response.ProcessDetailResponse;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;
import com.jurisfacil.shared.tenant.TenantAwareSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessDetailService {

    private final ProcessRepository processRepository;
    private final ProcessPartyRepository processPartyRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProcessDetailResponse get(UUID processId) {
        ProcessEntity process = processRepository.findOne(
                        TenantAwareSpecification.<ProcessEntity>byCurrentTenant()
                                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), processId)))
                .orElseThrow(ProcessNotFoundException::new);
        var parties = processPartyRepository.findByOrganizationIdAndProcessIdOrderByCreatedAtAsc(
                process.getOrganizationId(), process.getId());
        ProcessDetailResponse.ResponsibleMember responsibleMember = responsibleMember(process);
        return ProcessDetailResponse.from(process, responsibleMember, parties);
    }

    private ProcessDetailResponse.ResponsibleMember responsibleMember(ProcessEntity process) {
        if (process.getResponsibleMemberId() == null) {
            return null;
        }
        MembershipEntity membership = membershipRepository.findById(process.getResponsibleMemberId()).orElse(null);
        if (membership == null) {
            return null;
        }
        String name = userRepository.findById(membership.getUserId()).map(UserEntity::getName).orElse(null);
        return new ProcessDetailResponse.ResponsibleMember(membership.getId(), name);
    }

    public static final class ProcessNotFoundException extends AbstractBusinessException {
        public ProcessNotFoundException() {
            super(ErrorCode.RESOURCE_NOT_FOUND.name(), "Process not found.");
        }
    }
}
