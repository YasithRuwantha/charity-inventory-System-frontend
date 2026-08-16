package com.charitymanagement.api.charitymanagementback.donation.repository;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donation;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donor;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonorType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DonationSpecifications {

    private DonationSpecifications() {
    }

    public static Specification<Donation> donations(String search,
                                                    Long donorId,
                                                    DonationStatus status,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("donationReference")), pattern),
                        cb.like(cb.lower(root.get("donor").get("donorName")), pattern),
                        cb.like(cb.lower(root.get("donor").get("donorCode")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern)));
            }
            if (donorId != null) {
                predicates.add(cb.equal(root.get("donor").get("id"), donorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("donationDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("donationDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Donor> donors(String search, DonorType donorType, RecordStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("donorName")), pattern),
                        cb.like(cb.lower(root.get("donorCode")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern)));
            }
            if (donorType != null) {
                predicates.add(cb.equal(root.get("donorType"), donorType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
