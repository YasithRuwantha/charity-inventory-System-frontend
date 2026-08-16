package com.charitymanagement.api.charitymanagementback.inventory.service;

import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryTransactionResponse;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryTransaction;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.inventory.mapper.InventoryMapper;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryTransactionRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryTransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryTransactionQueryService {

    private static final Set<String> SORTABLE = Set.of("id", "createdAt", "quantity", "transactionType");

    private final InventoryTransactionRepository transactionRepository;
    private final InventoryItemService itemService;
    private final InventoryMapper mapper;

    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> search(Long itemId,
                                                     TransactionType transactionType,
                                                     ReferenceType referenceType,
                                                     Long performedBy,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "createdAt");
        Specification<InventoryTransaction> specification = InventoryTransactionSpecifications.build(
                itemId, transactionType, referenceType, performedBy, startDate, endDate);
        return transactionRepository.findAll(specification, safe).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> findForItem(Long itemId, Pageable pageable) {
        itemService.requireItem(itemId);
        return search(itemId, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionResponse> findForReference(ReferenceType referenceType, Long referenceId) {
        return transactionRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc(referenceType, referenceId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
