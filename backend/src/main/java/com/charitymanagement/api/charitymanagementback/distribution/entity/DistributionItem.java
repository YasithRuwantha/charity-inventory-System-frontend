package com.charitymanagement.api.charitymanagementback.distribution.entity;

import com.charitymanagement.api.charitymanagementback.common.entity.BaseEntity;
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

/**
 * One inventory line of an aid request.
 *
 * <p>The three quantities are deliberately separate: {@code requestedQuantity} is what was asked
 * for, {@code allocatedQuantity} what the staff earmarked after checking stock, and
 * {@code distributedQuantity} what actually left the store. Only the last one moves inventory.
 */
@Entity
@Table(name = "distribution_items", indexes = {
        @Index(name = "idx_distribution_item_request", columnList = "distribution_request_id"),
        @Index(name = "idx_distribution_item_inventory", columnList = "inventory_item_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_distribution_item_request"))
    private DistributionRequest distributionRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_distribution_item_inventory"))
    private InventoryItem inventoryItem;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "allocated_quantity", nullable = false)
    @Builder.Default
    private Integer allocatedQuantity = 0;

    @Column(name = "distributed_quantity", nullable = false)
    @Builder.Default
    private Integer distributedQuantity = 0;

    @Column(name = "notes", length = 500)
    private String notes;
}
