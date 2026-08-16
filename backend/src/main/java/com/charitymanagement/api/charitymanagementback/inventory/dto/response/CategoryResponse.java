package com.charitymanagement.api.charitymanagementback.inventory.dto.response;

import com.charitymanagement.api.charitymanagementback.common.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private RecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
