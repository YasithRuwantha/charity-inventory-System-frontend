package com.charitymanagement.api.charitymanagementback.donation.controller;

import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.dto.request.StatusUpdateRequest;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.CreateDonorRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.UpdateDonorRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonorResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonorStatisticsResponse;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonorType;
import com.charitymanagement.api.charitymanagementback.donation.service.DonationService;
import com.charitymanagement.api.charitymanagementback.donation.service.DonorService;
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
@RequestMapping("/api/donors")
@RequiredArgsConstructor
@Tag(name = "Donors", description = "Donor registry, donation history and contribution statistics")
public class DonorController {

    private final DonorService donorService;
    private final DonationService donationService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Register a donor", description = "Donor codes (DONOR-000001) are generated.")
    public ResponseEntity<ApiResponse<DonorResponse>> create(@Valid @RequestBody CreateDonorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(donorService.create(request), "Donor registered successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Search donors", description = "By name, code, email or phone; filterable and paged.")
    public ResponseEntity<ApiResponse<List<DonorResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DonorType donorType,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DonorResponse> page = donorService.search(search, donorType, status, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Get one donor")
    public ResponseEntity<ApiResponse<DonorResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(donorService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Update a donor")
    public ResponseEntity<ApiResponse<DonorResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateDonorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(donorService.update(id, request),
                "Donor updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Activate or deactivate a donor")
    public ResponseEntity<ApiResponse<DonorResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(donorService.changeStatus(id, request.getStatus()),
                "Donor status updated successfully"));
    }

    @GetMapping("/{id}/donations")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Full donation history for a donor",
            description = "Each entry includes its item lines, not just the donation header.")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> donations(
            @PathVariable Long id,
            @RequestParam(required = false) DonationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        donorService.get(id);
        Page<DonationResponse> page = donationService.search(null, id, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Contribution statistics for a donor")
    public ResponseEntity<ApiResponse<DonorStatisticsResponse>> statistics(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(donationService.getDonorStatistics(id)));
    }
}
