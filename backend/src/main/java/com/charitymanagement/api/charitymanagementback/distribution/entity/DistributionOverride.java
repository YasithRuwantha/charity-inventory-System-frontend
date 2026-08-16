package com.charitymanagement.api.charitymanagementback.distribution.entity;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Permanent record of an ADMIN consciously distributing an item the beneficiary already received
 * inside the duplicate window. Kept as its own table so the justification survives independently
 * of the audit log.
 */
@Entity
@Table(name = "distribution_overrides", indexes = {
        @Index(name = "idx_override_request", columnList = "distribution_request_id"),
        @Index(name = "idx_override_beneficiary", columnList = "beneficiary_id"),
        @Index(name = "idx_override_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_override_request"))
    private DistributionRequest distributionRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_override_beneficiary"))
    private Beneficiary beneficiary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_override_inventory_item"))
    private InventoryItem inventoryItem;

    /** The earlier distribution that triggered the warning. */
    @Column(name = "previous_distribution_id")
    private Long previousDistributionId;

    @Column(name = "days_since_last_distribution")
    private Integer daysSinceLastDistribution;

    @Column(name = "override_reason", nullable = false, length = 1000)
    private String overrideReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "overridden_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_override_user"))
    private User overriddenBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
