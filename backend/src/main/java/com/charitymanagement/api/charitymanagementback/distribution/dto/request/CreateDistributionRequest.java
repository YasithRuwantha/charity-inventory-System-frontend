package com.charitymanagement.api.charitymanagementback.distribution.dto.request;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateDistributionRequest {

    @NotNull(message = "Beneficiary id is required")
    private Long beneficiaryId;

    @NotNull(message = "Request date is required")
    @PastOrPresent(message = "Request date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requestDate;

    /** Defaults to the beneficiary's own priority level when omitted. */
    private PriorityLevel priority;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "A distribution request must contain at least one item")
    @Valid
    private List<DistributionItemRequest> items;
}
