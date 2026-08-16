package com.charitymanagement.api.charitymanagementback.beneficiary.dto.response;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BeneficiaryResponse {

    private Long id;
    private String beneficiaryCode;
    private String beneficiaryName;
    private String identificationNumber;
    private Integer familySize;
    private String contactNumber;
    private String address;
    private PriorityLevel priorityLevel;
    private RecordStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
