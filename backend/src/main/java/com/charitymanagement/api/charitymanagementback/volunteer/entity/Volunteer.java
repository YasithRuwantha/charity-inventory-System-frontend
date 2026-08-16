package com.charitymanagement.api.charitymanagementback.volunteer.entity;

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

import java.time.LocalDate;

@Entity
@Table(name = "volunteers",
        uniqueConstraints = @UniqueConstraint(name = "uk_volunteer_code", columnNames = "volunteer_code"),
        indexes = {
                @Index(name = "idx_volunteer_name", columnList = "volunteer_name"),
                @Index(name = "idx_volunteer_status", columnList = "status"),
                @Index(name = "idx_volunteer_email", columnList = "email")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Volunteer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "volunteer_code", nullable = false, length = 32)
    private String volunteerCode;

    @Column(name = "volunteer_name", nullable = false, length = 200)
    private String volunteerName;

    @Column(name = "phone", length = 40)
    private String phone;

    /**
     * Optional. When it matches the email of a VOLUNTEER user account, that user may read their own
     * tasks and update their status — this is the only link between the two records.
     */
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "notes", length = 1000)
    private String notes;
}
