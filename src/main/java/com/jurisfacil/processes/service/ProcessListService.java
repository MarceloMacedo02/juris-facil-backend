package com.jurisfacil.processes.service;

import com.jurisfacil.processes.controller.dto.response.PageResponse;
import com.jurisfacil.processes.controller.dto.response.ProcessListItem;
import com.jurisfacil.processes.model.entity.ProcessEntity;
import com.jurisfacil.processes.model.entity.ProcessPartyEntity;
import com.jurisfacil.processes.model.enums.ProcessStatus;
import com.jurisfacil.processes.repository.ProcessPartyRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;
import com.jurisfacil.shared.tenant.TenantAwareSpecification;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessListService {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50);

    private final ProcessRepository processRepository;
    private final ProcessPartyRepository processPartyRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProcessListItem> list(String query, ProcessStatus status, int page, int size) {
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new InvalidPageSizeException(size);
        }
        if (page < 0) {
            throw new InvalidPageException(page);
        }

        Page<ProcessEntity> processes = processRepository.findAll(
                specification(query, status),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<UUID> processIds = processes.getContent().stream().map(ProcessEntity::getId).toList();
        Map<UUID, String> clientNames = processIds.isEmpty()
                ? Map.of()
                : processPartyRepository.findClientNamesByProcessIds(processIds).stream()
                        .collect(Collectors.toMap(ClientNameProjection::processId, ClientNameProjection::name));

        List<ProcessListItem> items = processes.getContent().stream()
                .map(process -> new ProcessListItem(
                        process.getId(), process.getCnjNumber(), process.getTitle(), process.getCourt(),
                        process.getCourtUnit(), process.getStatus().name(), clientNames.get(process.getId()),
                        null, process.getUpdatedAt()))
                .toList();
        return new PageResponse<>(items, processes.getNumber(), processes.getSize(),
                processes.getTotalElements(), processes.getTotalPages());
    }

    private Specification<ProcessEntity> specification(String query, ProcessStatus status) {
        Specification<ProcessEntity> specification = TenantAwareSpecification.byCurrentTenant();
        if (status != null) {
            specification = specification.and((root, ignored, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (query == null || query.isBlank()) {
            return specification;
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return specification.and((root, criteriaQuery, criteriaBuilder) -> {
            Predicate title = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
            Predicate cnjNumber = criteriaBuilder.like(criteriaBuilder.lower(root.get("cnjNumber")), pattern);
            Predicate courtUnit = criteriaBuilder.like(criteriaBuilder.lower(root.get("courtUnit")), pattern);
            var partySubquery = criteriaQuery.subquery(UUID.class);
            var party = partySubquery.from(ProcessPartyEntity.class);
            partySubquery.select(party.get("processId"));
            partySubquery.where(
                    criteriaBuilder.equal(party.get("processId"), root.get("id")),
                    criteriaBuilder.like(criteriaBuilder.lower(party.get("name")), pattern));
            return criteriaBuilder.or(title, cnjNumber, courtUnit, criteriaBuilder.exists(partySubquery));
        });
    }

    public interface ClientNameProjection {
        UUID processId();

        String name();
    }

    public static final class InvalidPageSizeException extends AbstractBusinessException {
        public InvalidPageSizeException(int size) {
            super(ErrorCode.VALIDATION_ERROR.name(), "Page size must be one of 10, 25 or 50.");
        }
    }

    public static final class InvalidPageException extends AbstractBusinessException {
        public InvalidPageException(int page) {
            super(ErrorCode.VALIDATION_ERROR.name(), "Page must be zero or greater.");
        }
    }
}
