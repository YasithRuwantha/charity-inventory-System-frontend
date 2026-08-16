package com.charitymanagement.api.charitymanagementback.audit.repository;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:userId is null or a.userId = :userId)
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:startDate is null or a.createdAt >= :startDate)
              and (:endDate is null or a.createdAt <= :endDate)
            """)
    Page<AuditLog> search(@Param("userId") Long userId,
                          @Param("action") AuditAction action,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate,
                          Pageable pageable);

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();
}
