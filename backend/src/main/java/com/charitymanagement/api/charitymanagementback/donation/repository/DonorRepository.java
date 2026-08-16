package com.charitymanagement.api.charitymanagementback.donation.repository;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long>, JpaSpecificationExecutor<Donor> {

    Optional<Donor> findByDonorCodeIgnoreCase(String donorCode);

    Optional<Donor> findByDonorNameIgnoreCase(String donorName);

    boolean existsByDonorNameIgnoreCase(String donorName);

    boolean existsByDonorNameIgnoreCaseAndIdNot(String donorName, Long id);

    long countByStatus(RecordStatus status);
}
