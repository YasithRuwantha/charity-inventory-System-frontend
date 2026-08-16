package com.charitymanagement.api.charitymanagementback.inventory.repository;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    Optional<InventoryCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByStatus(RecordStatus status);

    @Query("""
            select c from InventoryCategory c
            where (:search is null or lower(c.name) like :search or lower(c.description) like :search)
              and (:status is null or c.status = :status)
            """)
    Page<InventoryCategory> search(@Param("search") String search,
                                   @Param("status") RecordStatus status,
                                   Pageable pageable);
}
