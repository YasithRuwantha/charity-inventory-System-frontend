package com.charitymanagement.api.charitymanagementback.beneficiary.repository;

import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository
        extends JpaRepository<Beneficiary, Long>, JpaSpecificationExecutor<Beneficiary> {

    Optional<Beneficiary> findByIdentificationNumberIgnoreCase(String identificationNumber);

    boolean existsByIdentificationNumberIgnoreCase(String identificationNumber);

    boolean existsByIdentificationNumberIgnoreCaseAndIdNot(String identificationNumber, Long id);

    long countByStatus(RecordStatus status);

    long countByPriorityLevel(PriorityLevel priorityLevel);

    @Query("select b.priorityLevel, count(b) from Beneficiary b group by b.priorityLevel")
    List<Object[]> aggregateByPriority();

    @Query("select b.status, count(b) from Beneficiary b group by b.status")
    List<Object[]> aggregateByStatus();
}
