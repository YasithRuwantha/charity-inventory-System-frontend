package com.charitymanagement.api.charitymanagementback.volunteer.repository;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.Volunteer;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.VolunteerTask;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VolunteerSpecifications {

    private VolunteerSpecifications() {
    }

    public static Specification<Volunteer> volunteers(String search, RecordStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("volunteerCode")), pattern),
                        cb.like(cb.lower(root.get("volunteerName")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VolunteerTask> tasks(String search,
                                                     Long volunteerId,
                                                     TaskStatus status,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                var volunteer = root.join("volunteer", JoinType.LEFT);
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                        cb.like(cb.lower(volunteer.get("volunteerCode")), pattern),
                        cb.like(cb.lower(volunteer.get("volunteerName")), pattern)));
            }
            if (volunteerId != null) {
                predicates.add(cb.equal(root.get("volunteer").get("id"), volunteerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("taskDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("taskDate"), endDate));
            }
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
