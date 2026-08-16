package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveDistributionRequest {

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
