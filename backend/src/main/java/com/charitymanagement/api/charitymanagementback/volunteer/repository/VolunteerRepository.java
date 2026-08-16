package com.charitymanagement.api.charitymanagementback.volunteer.repository;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long>,
        JpaSpecificationExecutor<Volunteer> {

    Optional<Volunteer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByStatus(RecordStatus status);
}
