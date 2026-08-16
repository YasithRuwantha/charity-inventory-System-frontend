package com.charitymanagement.api.charitymanagementback.distribution.controller;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.dto.ApiResponse;
import com.charitymanagement.api.charitymanagementback.common.dto.PaginationMeta;
import com.charitymanagement.api.charitymanagementback.common.security.Roles;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.AllocateDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.ApproveDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.CancelDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.CompleteDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.CreateDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.RejectDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.request.UpdateDistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionReportResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DuplicateDistributionResponse;
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
@RequestMapping("/api/distributions")
@RequiredArgsConstructor
@Tag(name = "Distributions",
        description = "Aid requests: allocation, approval and the confirmed hand-over that moves stock")
public class DistributionController {

    private final DistributionService distributionService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Create a distribution request",
            description = "Creates a PENDING request with one or more items. Inventory is not touched.")
    public ResponseEntity<ApiResponse<DistributionResponse>> create(
            @Valid @RequestBody CreateDistributionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(distributionService.create(request),
                        "Distribution request created successfully"));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Search distribution requests",
            description = "By reference or beneficiary; filterable by status, priority, beneficiary and date range.")
    public ResponseEntity<ApiResponse<List<DistributionResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DistributionStatus status,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) Long beneficiaryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DistributionResponse> page = distributionService.search(search, status, priority, beneficiaryId,
                startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/check-duplicate")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Check whether a beneficiary recently received an item",
            description = "Returns the previous distribution and how many days ago it happened, using the "
                    + "configured duplicate window (charity.distribution.duplicate-days).")
    public ResponseEntity<ApiResponse<DuplicateDistributionResponse>> checkDuplicate(
            @RequestParam Long beneficiaryId, @RequestParam Long inventoryItemId) {
        return ResponseEntity.ok(ApiResponse.success(
                distributionService.checkDuplicate(beneficiaryId, inventoryItemId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Get one distribution request with its items")
    public ResponseEntity<ApiResponse<DistributionResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.get(id)));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Look up a distribution by its DIST-YYYY-NNNNNN reference")
    public ResponseEntity<ApiResponse<DistributionResponse>> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.getByReference(reference)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Edit a PENDING distribution request",
            description = "Replaces the item list. Requests that are past PENDING cannot be edited.")
    public ResponseEntity<ApiResponse<DistributionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateDistributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.update(id, request),
                "Distribution request updated successfully"));
    }

    @PostMapping("/{id}/allocate")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Allocate inventory against the request",
            description = "Checks availability and expiry per line. Stock is NOT reduced by allocation.")
    public ResponseEntity<ApiResponse<DistributionResponse>> allocate(
            @PathVariable Long id, @Valid @RequestBody AllocateDistributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.allocate(id, request),
                "Inventory allocated successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Approve a PENDING request",
            description = "Requires prior allocation. Stock is still NOT reduced.")
    public ResponseEntity<ApiResponse<DistributionResponse>> approve(
            @PathVariable Long id, @RequestBody(required = false) ApproveDistributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.approve(id, request),
                "Distribution approved successfully"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Reject a request", description = "A rejection reason is mandatory.")
    public ResponseEntity<ApiResponse<DistributionResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectDistributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.reject(id, request),
                "Distribution rejected"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Cancel a request", description = "A cancellation reason is mandatory.")
    public ResponseEntity<ApiResponse<DistributionResponse>> cancel(
            @PathVariable Long id, @Valid @RequestBody CancelDistributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.cancel(id, request),
                "Distribution cancelled"));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Confirm the hand-over and deduct stock",
            description = "The only endpoint that reduces inventory. Refuses expired items, insufficient "
                    + "stock and repeat completion; a duplicate warning returns 409 unless an ADMIN supplies "
                    + "overrideDuplicates with a reason. Any failing line rolls back the whole distribution.")
    public ResponseEntity<ApiResponse<DistributionResponse>> complete(
            @PathVariable Long id, @Valid @RequestBody(required = false) CompleteDistributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.complete(id, request),
                "Distribution completed successfully"));
    }

    @GetMapping("/beneficiary/{beneficiaryId}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Distribution history for a beneficiary")
    public ResponseEntity<ApiResponse<List<DistributionResponse>>> byBeneficiary(
            @PathVariable Long beneficiaryId,
            @RequestParam(required = false) DistributionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DistributionResponse> page = distributionService.search(null, status, null, beneficiaryId,
                startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.paged(page.getContent(), PaginationMeta.of(page)));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Printable report for one distribution",
            description = "Includes beneficiary details, every line and any duplicate override applied.")
    public ResponseEntity<ApiResponse<DistributionReportResponse>> report(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.getReport(id)));
    }
}
