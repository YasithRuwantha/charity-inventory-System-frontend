package com.charitymanagement.api.charitymanagementback.donation.repository;

import com.charitymanagement.api.charitymanagementback.donation.entity.DonationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DonationItemRepository extends JpaRepository<DonationItem, Long> {

    @Query("""
            select i from DonationItem i
            join fetch i.inventoryItem
            where i.donation.id = :donationId
            order by i.id asc
            """)
    List<DonationItem> findByDonationId(@Param("donationId") Long donationId);

    boolean existsByInventoryItemId(Long inventoryItemId);
}
