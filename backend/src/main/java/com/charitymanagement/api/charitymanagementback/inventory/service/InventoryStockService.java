package com.charitymanagement.api.charitymanagementback.inventory.service;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.common.exception.ExpiredInventoryException;
import com.charitymanagement.api.charitymanagementback.common.exception.InsufficientStockException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryTransaction;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The only place in the system that changes {@code inventory_items.quantity}.
 *
 * <p>Every movement takes a pessimistic row lock, validates against the freshly-read quantity, and
 * writes an {@link InventoryTransaction} in the same transaction — so stock can never go negative
 * under concurrency and the ledger can never drift from the balance.
 *
 * <p>Declared {@code MANDATORY}: callers must already own a transaction, which is what makes a
 * multi-item donation or distribution roll back as one unit.
 */
@Service
@RequiredArgsConstructor
public class InventoryStockService {

    private static final Logger log = LoggerFactory.getLogger(InventoryStockService.class);

    private final InventoryItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;

    /** Loads an item under a write lock. Concurrent callers queue here rather than racing. */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryItem lockItem(Long itemId) {
        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", itemId));
    }

    /**
     * Adds stock (donations, positive corrections).
     *
     * @param batchExpiry expiry of the arriving batch, or null when not tracked
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryTransaction increase(Long itemId,
                                         int quantity,
                                         TransactionType transactionType,
                                         ReferenceType referenceType,
                                         Long referenceId,
                                         String notes,
                                         User performedBy,
                                         LocalDate batchExpiry) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity to add must be greater than zero");
        }
        InventoryItem item = lockItem(itemId);
        assertNotArchived(item);

        applyBatchExpiry(item, batchExpiry);
        return applyMovement(item, quantity, transactionType, referenceType, referenceId, notes, performedBy);
    }

    /**
     * Removes stock (completed distributions, negative corrections).
     *
     * @param rejectExpired when true an expired item is refused outright — used by distributions
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryTransaction decrease(Long itemId,
                                         int quantity,
                                         TransactionType transactionType,
                                         ReferenceType referenceType,
                                         Long referenceId,
                                         String notes,
                                         User performedBy,
                                         boolean rejectExpired) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity to remove must be greater than zero");
        }
        InventoryItem item = lockItem(itemId);
        assertNotArchived(item);

        if (rejectExpired && item.isExpired()) {
            throw new ExpiredInventoryException("Item " + item.getItemName() + " (" + item.getItemCode()
                    + ") expired on " + item.getExpiryDate() + " and cannot be distributed");
        }
        int available = item.getQuantity() == null ? 0 : item.getQuantity();
        if (available < quantity) {
            throw InsufficientStockException.forItem(item.getItemCode(), item.getItemName(), available, quantity);
        }
        return applyMovement(item, -quantity, transactionType, referenceType, referenceId, notes, performedBy);
    }

    /**
     * Signed manual adjustment used by {@code POST /api/inventory/{id}/adjust-stock}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryTransaction adjust(Long itemId,
                                       int signedAdjustment,
                                       TransactionType transactionType,
                                       String notes,
                                       User performedBy) {
        if (signedAdjustment == 0) {
            throw new BadRequestException("Adjustment must be a non-zero quantity");
        }
        if (signedAdjustment > 0) {
            return increase(itemId, signedAdjustment, transactionType, ReferenceType.MANUAL, null,
                    notes, performedBy, null);
        }
        return decrease(itemId, -signedAdjustment, transactionType, ReferenceType.MANUAL, null,
                notes, performedBy, false);
    }

    private InventoryTransaction applyMovement(InventoryItem item,
                                               int signedQuantity,
                                               TransactionType transactionType,
                                               ReferenceType referenceType,
                                               Long referenceId,
                                               String notes,
                                               User performedBy) {
        int quantityBefore = item.getQuantity() == null ? 0 : item.getQuantity();
        int quantityAfter = quantityBefore + signedQuantity;

        // Belt and braces: the caller-specific checks above should already guarantee this.
        if (quantityAfter < 0) {
            throw InsufficientStockException.forItem(item.getItemCode(), item.getItemName(),
                    quantityBefore, Math.abs(signedQuantity));
        }

        item.setQuantity(quantityAfter);
        item.recalculateStatus();
        itemRepository.save(item);

        InventoryTransaction transaction = transactionRepository.save(InventoryTransaction.builder()
                .inventoryItem(item)
                .transactionType(transactionType)
                .quantity(signedQuantity)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .performedBy(performedBy)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Stock movement {} on item {} ({}): {} -> {} [{} #{}]", transactionType, item.getItemCode(),
                item.getItemName(), quantityBefore, quantityAfter, referenceType, referenceId);
        return transaction;
    }

    /**
     * Keeps the item's single expiry date honest as new batches arrive. Without per-batch tracking
     * the conservative choice is the earliest date, except when the item is already past its date —
     * then the incoming batch replaces it, otherwise restocked goods would stay EXPIRED forever.
     */
    private void applyBatchExpiry(InventoryItem item, LocalDate batchExpiry) {
        if (batchExpiry == null) {
            return;
        }
        LocalDate current = item.getExpiryDate();
        if (current == null || current.isBefore(LocalDate.now()) || batchExpiry.isBefore(current)) {
            item.setExpiryDate(batchExpiry);
        }
    }

    private void assertNotArchived(InventoryItem item) {
        if (item.isArchived()) {
            throw new BadRequestException("Inventory item " + item.getItemCode()
                    + " is archived and cannot be used in stock movements", "ITEM_ARCHIVED");
        }
    }
}
