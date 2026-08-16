package com.charitymanagement.api.charitymanagementback.common.dto.request;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Body of the shared {@code PATCH /{id}/status} endpoints. */
@Data
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private RecordStatus status;
}
