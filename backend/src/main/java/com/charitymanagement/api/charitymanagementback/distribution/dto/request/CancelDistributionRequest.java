package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelDistributionRequest {

    @NotBlank(message = "A cancellation reason is required")
    @Size(max = 1000, message = "Cancellation reason must not exceed 1000 characters")
    private String reason;
}
