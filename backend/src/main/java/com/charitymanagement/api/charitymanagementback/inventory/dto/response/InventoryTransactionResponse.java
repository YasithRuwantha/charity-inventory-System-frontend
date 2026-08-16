package com.charitymanagement.api.charitymanagementback.inventory.dto.response;

import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryTransactionResponse {

    private Long id;
    private Long inventoryItemId;
    private String itemCode;
    private String itemName;
    private TransactionType transactionType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private ReferenceType referenceType;
    private Long referenceId;
    private String notes;
    private UserSummary performedBy;
    private LocalDateTime createdAt;
}
