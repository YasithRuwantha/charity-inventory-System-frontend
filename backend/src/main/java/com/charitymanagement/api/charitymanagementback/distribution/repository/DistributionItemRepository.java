package com.charitymanagement.api.charitymanagementback.distribution.repository;

import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DistributionItemRepository extends JpaRepository<DistributionItem, Long> {

    List<DistributionItem> findByDistributionRequestId(Long distributionRequestId);

    /**
     * Completed lines where this beneficiary already received this item, newest first. Used both
     * by the duplicate check and by the {@code /check-duplicate} endpoint; callers pass a
     * {@code Pageable} of size 1 when they only need the most recent one.
     */
    @Query("""
            select i from DistributionItem i
            where i.distributionRequest.beneficiary.id = :beneficiaryId
              and i.inventoryItem.id = :inventoryItemId
              and i.distributionRequest.status = 'COMPLETED'
              and i.distributionRequest.completedAt >= :since
              and i.distributedQuantity > 0
            order by i.distributionRequest.completedAt desc
            """)
    List<DistributionItem> findRecentCompleted(@Param("beneficiaryId") Long beneficiaryId,
                                               @Param("inventoryItemId") Long inventoryItemId,
                                               @Param("since") LocalDateTime since,
                                               Pageable pageable);

    @Query("""
            select coalesce(sum(i.distributedQuantity), 0) from DistributionItem i
            where i.distributionRequest.beneficiary.id = :beneficiaryId
              and i.distributionRequest.status = 'COMPLETED'
            """)
    long sumDistributedQuantityByBeneficiary(@Param("beneficiaryId") Long beneficiaryId);

    @Query("""
            select count(i) from DistributionItem i
            where i.distributionRequest.beneficiary.id = :beneficiaryId
              and i.distributionRequest.status = 'COMPLETED'
            """)
    long countItemLinesByBeneficiary(@Param("beneficiaryId") Long beneficiaryId);

    @Query("""
            select i.inventoryItem.id, i.inventoryItem.itemCode, i.inventoryItem.itemName,
                   coalesce(sum(i.distributedQuantity), 0)
            from DistributionItem i
            where i.distributionRequest.status = 'COMPLETED'
              and (:startDate is null or i.distributionRequest.requestDate >= :startDate)
              and (:endDate is null or i.distributionRequest.requestDate <= :endDate)
              and (:itemId is null or i.inventoryItem.id = :itemId)
            group by i.inventoryItem.id, i.inventoryItem.itemCode, i.inventoryItem.itemName
            order by coalesce(sum(i.distributedQuantity), 0) desc
            """)
    List<Object[]> aggregateByItem(@Param("startDate") java.time.LocalDate startDate,
                                   @Param("endDate") java.time.LocalDate endDate,
                                   @Param("itemId") Long itemId);

    /** True when any completed distribution still references this inventory item. */
    boolean existsByInventoryItemId(Long inventoryItemId);
}
