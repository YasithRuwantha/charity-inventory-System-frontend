package com.charitymanagement.api.charitymanagementback.volunteer.dto.response;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerResponse {

    private Long id;
    private String volunteerCode;
    private String volunteerName;
    private String phone;
    private String email;
    private String address;
    private RecordStatus status;
    private LocalDate joinedDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
