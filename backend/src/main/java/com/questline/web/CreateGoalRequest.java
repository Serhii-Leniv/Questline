package com.questline.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Manual goal creation. AI generation is a separate, optional flow (SPEC §3.2). */
public record CreateGoalRequest(
        @NotBlank String title,
        String description,
        String context,
        String target,
        @Future LocalDate targetDate
) {
}
