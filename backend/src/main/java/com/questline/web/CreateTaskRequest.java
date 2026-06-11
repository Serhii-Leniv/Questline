package com.questline.web;

import com.questline.domain.ResourceLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Manual task creation. {@code goalId} is required; {@code milestoneId} is optional. */
public record CreateTaskRequest(
        @NotNull UUID goalId,
        UUID milestoneId,
        @NotBlank String title,
        String description,
        @Positive Integer estimateMinutes,
        LocalDate scheduledFor,
        String notes,
        List<ResourceLink> resources
) {
}
