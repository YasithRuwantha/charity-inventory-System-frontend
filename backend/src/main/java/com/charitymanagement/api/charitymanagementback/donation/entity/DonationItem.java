package com.charitymanagement.api.charitymanagementback.donation.entity;

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

import java.time.LocalDate;

@Entity
@Table(name = "donation_items", indexes = {
        @Index(name = "idx_donation_item_donation", columnList = "donation_id"),
        @Index(name = "idx_donation_item_inventory", columnList = "inventory_item_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_donation_item_donation"))
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_donation_item_inventory"))
    private InventoryItem inventoryItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Expiry of this particular donated batch, when the donor supplied one. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "notes", length = 500)
    private String notes;
}
