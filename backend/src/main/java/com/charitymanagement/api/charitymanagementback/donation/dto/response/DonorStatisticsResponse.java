package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonorStatisticsResponse {

    private Long donorId;
    private String donorCode;
    private String donorName;
    private long totalDonations;
    private long totalItemLines;
    private long totalQuantityDonated;
    private LocalDate lastDonationDate;
    /** Ten most recent donations, newest first. */
    private List<DonationResponse> recentDonations;
}
