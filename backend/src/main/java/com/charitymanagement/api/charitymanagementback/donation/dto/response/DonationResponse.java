package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Donation header plus its item lines — history endpoints return the detail, not just the header. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonationResponse {

    private Long id;
    private String donationReference;
    private Long donorId;
    private String donorCode;
    private String donorName;
    private LocalDate donationDate;
    private String notes;
    private UserSummary receivedBy;
    private DonationStatus status;
    private int totalItems;
    private long totalQuantity;
    private List<DonationItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
