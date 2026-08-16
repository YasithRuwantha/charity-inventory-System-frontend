package com.charitymanagement.api.charitymanagementback.volunteer.dto.request;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CreateVolunteerRequest {

    @NotBlank(message = "Volunteer name is required")
    @Size(max = 200, message = "Volunteer name must not exceed 200 characters")
    private String volunteerName;

    @Size(max = 40, message = "Phone must not exceed 40 characters")
    private String phone;

    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @PastOrPresent(message = "Joined date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joinedDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private RecordStatus status;
}
