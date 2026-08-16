package com.charitymanagement.api.charitymanagementback.distribution.entity;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.entity.BaseEntity;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "distribution_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_distribution_reference", columnNames = "request_reference"),
        indexes = {
                @Index(name = "idx_distribution_status", columnList = "status"),
                @Index(name = "idx_distribution_request_date", columnList = "request_date"),
                @Index(name = "idx_distribution_beneficiary", columnList = "beneficiary_id"),
                @Index(name = "idx_distribution_priority", columnList = "priority")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_reference", nullable = false, length = 32)
    private String requestReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_distribution_beneficiary"))
    private Beneficiary beneficiary;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private PriorityLevel priority = PriorityLevel.MEDIUM;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DistributionStatus status = DistributionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", foreignKey = @ForeignKey(name = "fk_distribution_requested_by"))
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", foreignKey = @ForeignKey(name = "fk_distribution_approved_by"))
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by", foreignKey = @ForeignKey(name = "fk_distribution_rejected_by"))
    private User rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by", foreignKey = @ForeignKey(name = "fk_distribution_cancelled_by"))
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by", foreignKey = @ForeignKey(name = "fk_distribution_completed_by"))
    private User completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Read/aggregate view of the lines. Lines are created and removed through
     * {@code DistributionItemRepository} rather than by mutating this collection, so no
     * orphan removal is configured here.
     */
    @OneToMany(mappedBy = "distributionRequest", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<DistributionItem> items = new ArrayList<>();

    /** Guards against two concurrent transitions of the same request. */
    @Version
    @Column(name = "version")
    private Long version;

    public boolean isEditable() {
        return status == DistributionStatus.PENDING;
    }
}
