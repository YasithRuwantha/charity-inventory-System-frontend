package com.charitymanagement.api.charitymanagementback.inventory.repository;

import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.enums.InventoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {

    /**
     * Row-level write lock. Every stock deduction goes through this so two concurrent requests
     * cannot both read the same "available" quantity and drive it negative.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);

    Optional<InventoryItem> findByItemCodeIgnoreCase(String itemCode);

    /** A name can repeat across categories, so this deliberately returns every match. */
    List<InventoryItem> findByItemNameIgnoreCaseAndArchivedFalse(String itemName);

    boolean existsByItemNameIgnoreCaseAndCategoryIdAndArchivedFalse(String itemName, Long categoryId);

    boolean existsByCategoryIdAndArchivedFalse(Long categoryId);

    long countByArchivedFalse();

    long countByArchivedFalseAndStatus(InventoryStatus status);

    @Query("select coalesce(sum(i.quantity), 0) from InventoryItem i where i.archived = false")
    long sumTotalQuantity();

    @Query("""
            select count(i) from InventoryItem i
            where i.archived = false and i.expiryDate is not null
              and i.expiryDate >= :today and i.expiryDate <= :horizon
            """)
    long countExpiringBetween(@Param("today") LocalDate today, @Param("horizon") LocalDate horizon);

    // ── Category-scoped variants, used when a report is filtered by ?categoryId= ──────────────
    // A null categoryId means "every category", so one query serves both the filtered and the
    // unfiltered report.

    @Query("select count(i) from InventoryItem i "
            + "where i.archived = false and (:categoryId is null or i.category.id = :categoryId)")
    long countByCategory(@Param("categoryId") Long categoryId);

    @Query("select coalesce(sum(i.quantity), 0) from InventoryItem i "
            + "where i.archived = false and (:categoryId is null or i.category.id = :categoryId)")
    long sumQuantityByCategory(@Param("categoryId") Long categoryId);

    @Query("select count(i) from InventoryItem i where i.archived = false and i.status = :status "
            + "and (:categoryId is null or i.category.id = :categoryId)")
    long countByStatusAndCategory(@Param("status") InventoryStatus status,
                                  @Param("categoryId") Long categoryId);

    @Query("""
            select count(i) from InventoryItem i
            where i.archived = false and i.quantity <= i.minimumStockLevel and i.quantity > 0
              and (:categoryId is null or i.category.id = :categoryId)
            """)
    long countLowStockByCategory(@Param("categoryId") Long categoryId);

    @Query("select count(i) from InventoryItem i where i.archived = false and i.quantity = 0 "
            + "and (:categoryId is null or i.category.id = :categoryId)")
    long countOutOfStockByCategory(@Param("categoryId") Long categoryId);

    @Query("""
            select count(i) from InventoryItem i
            where i.archived = false and i.expiryDate is not null and i.expiryDate < :today
              and (:categoryId is null or i.category.id = :categoryId)
            """)
    long countExpiredByCategory(@Param("today") LocalDate today, @Param("categoryId") Long categoryId);

    @Query("""
            select count(i) from InventoryItem i
            where i.archived = false and i.expiryDate is not null
              and i.expiryDate >= :today and i.expiryDate <= :horizon
              and (:categoryId is null or i.category.id = :categoryId)
            """)
    long countExpiringBetweenByCategory(@Param("today") LocalDate today,
                                        @Param("horizon") LocalDate horizon,
                                        @Param("categoryId") Long categoryId);

    @Query("""
            select i from InventoryItem i
            where i.archived = false and i.expiryDate is not null
              and i.expiryDate >= :today and i.expiryDate <= :horizon
            order by i.expiryDate asc
            """)
    List<InventoryItem> findExpiringBetween(@Param("today") LocalDate today,
                                            @Param("horizon") LocalDate horizon);

    @Query("""
            select c.id, c.name, count(i), coalesce(sum(i.quantity), 0)
            from InventoryCategory c left join InventoryItem i on i.category = c and i.archived = false
            group by c.id, c.name
            order by c.name asc
            """)
    List<Object[]> aggregateByCategory();

    @Query("""
            select i from InventoryItem i
            where i.archived = false and i.quantity <= i.minimumStockLevel and i.quantity > 0
            order by i.quantity asc
            """)
    List<InventoryItem> findLowStock();

    @Query("select count(i) from InventoryItem i "
            + "where i.archived = false and i.quantity <= i.minimumStockLevel and i.quantity > 0")
    long countLowStock();

    @Query("select count(i) from InventoryItem i where i.archived = false and i.quantity = 0")
    long countOutOfStock();

    @Query("select i from InventoryItem i where i.archived = false and i.quantity = 0 order by i.itemName asc")
    List<InventoryItem> findOutOfStock();

    /**
     * Counted from the expiry date rather than the cached status: an item can pass its expiry date
     * without any stock movement to trigger a status recalculation.
     */
    @Query("select count(i) from InventoryItem i "
            + "where i.archived = false and i.expiryDate is not null and i.expiryDate < :today")
    long countExpired(@Param("today") LocalDate today);

    @Query("select i from InventoryItem i "
            + "where i.archived = false and i.expiryDate is not null and i.expiryDate < :today "
            + "order by i.expiryDate asc")
    List<InventoryItem> findExpired(@Param("today") LocalDate today);
}
