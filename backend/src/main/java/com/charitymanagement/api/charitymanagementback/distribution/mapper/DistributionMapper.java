package com.charitymanagement.api.charitymanagementback.distribution.mapper;

import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.common.config.CharityProperties;
import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionItemResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionOverrideResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionReportResponse;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DistributionResponse;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionItem;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionOverride;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionRequest;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DistributionMapper {

    private final CharityProperties properties;

    public DistributionItemResponse toResponse(DistributionItem item) {
        InventoryItem inventoryItem = item.getInventoryItem();
        return DistributionItemResponse.builder()
                .id(item.getId())
                .inventoryItemId(inventoryItem.getId())
                .itemCode(inventoryItem.getItemCode())
                .itemName(inventoryItem.getItemName())
                .categoryName(inventoryItem.getCategory() != null ? inventoryItem.getCategory().getName() : null)
                .unit(inventoryItem.getUnit())
                .requestedQuantity(item.getRequestedQuantity())
                .allocatedQuantity(item.getAllocatedQuantity())
                .distributedQuantity(item.getDistributedQuantity())
                .notes(item.getNotes())
                .availableQuantity(inventoryItem.getQuantity())
                .inventoryStatus(inventoryItem.getStatus())
                .expiryDate(inventoryItem.getExpiryDate())
                .expired(inventoryItem.isExpired())
                .build();
    }

    public DistributionResponse toResponse(DistributionRequest request, List<DistributionItem> items) {
        List<DistributionItemResponse> itemResponses = items.stream().map(this::toResponse).toList();
        Beneficiary beneficiary = request.getBeneficiary();
        return DistributionResponse.builder()
                .id(request.getId())
                .requestReference(request.getRequestReference())
                .beneficiaryId(beneficiary.getId())
                .beneficiaryCode(beneficiary.getBeneficiaryCode())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .requestDate(request.getRequestDate())
                .priority(request.getPriority())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status(request.getStatus())
                .requestedBy(UserSummary.from(request.getRequestedBy()))
                .approvedBy(UserSummary.from(request.getApprovedBy()))
                .approvedAt(request.getApprovedAt())
                .rejectedBy(UserSummary.from(request.getRejectedBy()))
                .rejectedAt(request.getRejectedAt())
                .rejectionReason(request.getRejectionReason())
                .cancelledBy(UserSummary.from(request.getCancelledBy()))
                .cancelledAt(request.getCancelledAt())
                .cancellationReason(request.getCancellationReason())
                .completedBy(UserSummary.from(request.getCompletedBy()))
                .completedAt(request.getCompletedAt())
                .totalItems(itemResponses.size())
                .totalRequestedQuantity(sum(itemResponses, DistributionItemResponse::getRequestedQuantity))
                .totalAllocatedQuantity(sum(itemResponses, DistributionItemResponse::getAllocatedQuantity))
                .totalDistributedQuantity(sum(itemResponses, DistributionItemResponse::getDistributedQuantity))
                .items(itemResponses)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public DistributionOverrideResponse toResponse(DistributionOverride override) {
        return DistributionOverrideResponse.builder()
                .id(override.getId())
                .distributionRequestId(override.getDistributionRequest().getId())
                .distributionReference(override.getDistributionRequest().getRequestReference())
                .beneficiaryId(override.getBeneficiary().getId())
                .beneficiaryName(override.getBeneficiary().getBeneficiaryName())
                .inventoryItemId(override.getInventoryItem().getId())
                .itemName(override.getInventoryItem().getItemName())
                .previousDistributionId(override.getPreviousDistributionId())
                .daysSinceLastDistribution(override.getDaysSinceLastDistribution())
                .overrideReason(override.getOverrideReason())
                .overriddenBy(UserSummary.from(override.getOverriddenBy()))
                .createdAt(override.getCreatedAt())
                .build();
    }

    public DistributionReportResponse toReport(DistributionRequest request,
                                               List<DistributionItem> items,
                                               List<DistributionOverride> overrides) {
        Beneficiary beneficiary = request.getBeneficiary();
        return DistributionReportResponse.builder()
                .organizationName(properties.getOrganization().getName())
                .organizationAddress(properties.getOrganization().getAddress())
                .organizationContact(properties.getOrganization().getContact())
                .distribution(toResponse(request, items))
                .beneficiaryIdentificationNumber(beneficiary.getIdentificationNumber())
                .beneficiaryFamilySize(beneficiary.getFamilySize())
                .beneficiaryContactNumber(beneficiary.getContactNumber())
                .beneficiaryAddress(beneficiary.getAddress())
                .overrides(overrides.stream().map(this::toResponse).toList())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private static long sum(List<DistributionItemResponse> items,
                            Function<DistributionItemResponse, Integer> extractor) {
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }
}
