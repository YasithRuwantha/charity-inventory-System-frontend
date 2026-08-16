package com.charitymanagement.api.charitymanagementback.beneficiary.dto.request;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBeneficiaryRequest {

    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 200, message = "Beneficiary name must not exceed 200 characters")
    private String beneficiaryName;

    @Size(max = 64, message = "Identification number must not exceed 64 characters")
    private String identificationNumber;

    @NotNull(message = "Family size is required")
    @Min(value = 1, message = "Family size must be at least 1")
    private Integer familySize;

    @Size(max = 40, message = "Contact number must not exceed 40 characters")
    private String contactNumber;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Priority level is required")
    private PriorityLevel priorityLevel;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
