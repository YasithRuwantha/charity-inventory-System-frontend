package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** One parsed spreadsheet row, with every validation error found on it. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkRowResult {

    /** 1-based row number as it appears in the uploaded file, header excluded. */
    private int rowNumber;
    private boolean valid;

    private String donor;
    private String item;
    private String category;
    private String quantity;
    private String unit;
    private String expiryDate;
    private String donationDate;

    private List<String> errors;
}
