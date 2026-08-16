package com.charitymanagement.api.charitymanagementback.donation.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.DuplicateResourceException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.reference.ReferenceGenerator;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.CreateDonorRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.request.UpdateDonorRequest;
import com.charitymanagement.api.charitymanagementback.donation.dto.response.DonorResponse;
import com.charitymanagement.api.charitymanagementback.donation.entity.Donor;
import com.charitymanagement.api.charitymanagementback.donation.enums.DonorType;
import com.charitymanagement.api.charitymanagementback.donation.mapper.DonationMapper;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonationSpecifications;
import com.charitymanagement.api.charitymanagementback.donation.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DonorService {

    private static final Set<String> SORTABLE =
            Set.of("id", "donorCode", "donorName", "donorType", "status", "createdAt", "updatedAt");

    private final DonorRepository donorRepository;
    private final DonationMapper mapper;
    private final ReferenceGenerator referenceGenerator;
    private final AuditService auditService;

    @Transactional
    public DonorResponse create(CreateDonorRequest request) {
        String donorName = request.getDonorName().trim();
        // Anonymous donors are intentionally exempt: several may share the same placeholder name.
        if (request.getDonorType() != DonorType.ANONYMOUS
                && donorRepository.existsByDonorNameIgnoreCase(donorName)) {
            throw new DuplicateResourceException("A donor named '" + donorName + "' is already registered");
        }
        Donor donor = donorRepository.save(Donor.builder()
                .donorCode(referenceGenerator.next(ReferenceGenerator.DONOR))
                .donorName(donorName)
                .donorType(request.getDonorType())
                .phone(trimToNull(request.getPhone()))
                .email(trimToNull(request.getEmail()))
                .address(trimToNull(request.getAddress()))
                .notes(trimToNull(request.getNotes()))
                .status(request.getStatus() != null ? request.getStatus() : RecordStatus.ACTIVE)
                .build());

        auditService.record(AuditAction.CREATE_DONOR, "Donor", donor.getId(),
                "Registered donor " + donor.getDonorCode() + " (" + donor.getDonorName() + ")",
                null, snapshot(donor));
        return mapper.toResponse(donor);
    }

    @Transactional
    public DonorResponse update(Long id, UpdateDonorRequest request) {
        Donor donor = requireDonor(id);
        Map<String, Object> before = snapshot(donor);

        String donorName = request.getDonorName().trim();
        if (request.getDonorType() != DonorType.ANONYMOUS
                && donorRepository.existsByDonorNameIgnoreCaseAndIdNot(donorName, id)) {
            throw new DuplicateResourceException("A donor named '" + donorName + "' is already registered");
        }
        donor.setDonorName(donorName);
        donor.setDonorType(request.getDonorType());
        donor.setPhone(trimToNull(request.getPhone()));
        donor.setEmail(trimToNull(request.getEmail()));
        donor.setAddress(trimToNull(request.getAddress()));
        donor.setNotes(trimToNull(request.getNotes()));
        donorRepository.save(donor);

        auditService.record(AuditAction.UPDATE_DONOR, "Donor", id,
                "Updated donor " + donor.getDonorCode(), before, snapshot(donor));
        return mapper.toResponse(donor);
    }

    @Transactional
    public DonorResponse changeStatus(Long id, RecordStatus status) {
        Donor donor = requireDonor(id);
        Map<String, Object> before = snapshot(donor);
        donor.setStatus(status);
        donorRepository.save(donor);

        auditService.record(AuditAction.CHANGE_DONOR_STATUS, "Donor", id,
                "Donor " + donor.getDonorCode() + " set to " + status, before, snapshot(donor));
        return mapper.toResponse(donor);
    }

    @Transactional(readOnly = true)
    public DonorResponse get(Long id) {
        return mapper.toResponse(requireDonor(id));
    }

    @Transactional(readOnly = true)
    public Page<DonorResponse> search(String search, DonorType donorType, RecordStatus status, Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "createdAt");
        return donorRepository.findAll(DonationSpecifications.donors(search, donorType, status), safe)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Donor requireDonor(Long id) {
        return donorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Donor", id));
    }

    /** Used by the bulk import, which identifies donors by name or donor code. */
    @Transactional(readOnly = true)
    public Donor findByNameOrCode(String value) {
        String trimmed = value.trim();
        return donorRepository.findByDonorCodeIgnoreCase(trimmed)
                .or(() -> donorRepository.findByDonorNameIgnoreCase(trimmed))
                .orElse(null);
    }

    private static Map<String, Object> snapshot(Donor donor) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("donorCode", donor.getDonorCode());
        values.put("donorName", donor.getDonorName());
        values.put("donorType", donor.getDonorType());
        values.put("phone", donor.getPhone());
        values.put("email", donor.getEmail());
        values.put("status", donor.getStatus());
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
