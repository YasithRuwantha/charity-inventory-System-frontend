package com.charitymanagement.api.charitymanagementback.inventory.repository;

import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryTransaction;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {

    List<InventoryTransaction> findByReferenceTypeAndReferenceIdOrderByIdAsc(ReferenceType referenceType,
                                                                             Long referenceId);

    boolean existsByReferenceTypeAndReferenceId(ReferenceType referenceType, Long referenceId);

    @Query("""
            select t.transactionType, count(t), coalesce(sum(abs(t.quantity)), 0)
            from InventoryTransaction t
            where (:startDate is null or t.createdAt >= :startDate)
              and (:endDate is null or t.createdAt <= :endDate)
            group by t.transactionType
            """)
    List<Object[]> aggregateByType(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}
