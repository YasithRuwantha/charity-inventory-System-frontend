package com.charitymanagement.api.charitymanagementback.beneficiary.repository;

import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BeneficiarySpecifications {

    private BeneficiarySpecifications() {
    }

    public static Specification<Beneficiary> build(String search, PriorityLevel priority, RecordStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("beneficiaryCode")), pattern),
                        cb.like(cb.lower(root.get("beneficiaryName")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("identificationNumber"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("contactNumber"), "")), pattern)));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priorityLevel"), priority));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
