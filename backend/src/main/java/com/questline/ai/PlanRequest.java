package com.questline.ai;

import java.time.LocalDate;

/**
 * Our own input type for plan generation — free of any Spring AI / provider types. Built by the
 * service layer from the user's goal input.
 */
public record PlanRequest(
        String context,
        String target,
        LocalDate targetDate,
        Integer weeklyCapacityMinutes
) {
}
