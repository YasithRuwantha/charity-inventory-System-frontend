package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkDonationImportResponse {

    private String fileName;
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private int skippedRows;
    private int createdDonations;
    private long totalQuantityImported;
    private List<String> createdDonationReferences;
    /** Every rejected row, with its row number and reasons — nothing is discarded silently. */
    private List<BulkRowResult> errors;
    private String message;
}
