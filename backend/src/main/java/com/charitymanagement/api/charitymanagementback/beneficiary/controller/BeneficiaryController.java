package com.charitymanagement.api.charitymanagementback.beneficiary.controller;

import com.charitymanagement.api.charitymanagementback.beneficiary.dto.request.CreateBeneficiaryRequest;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.request.UpdateBeneficiaryRequest;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.response.BeneficiaryResponse;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.response.BeneficiaryStatisticsResponse;
import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.beneficiary.service.BeneficiaryService;
import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.dto.request.StatusUpdateRequest;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionResponse;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import com.charitymanagement.api.charitymanagementback.distribution.service.DistributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries",
        description = "Beneficiary registry, family information, priority and distribution history")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final DistributionService distributionService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Register a beneficiary",
            description = "Beneficiary codes (BEN-000001) are generated. Family size must be at least 1 and an "
                    + "identification number, when supplied, must be unique.")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> create(
            @Valid @RequestBody CreateBeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(beneficiaryService.create(request),
                        "Beneficiary registered successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Search beneficiaries",
            description = "By code, name, identification number or contact number; filterable by priority and "
                    + "status, and paged.")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BeneficiaryResponse> page = beneficiaryService.search(search, priority, status, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Get one beneficiary")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(beneficiaryService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Update a beneficiary")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateBeneficiaryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(beneficiaryService.update(id, request),
                "Beneficiary updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Activate or deactivate a beneficiary",
            description = "Inactive beneficiaries keep their history but cannot receive new distributions.")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(beneficiaryService.changeStatus(id, request.getStatus()),
                "Beneficiary status updated successfully"));
    }

    @GetMapping("/{id}/distributions")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Distribution history for a beneficiary",
            description = "Each entry includes its item lines with requested, allocated and distributed "
                    + "quantities.")
    public ResponseEntity<ApiResponse<List<DistributionResponse>>> distributions(
            @PathVariable Long id,
            @RequestParam(required = false) DistributionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        beneficiaryService.get(id);
        Page<DistributionResponse> page = distributionService.search(null, status, null, id,
                startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Aid statistics for a beneficiary",
            description = "Totals received, last distribution date, outstanding requests and recent history.")
    public ResponseEntity<ApiResponse<BeneficiaryStatisticsResponse>> statistics(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.getBeneficiaryStatistics(id)));
    }
}
