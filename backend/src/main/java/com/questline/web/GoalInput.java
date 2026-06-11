package com.questline.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** Input for AI plan generation (Flow A). */
public record GoalInput(
        @NotBlank String context,
        @NotBlank String target,
        @Future LocalDate targetDate,
        @Positive Integer weeklyCapacityMinutes
) {
}
