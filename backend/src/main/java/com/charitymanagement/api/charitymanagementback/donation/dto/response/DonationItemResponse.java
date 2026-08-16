package com.charitymanagement.api.charitymanagementback.donation.dto.response;

import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonationItemResponse {

    private Long id;
    private Long inventoryItemId;
    private String itemCode;
    private String itemName;
    private String categoryName;
    private Integer quantity;
    private UnitOfMeasure unit;
    private LocalDate expiryDate;
    private String notes;
}
