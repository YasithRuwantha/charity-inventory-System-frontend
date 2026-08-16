package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonorResponse {

    private Long id;
    private String donorCode;
    private String donorName;
    private DonorType donorType;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private RecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
