package com.charitymanagement.api.charitymanagementback.inventory.repository;

import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ExpiryFilter;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Type-safe, parameter-bound predicates for the inventory search endpoint. Building the query with
 * the Criteria API keeps user input out of the SQL text entirely.
 */
public final class InventoryItemSpecifications {

    private InventoryItemSpecifications() {
    }

    public static Specification<InventoryItem> build(String search,
                                                     Long categoryId,
                                                     InventoryStatus status,
                                                     ExpiryFilter expiryFilter,
                                                     boolean includeArchived) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!includeArchived) {
                predicates.add(cb.isFalse(root.get("archived")));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("itemName")), pattern),
                        cb.like(cb.lower(root.get("itemCode")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (expiryFilter != null) {
                LocalDate today = LocalDate.now();
                switch (expiryFilter) {
                    case EXPIRED -> predicates.add(cb.and(
                            cb.isNotNull(root.get("expiryDate")),
                            cb.lessThan(root.get("expiryDate"), today)));
                    case EXPIRING_7_DAYS -> predicates.add(cb.and(
                            cb.isNotNull(root.get("expiryDate")),
                            cb.between(root.get("expiryDate"), today, today.plusDays(7))));
                    case EXPIRING_30_DAYS -> predicates.add(cb.and(
                            cb.isNotNull(root.get("expiryDate")),
                            cb.between(root.get("expiryDate"), today, today.plusDays(30))));
                    case NO_EXPIRY -> predicates.add(cb.isNull(root.get("expiryDate")));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
