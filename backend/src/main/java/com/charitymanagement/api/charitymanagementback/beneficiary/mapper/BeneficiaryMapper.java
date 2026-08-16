package com.charitymanagement.api.charitymanagementback.beneficiary.mapper;

import com.charitymanagement.api.charitymanagementback.beneficiary.dto.response.BeneficiaryResponse;
import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryMapper {

    public BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .beneficiaryCode(beneficiary.getBeneficiaryCode())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .identificationNumber(beneficiary.getIdentificationNumber())
                .familySize(beneficiary.getFamilySize())
                .contactNumber(beneficiary.getContactNumber())
                .address(beneficiary.getAddress())
                .priorityLevel(beneficiary.getPriorityLevel())
                .status(beneficiary.getStatus())
                .notes(beneficiary.getNotes())
                .createdAt(beneficiary.getCreatedAt())
                .updatedAt(beneficiary.getUpdatedAt())
                .build();
    }
}
