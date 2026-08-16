package com.charitymanagement.api.charitymanagementback.inventory.repository;

import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryTransaction;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class InventoryTransactionSpecifications {

    private InventoryTransactionSpecifications() {
    }

    public static Specification<InventoryTransaction> build(Long itemId,
                                                            TransactionType transactionType,
                                                            ReferenceType referenceType,
                                                            Long performedBy,
                                                            LocalDate startDate,
                                                            LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (itemId != null) {
                predicates.add(cb.equal(root.get("inventoryItem").get("id"), itemId));
            }
            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (referenceType != null) {
                predicates.add(cb.equal(root.get("referenceType"), referenceType));
            }
            if (performedBy != null) {
                predicates.add(cb.equal(root.get("performedBy").get("id"), performedBy));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
