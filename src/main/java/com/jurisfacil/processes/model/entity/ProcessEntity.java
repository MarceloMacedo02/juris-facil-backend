package com.jurisfacil.processes.model.entity;

import com.jurisfacil.processes.model.enums.ProcessStatus;
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
import java.math.BigDecimal;
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
@Table(name = "processes")
public class ProcessEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "process_id")
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "cnj_number", length = 25)
    private String cnjNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String court;

    @Column(name = "court_unit", nullable = false, length = 100)
    private String courtUnit;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessStatus status;

    private String notes;

    @Column(name = "secret_justice", nullable = false)
    private boolean secretJustice;

    @Column(name = "claim_value", precision = 15, scale = 2)
    private BigDecimal claimValue;

    @Column(length = 50)
    private String area;

    @Column(name = "responsible_member_id")
    private UUID responsibleMemberId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (status == null) status = ProcessStatus.ACTIVE;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
