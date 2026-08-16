package com.charitymanagement.api.charitymanagementback.donation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateDonationRequest {

    @NotNull(message = "Donor is required")
    private Long donorId;

    @NotNull(message = "Donation date is required")
    @PastOrPresent(message = "Donation date cannot be in the future")
    private LocalDate donationDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "A donation must contain at least one item")
    @Valid
    private List<DonationItemRequest> items;
}
