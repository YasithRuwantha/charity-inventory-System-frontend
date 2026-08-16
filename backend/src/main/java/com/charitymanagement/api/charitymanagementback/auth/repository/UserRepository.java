package com.charitymanagement.api.charitymanagementback.auth.repository;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /**
     * Admin user listing. A null {@code status} filter matches rows whose status column is still
     * null — those pre-date the column and are treated as ACTIVE everywhere else.
     */
    @Query("""
            select u from User u
            where (:search is null
                   or lower(u.name) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
              and (:role is null or u.role = :role)
              and (:status is null
                   or u.status = :status
                   or (:status = com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus.ACTIVE
                       and u.status is null))
            """)
    Page<User> search(@Param("search") String search,
                      @Param("role") UserRole role,
                      @Param("status") UserStatus status,
                      Pageable pageable);

    /** Used to refuse the change that would leave the system with no way in. */
    @Query("""
            select count(u) from User u
            where u.role = com.charitymanagement.api.charitymanagementback.auth.entity.UserRole.ADMIN
              and (u.status is null
                   or u.status = com.charitymanagement.api.charitymanagementback.auth.entity.UserStatus.ACTIVE)
            """)
    long countActiveAdmins();
}