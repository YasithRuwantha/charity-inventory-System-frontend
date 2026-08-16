package com.charitymanagement.api.charitymanagementback.distribution.repository;

import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DistributionSpecifications {

    private DistributionSpecifications() {
    }

    public static Specification<DistributionRequest> build(String search,
                                                           DistributionStatus status,
                                                           PriorityLevel priority,
                                                           Long beneficiaryId,
                                                           LocalDate startDate,
                                                           LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                var beneficiary = root.join("beneficiary", JoinType.LEFT);
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("requestReference")), pattern),
                        cb.like(cb.lower(beneficiary.get("beneficiaryCode")), pattern),
                        cb.like(cb.lower(beneficiary.get("beneficiaryName")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (beneficiaryId != null) {
                predicates.add(cb.equal(root.get("beneficiary").get("id"), beneficiaryId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestDate"), endDate));
            }
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
