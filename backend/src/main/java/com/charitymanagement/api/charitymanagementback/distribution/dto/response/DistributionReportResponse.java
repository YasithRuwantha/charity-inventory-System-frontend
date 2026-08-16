package com.charitymanagement.api.charitymanagementback.distribution.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Printable summary of a single distribution: the request, who handled each step, what actually
 * left the store, and any duplicate override that was applied. Shaped so a PDF can be rendered
 * from it later without touching the service layer.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DistributionReportResponse {

    private String organizationName;
    private String organizationAddress;
    private String organizationContact;

    private DistributionResponse distribution;

    private String beneficiaryIdentificationNumber;
    private Integer beneficiaryFamilySize;
    private String beneficiaryContactNumber;
    private String beneficiaryAddress;

    private List<DistributionOverrideResponse> overrides;

    private LocalDateTime generatedAt;
}
