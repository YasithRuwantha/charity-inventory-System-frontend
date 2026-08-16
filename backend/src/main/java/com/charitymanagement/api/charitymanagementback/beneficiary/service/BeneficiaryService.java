package com.charitymanagement.api.charitymanagementback.beneficiary.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.request.CreateBeneficiaryRequest;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.request.UpdateBeneficiaryRequest;
import com.charitymanagement.api.charitymanagementback.beneficiary.dto.response.BeneficiaryResponse;
import com.charitymanagement.api.charitymanagementback.beneficiary.entity.Beneficiary;
import com.charitymanagement.api.charitymanagementback.beneficiary.enums.PriorityLevel;
import com.charitymanagement.api.charitymanagementback.beneficiary.mapper.BeneficiaryMapper;
import com.charitymanagement.api.charitymanagementback.beneficiary.repository.BeneficiaryRepository;
import com.charitymanagement.api.charitymanagementback.beneficiary.repository.BeneficiarySpecifications;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.DuplicateResourceException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.reference.ReferenceGenerator;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryService.class);

    private static final Set<String> SORTABLE = Set.of(
            "id", "beneficiaryCode", "beneficiaryName", "familySize", "priorityLevel", "status",
            "createdAt", "updatedAt");

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryMapper mapper;
    private final ReferenceGenerator referenceGenerator;
    private final AuditService auditService;

    @Transactional
    public BeneficiaryResponse create(CreateBeneficiaryRequest request) {
        String identification = trimToNull(request.getIdentificationNumber());
        assertIdentificationAvailable(identification, null);

        Beneficiary beneficiary = beneficiaryRepository.save(Beneficiary.builder()
                .beneficiaryCode(referenceGenerator.next(ReferenceGenerator.BENEFICIARY))
                .beneficiaryName(request.getBeneficiaryName().trim())
                .identificationNumber(identification)
                .familySize(request.getFamilySize())
                .contactNumber(trimToNull(request.getContactNumber()))
                .address(trimToNull(request.getAddress()))
                .priorityLevel(request.getPriorityLevel())
                .status(request.getStatus() != null ? request.getStatus() : RecordStatus.ACTIVE)
                .notes(trimToNull(request.getNotes()))
                .build());

        auditService.record(AuditAction.CREATE_BENEFICIARY, "Beneficiary", beneficiary.getId(),
                "Registered beneficiary " + beneficiary.getBeneficiaryCode()
                        + " (" + beneficiary.getBeneficiaryName() + ")",
                null, snapshot(beneficiary));
        log.info("Registered beneficiary {} with priority {}", beneficiary.getBeneficiaryCode(),
                beneficiary.getPriorityLevel());
        return mapper.toResponse(beneficiary);
    }

    @Transactional
    public BeneficiaryResponse update(Long id, UpdateBeneficiaryRequest request) {
        Beneficiary beneficiary = requireBeneficiary(id);
        Map<String, Object> before = snapshot(beneficiary);

        String identification = trimToNull(request.getIdentificationNumber());
        assertIdentificationAvailable(identification, id);

        beneficiary.setBeneficiaryName(request.getBeneficiaryName().trim());
        beneficiary.setIdentificationNumber(identification);
        beneficiary.setFamilySize(request.getFamilySize());
        beneficiary.setContactNumber(trimToNull(request.getContactNumber()));
        beneficiary.setAddress(trimToNull(request.getAddress()));
        beneficiary.setPriorityLevel(request.getPriorityLevel());
        beneficiary.setNotes(trimToNull(request.getNotes()));
        beneficiaryRepository.save(beneficiary);

        auditService.record(AuditAction.UPDATE_BENEFICIARY, "Beneficiary", id,
                "Updated beneficiary " + beneficiary.getBeneficiaryCode(), before, snapshot(beneficiary));
        return mapper.toResponse(beneficiary);
    }

    @Transactional
    public BeneficiaryResponse changeStatus(Long id, RecordStatus status) {
        Beneficiary beneficiary = requireBeneficiary(id);
        Map<String, Object> before = snapshot(beneficiary);
        beneficiary.setStatus(status);
        beneficiaryRepository.save(beneficiary);

        auditService.record(AuditAction.CHANGE_BENEFICIARY_STATUS, "Beneficiary", id,
                "Beneficiary " + beneficiary.getBeneficiaryCode() + " set to " + status,
                before, snapshot(beneficiary));
        return mapper.toResponse(beneficiary);
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse get(Long id) {
        return mapper.toResponse(requireBeneficiary(id));
    }

    @Transactional(readOnly = true)
    public Page<BeneficiaryResponse> search(String search, PriorityLevel priority, RecordStatus status,
                                            Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "createdAt");
        return beneficiaryRepository.findAll(BeneficiarySpecifications.build(search, priority, status), safe)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Beneficiary requireBeneficiary(Long id) {
        return beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));
    }

    /**
     * Identification numbers are optional, so only a supplied value is checked. The database
     * unique index tolerates repeated NULLs, which is what makes "optional but unique" work.
     */
    private void assertIdentificationAvailable(String identificationNumber, Long excludedId) {
        if (identificationNumber == null) {
            return;
        }
        boolean taken = excludedId == null
                ? beneficiaryRepository.existsByIdentificationNumberIgnoreCase(identificationNumber)
                : beneficiaryRepository.existsByIdentificationNumberIgnoreCaseAndIdNot(identificationNumber,
                        excludedId);
        if (taken) {
            throw new DuplicateResourceException(
                    "A beneficiary with identification number '" + identificationNumber + "' already exists");
        }
    }

    private static Map<String, Object> snapshot(Beneficiary beneficiary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("beneficiaryCode", beneficiary.getBeneficiaryCode());
        values.put("beneficiaryName", beneficiary.getBeneficiaryName());
        values.put("identificationNumber", beneficiary.getIdentificationNumber());
        values.put("familySize", beneficiary.getFamilySize());
        values.put("contactNumber", beneficiary.getContactNumber());
        values.put("priorityLevel", beneficiary.getPriorityLevel());
        values.put("status", beneficiary.getStatus());
        return values;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
