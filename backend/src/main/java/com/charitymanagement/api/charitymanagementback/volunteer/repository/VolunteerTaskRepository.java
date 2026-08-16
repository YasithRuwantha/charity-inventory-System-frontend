package com.charitymanagement.api.charitymanagementback.volunteer.repository;

import com.charitymanagement.api.charitymanagementback.volunteer.entity.VolunteerTask;
import com.charitymanagement.api.charitymanagementback.volunteer.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VolunteerTaskRepository extends JpaRepository<VolunteerTask, Long>,
        JpaSpecificationExecutor<VolunteerTask> {

    List<VolunteerTask> findByVolunteerIdOrderByTaskDateDesc(Long volunteerId);

    long countByStatus(TaskStatus status);

    long countByVolunteerIdAndStatus(Long volunteerId, TaskStatus status);

    long countByStatusAndDueDateBefore(TaskStatus status, LocalDate date);

    @Query("select t.status, count(t) from VolunteerTask t "
            + "where (:volunteerId is null or t.volunteer.id = :volunteerId) "
            + "and (:startDate is null or t.taskDate >= :startDate) "
            + "and (:endDate is null or t.taskDate <= :endDate) "
            + "group by t.status")
    List<Object[]> aggregateByStatus(@Param("volunteerId") Long volunteerId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("""
            select t.volunteer.id, t.volunteer.volunteerCode, t.volunteer.volunteerName, t.status, count(t)
            from VolunteerTask t
            where (:volunteerId is null or t.volunteer.id = :volunteerId)
              and (:startDate is null or t.taskDate >= :startDate)
              and (:endDate is null or t.taskDate <= :endDate)
            group by t.volunteer.id, t.volunteer.volunteerCode, t.volunteer.volunteerName, t.status
            order by t.volunteer.volunteerCode
            """)
    List<Object[]> aggregateByVolunteerAndStatus(@Param("volunteerId") Long volunteerId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}
