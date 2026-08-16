package com.charitymanagement.api.charitymanagementback.inventory.dto.request;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 120, message = "Category name must not exceed 120 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private RecordStatus status;
}
