package com.charitymanagement.api.charitymanagementback.common.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row per human-readable reference series (INV, DONOR, DON-2026, ...). */
@Entity
@Table(name = "reference_sequences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceSequence {

    @Id
    @Column(name = "sequence_name", length = 64, nullable = false)
    private String sequenceName;

    @Column(name = "next_value", nullable = false)
    private long nextValue;
}
