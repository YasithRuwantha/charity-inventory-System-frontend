package com.charitymanagement.api.charitymanagementback.donation.repository;

import com.charitymanagement.api.charitymanagementback.donation.entity.Donation;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {

    Optional<Donation> findByDonationReferenceIgnoreCase(String donationReference);

    Optional<Donation> findByIdempotencyKey(String idempotencyKey);

    long countByStatus(DonationStatus status);

    long countByDonationDateBetween(LocalDate start, LocalDate end);

    @Query("select count(d) from Donation d where d.donor.id = :donorId and d.status = :status")
    long countByDonor(@Param("donorId") Long donorId, @Param("status") DonationStatus status);

    @Query("select max(d.donationDate) from Donation d where d.donor.id = :donorId and d.status = :status")
    LocalDate findLastDonationDate(@Param("donorId") Long donorId, @Param("status") DonationStatus status);

    @Query("""
            select coalesce(sum(i.quantity), 0) from DonationItem i
            where i.donation.donor.id = :donorId and i.donation.status = :status
            """)
    long sumQuantityByDonor(@Param("donorId") Long donorId, @Param("status") DonationStatus status);

    @Query("""
            select count(distinct i.id) from DonationItem i
            where i.donation.donor.id = :donorId and i.donation.status = :status
            """)
    long countItemsByDonor(@Param("donorId") Long donorId, @Param("status") DonationStatus status);

    /** Monthly donation counts and quantities for the dashboard trend chart. */
    @Query("""
            select year(d.donationDate), month(d.donationDate), count(distinct d.id),
                   coalesce(sum(i.quantity), 0)
            from Donation d left join d.items i
            where d.donationDate >= :from and d.status = :status
            group by year(d.donationDate), month(d.donationDate)
            order by year(d.donationDate), month(d.donationDate)
            """)
    List<Object[]> aggregateMonthly(@Param("from") LocalDate from, @Param("status") DonationStatus status);

    @Query("""
            select d.donor.id, d.donor.donorCode, d.donor.donorName, count(distinct d.id),
                   coalesce(sum(i.quantity), 0)
            from Donation d left join d.items i
            where d.status = :status
              and (:startDate is null or d.donationDate >= :startDate)
              and (:endDate is null or d.donationDate <= :endDate)
              and (:donorId is null or d.donor.id = :donorId)
            group by d.donor.id, d.donor.donorCode, d.donor.donorName
            order by count(distinct d.id) desc
            """)
    List<Object[]> aggregateByDonor(@Param("status") DonationStatus status,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("donorId") Long donorId);

    @Query("""
            select count(distinct d.id), coalesce(sum(i.quantity), 0), count(i.id)
            from Donation d left join d.items i
            where d.status = :status
              and (:startDate is null or d.donationDate >= :startDate)
              and (:endDate is null or d.donationDate <= :endDate)
            """)
    List<Object[]> summarise(@Param("status") DonationStatus status,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate);
}
