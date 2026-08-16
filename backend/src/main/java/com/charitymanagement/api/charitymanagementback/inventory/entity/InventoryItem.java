package com.charitymanagement.api.charitymanagementback.inventory.entity;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.entity.BaseEntity;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_item_code", columnNames = "item_code"),
        indexes = {
                @Index(name = "idx_item_name", columnList = "item_name"),
                @Index(name = "idx_item_category", columnList = "category_id"),
                @Index(name = "idx_item_status", columnList = "status"),
                @Index(name = "idx_item_expiry", columnList = "expiry_date")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, length = 32)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_item_category"))
    private InventoryCategory category;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private UnitOfMeasure unit;

    @Column(name = "minimum_stock_level", nullable = false)
    @Builder.Default
    private Integer minimumStockLevel = 0;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryStatus status = InventoryStatus.OUT_OF_STOCK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_item_created_by"))
    private User createdBy;

    /** Soft delete. Archived items keep their history but cannot receive or release stock. */
    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Version
    @Column(name = "version")
    private Long version;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Derives {@link InventoryStatus} from the current quantity and expiry date.
     * Expiry outranks quantity: expired stock is unusable however much of it there is.
     */
    public void recalculateStatus() {
        if (isExpired()) {
            this.status = InventoryStatus.EXPIRED;
        } else if (quantity == null || quantity == 0) {
            this.status = InventoryStatus.OUT_OF_STOCK;
        } else if (minimumStockLevel != null && quantity <= minimumStockLevel) {
            this.status = InventoryStatus.LOW_STOCK;
        } else {
            this.status = InventoryStatus.IN_STOCK;
        }
    }
}
