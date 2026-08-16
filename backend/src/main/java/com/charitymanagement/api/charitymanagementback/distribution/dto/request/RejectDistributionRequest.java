package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectDistributionRequest {

    @NotBlank(message = "A rejection reason is required")
    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    private String reason;
}
