package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Dry run: nothing is written to the database. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkDonationPreviewResponse {

    private String fileName;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    /** Donations that would be created, one per donor + donation date. */
    private int donationsToCreate;
    private List<BulkRowResult> rows;
}
