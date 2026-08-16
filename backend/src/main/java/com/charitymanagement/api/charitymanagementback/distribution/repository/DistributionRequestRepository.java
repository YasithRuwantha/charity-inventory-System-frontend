package com.charitymanagement.api.charitymanagementback.distribution.repository;

import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionRequest;
import com.charitymanagement.api.charitymanagementback.distribution.enums.DistributionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DistributionRequestRepository
        extends JpaRepository<DistributionRequest, Long>, JpaSpecificationExecutor<DistributionRequest> {

    Optional<DistributionRequest> findByRequestReferenceIgnoreCase(String requestReference);

    /**
     * Loads a request under a write lock. Completion takes this lock first, so two concurrent
     * completions of the same request are serialised and the second one sees COMPLETED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from DistributionRequest r where r.id = :id")
    Optional<DistributionRequest> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(DistributionStatus status);

    long countByStatusAndRequestDateBetween(DistributionStatus status, LocalDate start, LocalDate end);

    long countByBeneficiaryIdAndStatus(Long beneficiaryId, DistributionStatus status);

    @Query("select max(r.completedAt) from DistributionRequest r "
            + "where r.beneficiary.id = :beneficiaryId and r.status = 'COMPLETED'")
    LocalDateTime findLastCompletedAt(@Param("beneficiaryId") Long beneficiaryId);

    @Query("select r.status, count(r) from DistributionRequest r "
            + "where (:startDate is null or r.requestDate >= :startDate) "
            + "and (:endDate is null or r.requestDate <= :endDate) "
            + "group by r.status")
    List<Object[]> aggregateByStatus(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("select r.priority, count(r) from DistributionRequest r "
            + "where (:startDate is null or r.requestDate >= :startDate) "
            + "and (:endDate is null or r.requestDate <= :endDate) "
            + "group by r.priority")
    List<Object[]> aggregateByPriority(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /** Monthly completed-distribution counts and distributed quantities for the dashboard trend. */
    @Query("""
            select year(r.requestDate), month(r.requestDate), count(distinct r.id),
                   coalesce(sum(i.distributedQuantity), 0)
            from DistributionRequest r left join r.items i
            where r.requestDate >= :from and r.status = :status
            group by year(r.requestDate), month(r.requestDate)
            order by year(r.requestDate), month(r.requestDate)
            """)
    List<Object[]> aggregateMonthly(@Param("from") LocalDate from,
                                    @Param("status") DistributionStatus status);

    @Query("""
            select r.beneficiary.id, r.beneficiary.beneficiaryCode, r.beneficiary.beneficiaryName,
                   count(distinct r.id), coalesce(sum(i.distributedQuantity), 0)
            from DistributionRequest r left join r.items i
            where r.status = 'COMPLETED'
              and (:startDate is null or r.requestDate >= :startDate)
              and (:endDate is null or r.requestDate <= :endDate)
              and (:beneficiaryId is null or r.beneficiary.id = :beneficiaryId)
            group by r.beneficiary.id, r.beneficiary.beneficiaryCode, r.beneficiary.beneficiaryName
            order by count(distinct r.id) desc
            """)
    List<Object[]> aggregateByBeneficiary(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("beneficiaryId") Long beneficiaryId);

    @Query("""
            select count(distinct r.id), coalesce(sum(i.distributedQuantity), 0), count(i.id)
            from DistributionRequest r left join r.items i
            where r.status = 'COMPLETED'
              and (:startDate is null or r.requestDate >= :startDate)
              and (:endDate is null or r.requestDate <= :endDate)
            """)
    List<Object[]> summariseCompleted(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    List<DistributionRequest> findTop10ByStatusOrderByRequestDateDesc(DistributionStatus status);
}
