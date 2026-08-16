package com.charitymanagement.api.charitymanagementback.donation.entity;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.entity.BaseEntity;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_donation_reference", columnNames = "donation_reference"),
                @UniqueConstraint(name = "uk_donation_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_donation_donor", columnList = "donor_id"),
                @Index(name = "idx_donation_date", columnList = "donation_date"),
                @Index(name = "idx_donation_status", columnList = "status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donation_reference", nullable = false, length = 40)
    private String donationReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_donation_donor"))
    private Donor donor;

    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", foreignKey = @ForeignKey(name = "fk_donation_received_by"))
    private User receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DonationStatus status = DonationStatus.RECEIVED;

    /** Optional client-supplied key that makes a retried POST return the original donation. */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    /**
     * Cascade covers persist/merge only — never REMOVE, so donation history cannot be deleted
     * out from under the inventory ledger.
     */
    @OneToMany(mappedBy = "donation", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<DonationItem> items = new ArrayList<>();

    public void addItem(DonationItem item) {
        item.setDonation(this);
        this.items.add(item);
    }
}
