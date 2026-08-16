package com.charitymanagement.api.charitymanagementback.donation.mapper;

import com.charitymanagement.api.charitymanagementback.common.config.CharityProperties;
import com.charitymanagement.api.charitymanagementback.common.dto.UserSummary;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationItemResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationReceiptResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonationResponse;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonorResponse;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donation;
import com.charitymanagement.api.charitymanagementback.donation.entity.DonationItem;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donor;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DonationMapper {

    private final CharityProperties properties;

    public DonorResponse toResponse(Donor donor) {
        return DonorResponse.builder()
                .id(donor.getId())
                .donorCode(donor.getDonorCode())
                .donorName(donor.getDonorName())
                .donorType(donor.getDonorType())
                .phone(donor.getPhone())
                .email(donor.getEmail())
                .address(donor.getAddress())
                .notes(donor.getNotes())
                .status(donor.getStatus())
                .createdAt(donor.getCreatedAt())
                .updatedAt(donor.getUpdatedAt())
                .build();
    }

    public DonationItemResponse toResponse(DonationItem item) {
        InventoryItem inventoryItem = item.getInventoryItem();
        return DonationItemResponse.builder()
                .id(item.getId())
                .inventoryItemId(inventoryItem.getId())
                .itemCode(inventoryItem.getItemCode())
                .itemName(inventoryItem.getItemName())
                .categoryName(inventoryItem.getCategory() != null ? inventoryItem.getCategory().getName() : null)
                .quantity(item.getQuantity())
                .unit(inventoryItem.getUnit())
                .expiryDate(item.getExpiryDate())
                .notes(item.getNotes())
                .build();
    }

    public DonationResponse toResponse(Donation donation, List<DonationItem> items) {
        List<DonationItemResponse> itemResponses = items.stream().map(this::toResponse).toList();
        Donor donor = donation.getDonor();
        return DonationResponse.builder()
                .id(donation.getId())
                .donationReference(donation.getDonationReference())
                .donorId(donor.getId())
                .donorCode(donor.getDonorCode())
                .donorName(donor.getDonorName())
                .donationDate(donation.getDonationDate())
                .notes(donation.getNotes())
                .receivedBy(UserSummary.from(donation.getReceivedBy()))
                .status(donation.getStatus())
                .totalItems(itemResponses.size())
                .totalQuantity(itemResponses.stream().mapToLong(DonationItemResponse::getQuantity).sum())
                .items(itemResponses)
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt())
                .build();
    }

    public DonationReceiptResponse toReceipt(Donation donation, List<DonationItem> items) {
        List<DonationItemResponse> itemResponses = items.stream().map(this::toResponse).toList();
        Donor donor = donation.getDonor();
        return DonationReceiptResponse.builder()
                .organizationName(properties.getOrganization().getName())
                .organizationAddress(properties.getOrganization().getAddress())
                .organizationContact(properties.getOrganization().getContact())
                .donationReference(donation.getDonationReference())
                .donationDate(donation.getDonationDate())
                .donorCode(donor.getDonorCode())
                .donorName(donor.getDonorName())
                .donorType(donor.getDonorType() != null ? donor.getDonorType().name() : null)
                .donorPhone(donor.getPhone())
                .donorEmail(donor.getEmail())
                .donorAddress(donor.getAddress())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .totalQuantity(itemResponses.stream().mapToLong(DonationItemResponse::getQuantity).sum())
                .receivedByName(donation.getReceivedBy() != null ? donation.getReceivedBy().getName() : null)
                .notes(donation.getNotes())
                .status(donation.getStatus().name())
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
