package com.questline.web;

import com.questline.domain.GoalStatus;
import jakarta.validation.constraints.Future;
import java.time.LocalDate;

/** Partial update — every field is optional; null means "leave unchanged". */
public record UpdateGoalRequest(
        String title,
        String description,
        String context,
        String target,
        @Future LocalDate targetDate,
        GoalStatus status
) {
}
