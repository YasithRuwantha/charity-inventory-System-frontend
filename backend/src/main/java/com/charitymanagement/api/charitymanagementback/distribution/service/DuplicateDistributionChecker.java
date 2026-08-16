package com.charitymanagement.api.charitymanagementback.distribution.service;

import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.common.config.CharityProperties;
import com.charitymanagement.api.charitymanagementback.distribution.dto.response.DuplicateDistributionResponse;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionItem;
import com.charitymanagement.api.charitymanagementback.distribution.repository.DistributionItemRepository;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Answers "has this beneficiary already received this item recently?".
 *
 * <p>The window comes from {@code charity.distribution.duplicate-days} (30 by default). A hit is a
 * warning, not a ban: repeated assistance is often legitimate, so the decision to proceed is left
 * to an ADMIN override rather than being refused outright.
 */
@Service
@RequiredArgsConstructor
public class DuplicateDistributionChecker {

    private final DistributionItemRepository distributionItemRepository;
    private final CharityProperties properties;

    public int windowDays() {
        return properties.getDistribution().getDuplicateDays();
    }

    @Transactional(readOnly = true)
    public DuplicateDistributionResponse check(Beneficiary beneficiary, InventoryItem inventoryItem) {
        int windowDays = windowDays();
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);

        List<DistributionItem> recent = distributionItemRepository.findRecentCompleted(
                beneficiary.getId(), inventoryItem.getId(), since, PageRequest.of(0, 1));

        DuplicateDistributionResponse.DuplicateDistributionResponseBuilder builder =
                DuplicateDistributionResponse.builder()
                        .beneficiaryId(beneficiary.getId())
                        .beneficiaryName(beneficiary.getBeneficiaryName())
                        .inventoryItemId(inventoryItem.getId())
                        .itemName(inventoryItem.getItemName())
                        .duplicateWindowDays(windowDays);

        if (recent.isEmpty()) {
            return builder
                    .duplicateWarning(false)
                    .message("No distribution of this item to this beneficiary in the last "
                            + windowDays + " days.")
                    .build();
        }

        DistributionItem previous = recent.get(0);
        LocalDateTime completedAt = previous.getDistributionRequest().getCompletedAt();
        int daysSince = (int) Duration.between(completedAt, LocalDateTime.now()).toDays();

        return builder
                .duplicateWarning(true)
                .message("This beneficiary received this item recently.")
                .previousDistributionId(previous.getDistributionRequest().getId())
                .previousDistributionReference(previous.getDistributionRequest().getRequestReference())
                .lastDistributionDate(completedAt)
                .daysSinceLastDistribution(daysSince)
                .quantityPreviouslyReceived(previous.getDistributedQuantity())
                .build();
    }
}
