package com.charitymanagement.api.charitymanagementback.common.reference;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReferenceSequenceRepository extends JpaRepository<ReferenceSequence, String> {

    /** Row-level write lock so two concurrent callers can never read the same counter value. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ReferenceSequence s where s.sequenceName = :name")
    Optional<ReferenceSequence> findByNameForUpdate(@Param("name") String name);
}
