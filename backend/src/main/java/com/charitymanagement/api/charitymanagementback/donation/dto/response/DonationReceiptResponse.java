package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Flat, self-contained receipt: everything a printed or PDF receipt needs is present here, so a
 * renderer never has to call back for more data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonationReceiptResponse {

    private String organizationName;
    private String organizationAddress;
    private String organizationContact;

    private String donationReference;
    private LocalDate donationDate;

    private String donorCode;
    private String donorName;
    private String donorType;
    private String donorPhone;
    private String donorEmail;
    private String donorAddress;

    private List<DonationItemResponse> items;
    private int totalItems;
    private long totalQuantity;

    private String receivedByName;
    private String notes;
    private String status;
    private LocalDateTime generatedAt;
}
