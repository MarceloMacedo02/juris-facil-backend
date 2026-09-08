package com.jurisfacil.processes.service;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.organizations.model.entity.MembershipEntity;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.processes.controller.dto.response.MovementResponse;
import com.jurisfacil.processes.controller.dto.response.PageResponse;
import com.jurisfacil.processes.mapper.MovementMapper;
import com.jurisfacil.processes.model.entity.ProcessMovementEntity;
import com.jurisfacil.processes.repository.MovementRepository;
import com.jurisfacil.processes.repository.ProcessRepository;
import com.jurisfacil.shared.tenant.TenantAwareSpecification;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovementListService {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final MovementRepository movementRepository;
    private final ProcessRepository processRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final MovementMapper movementMapper;

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> list(UUID organizationId, UUID processId, int page, int size) {
        validatePage(page, size);
        processRepository.findOne(processSpecification(processId, organizationId))
                .orElseThrow(ProcessDetailService.ProcessNotFoundException::new);

        Page<ProcessMovementEntity> movements = movementRepository
                .findByOrganizationIdAndProcessIdOrderByMovementDateDesc(
                        organizationId, processId, PageRequest.of(page, size));
        Map<UUID, String> authorNames = resolveAuthorNames(organizationId, movements);
        var items = movements.getContent().stream()
                .map(movement -> movementMapper.toResponse(movement,
                        movement.getCreatedBy() == null ? null : authorNames.get(movement.getCreatedBy())))
                .toList();
        return new PageResponse<>(items, movements.getNumber(), movements.getSize(),
                movements.getTotalElements(), movements.getTotalPages());
    }

    private Specification<com.jurisfacil.processes.model.entity.ProcessEntity> processSpecification(
            UUID processId, UUID organizationId) {
        return TenantAwareSpecification.<com.jurisfacil.processes.model.entity.ProcessEntity>byTenant(organizationId)
                .and((root, ignored, criteriaBuilder) -> criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("id"), processId),
                        criteriaBuilder.equal(root.get("organizationId"), organizationId)));
    }

    private Map<UUID, String> resolveAuthorNames(UUID organizationId, Page<ProcessMovementEntity> movements) {
        var membershipIds = movements.getContent().stream()
                .map(ProcessMovementEntity::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (membershipIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, MembershipEntity> memberships = membershipRepository
                .findByOrganizationIdAndIdIn(organizationId, membershipIds).stream()
                .collect(Collectors.toMap(MembershipEntity::getId, Function.identity()));
        Map<UUID, UserEntity> users = userRepository.findAllById(memberships.values().stream()
                        .map(MembershipEntity::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return memberships.values().stream()
                .filter(membership -> users.containsKey(membership.getUserId()))
                .collect(Collectors.toMap(MembershipEntity::getId,
                        membership -> users.get(membership.getUserId()).getName()));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new ProcessListService.InvalidPageException(page);
        }
        if (size != 10 && size != 25 && size != DEFAULT_PAGE_SIZE) {
            throw new ProcessListService.InvalidPageSizeException(size);
        }
    }
}
