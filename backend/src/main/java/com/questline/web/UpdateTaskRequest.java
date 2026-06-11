package com.questline.web;

import com.questline.domain.ResourceLink;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Partial update of a task's editable content. Status and schedule have their own endpoints;
 * null fields are left unchanged.
 */
public record UpdateTaskRequest(
        String title,
        String description,
        @Positive Integer estimateMinutes,
        String notes,
        List<ResourceLink> resources
) {
}
