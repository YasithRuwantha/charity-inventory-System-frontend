package com.charitymanagement.api.charitymanagementback.volunteer.service;

import com.charitymanagement.api.charitymanagementback.audit.entity.AuditAction;
import com.charitymanagement.api.charitymanagementback.audit.service.AuditService;
import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.auth.entity.UserRole;
import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import com.charitymanagement.api.charitymanagementback.common.exception.DuplicateResourceException;
import com.charitymanagement.api.charitymanagementback.common.exception.ForbiddenException;
import com.charitymanagement.api.charitymanagementback.common.exception.ResourceNotFoundException;
import com.charitymanagement.api.charitymanagementback.common.reference.ReferenceGenerator;
import com.charitymanagement.api.charitymanagementback.common.security.CurrentUserProvider;
import com.charitymanagement.api.charitymanagementback.common.util.PageableUtils;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.CreateVolunteerRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.request.UpdateVolunteerRequest;
import com.charitymanagement.api.charitymanagementback.volunteer.dto.response.VolunteerResponse;
import com.charitymanagement.api.charitymanagementback.volunteer.entity.Volunteer;
import com.charitymanagement.api.charitymanagementback.volunteer.mapper.VolunteerMapper;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerRepository;
import com.charitymanagement.api.charitymanagementback.volunteer.repository.VolunteerSpecifications;
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
public class VolunteerService {

    private static final Logger log = LoggerFactory.getLogger(VolunteerService.class);

    private static final Set<String> SORTABLE = Set.of(
            "id", "volunteerCode", "volunteerName", "status", "joinedDate", "createdAt", "updatedAt");

    private final VolunteerRepository volunteerRepository;
    private final VolunteerMapper mapper;
    private final ReferenceGenerator referenceGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional
    public VolunteerResponse create(CreateVolunteerRequest request) {
        String email = trimToNull(request.getEmail());
        if (email != null && volunteerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A volunteer with email '" + email + "' already exists");
        }
        Volunteer volunteer = volunteerRepository.save(Volunteer.builder()
                .volunteerCode(referenceGenerator.next(ReferenceGenerator.VOLUNTEER))
                .volunteerName(request.getVolunteerName().trim())
                .phone(trimToNull(request.getPhone()))
                .email(email)
                .address(trimToNull(request.getAddress()))
                .joinedDate(request.getJoinedDate())
                .notes(trimToNull(request.getNotes()))
                .status(request.getStatus() != null ? request.getStatus() : RecordStatus.ACTIVE)
                .build());

        auditService.record(AuditAction.CREATE_VOLUNTEER, "Volunteer", volunteer.getId(),
                "Registered volunteer " + volunteer.getVolunteerCode() + " ("
                        + volunteer.getVolunteerName() + ")",
                null, snapshot(volunteer));
        log.info("Registered volunteer {}", volunteer.getVolunteerCode());
        return mapper.toResponse(volunteer);
    }

    @Transactional
    public VolunteerResponse update(Long id, UpdateVolunteerRequest request) {
        Volunteer volunteer = requireVolunteer(id);
        Map<String, Object> before = snapshot(volunteer);

        String email = trimToNull(request.getEmail());
        if (email != null && volunteerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("A volunteer with email '" + email + "' already exists");
        }
        volunteer.setVolunteerName(request.getVolunteerName().trim());
        volunteer.setPhone(trimToNull(request.getPhone()));
        volunteer.setEmail(email);
        volunteer.setAddress(trimToNull(request.getAddress()));
        volunteer.setJoinedDate(request.getJoinedDate());
        volunteer.setNotes(trimToNull(request.getNotes()));
        volunteerRepository.save(volunteer);

        auditService.record(AuditAction.UPDATE_VOLUNTEER, "Volunteer", id,
                "Updated volunteer " + volunteer.getVolunteerCode(), before, snapshot(volunteer));
        return mapper.toResponse(volunteer);
    }

    @Transactional
    public VolunteerResponse changeStatus(Long id, RecordStatus status) {
        Volunteer volunteer = requireVolunteer(id);
        Map<String, Object> before = snapshot(volunteer);
        volunteer.setStatus(status);
        volunteerRepository.save(volunteer);

        auditService.record(AuditAction.CHANGE_VOLUNTEER_STATUS, "Volunteer", id,
                "Volunteer " + volunteer.getVolunteerCode() + " set to " + status,
                before, snapshot(volunteer));
        return mapper.toResponse(volunteer);
    }

    @Transactional(readOnly = true)
    public VolunteerResponse get(Long id) {
        return mapper.toResponse(requireVolunteer(id));
    }

    @Transactional(readOnly = true)
    public Page<VolunteerResponse> search(String search, RecordStatus status, Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, SORTABLE, "createdAt");
        return volunteerRepository.findAll(VolunteerSpecifications.volunteers(search, status), safe)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Volunteer requireVolunteer(Long id) {
        return volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer", id));
    }

    /**
     * A VOLUNTEER-role user may only reach the record whose email matches their own account.
     * Staff and admins are unrestricted. This is what keeps {@code /api/volunteers/{id}/tasks}
     * from becoming an insecure direct object reference.
     */
    @Transactional(readOnly = true)
    public void assertCanAccess(Volunteer volunteer) {
        User actor = currentUserProvider.requirePrincipal();
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.INVENTORY_STAFF) {
            return;
        }
        String volunteerEmail = volunteer.getEmail();
        if (volunteerEmail == null || !volunteerEmail.equalsIgnoreCase(actor.getEmail())) {
            throw new ForbiddenException("You may only access your own volunteer record");
        }
    }

    private static Map<String, Object> snapshot(Volunteer volunteer) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("volunteerCode", volunteer.getVolunteerCode());
        values.put("volunteerName", volunteer.getVolunteerName());
        values.put("phone", volunteer.getPhone());
        values.put("email", volunteer.getEmail());
        values.put("joinedDate", volunteer.getJoinedDate());
        values.put("status", volunteer.getStatus());
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
