package com.charitymanagement.api.charitymanagementback.inventory.service;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryCategory;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Inserts the starter categories on first run. Existing rows are never modified — a name that is
 * already present is skipped, so this is safe to run against a populated database.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "charity.seed.default-categories", havingValue = "true", matchIfMissing = true)
public class DefaultCategorySeeder {

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySeeder.class);

    private static final List<Map.Entry<String, String>> DEFAULTS = List.of(
            Map.entry("Food", "Food and nutrition supplies"),
            Map.entry("Clothing", "Garments, footwear and bedding"),
            Map.entry("Medicine", "Medical and pharmaceutical supplies"),
            Map.entry("Hygiene", "Sanitation and personal care items"),
            Map.entry("Education", "Books, stationery and learning materials"),
            Map.entry("Household", "Kitchenware, utensils and household goods"),
            Map.entry("Other", "Items that do not fit the categories above"));

    private final InventoryCategoryRepository categoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        int created = 0;
        for (Map.Entry<String, String> entry : DEFAULTS) {
            if (categoryRepository.existsByNameIgnoreCase(entry.getKey())) {
                continue;
            }
            categoryRepository.save(InventoryCategory.builder()
                    .name(entry.getKey())
                    .description(entry.getValue())
                    .status(RecordStatus.ACTIVE)
                    .build());
            created++;
        }
        if (created > 0) {
            log.info("Seeded {} default inventory categories", created);
        }
    }
}
