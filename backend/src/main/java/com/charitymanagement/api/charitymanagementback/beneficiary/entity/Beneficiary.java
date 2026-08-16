package com.charitymanagement.api.charitymanagementback.beneficiary.entity;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.entity.BaseEntity;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
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
@Table(name = "beneficiaries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_beneficiary_code", columnNames = "beneficiary_code"),
                @UniqueConstraint(name = "uk_beneficiary_identification", columnNames = "identification_number")
        },
        indexes = {
                @Index(name = "idx_beneficiary_name", columnList = "beneficiary_name"),
                @Index(name = "idx_beneficiary_priority", columnList = "priority_level"),
                @Index(name = "idx_beneficiary_status", columnList = "status"),
                @Index(name = "idx_beneficiary_contact", columnList = "contact_number")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beneficiary_code", nullable = false, length = 32)
    private String beneficiaryCode;

    @Column(name = "beneficiary_name", nullable = false, length = 200)
    private String beneficiaryName;

    /** Optional, but unique when supplied — null values do not collide under a SQL unique index. */
    @Column(name = "identification_number", length = 64)
    private String identificationNumber;

    @Column(name = "family_size", nullable = false)
    private Integer familySize;

    @Column(name = "contact_number", length = 40)
    private String contactNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    @Builder.Default
    private PriorityLevel priorityLevel = PriorityLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;
}
