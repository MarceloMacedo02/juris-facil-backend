package com.jurisfacil.processes.model.entity;

import com.jurisfacil.processes.model.enums.MovementSource;
import com.jurisfacil.processes.model.enums.MovementType;
import com.jurisfacil.shared.tenant.TenantAware;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "process_movements")
public class ProcessMovementEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "movement_id")
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Column(name = "movement_date", nullable = false)
    private OffsetDateTime movementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementSource source;

    @Column(name = "created_by")
    private UUID createdBy;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (source == null) source = MovementSource.MANUAL;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
