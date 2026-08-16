package com.charitymanagement.api.charitymanagementback.donation.entity;

import com.charitymanagement.api.charitymanagementback.common.entity.BaseEntity;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "donors",
        uniqueConstraints = @UniqueConstraint(name = "uk_donor_code", columnNames = "donor_code"),
        indexes = {
                @Index(name = "idx_donor_name", columnList = "donor_name"),
                @Index(name = "idx_donor_status", columnList = "status"),
                @Index(name = "idx_donor_type", columnList = "donor_type")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donor_code", nullable = false, length = 32)
    private String donorCode;

    @Column(name = "donor_name", nullable = false, length = 200)
    private String donorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "donor_type", nullable = false, length = 20)
    private DonorType donorType;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RecordStatus status = RecordStatus.ACTIVE;
}
