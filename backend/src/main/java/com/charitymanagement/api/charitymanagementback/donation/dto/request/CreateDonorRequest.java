package com.charitymanagement.api.charitymanagementback.donation.dto.request;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDonorRequest {

    @NotBlank(message = "Donor name is required")
    @Size(max = 200, message = "Donor name must not exceed 200 characters")
    private String donorName;

    @NotNull(message = "Donor type is required")
    private DonorType donorType;

    @Size(max = 40, message = "Phone must not exceed 40 characters")
    private String phone;

    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private RecordStatus status;
}
