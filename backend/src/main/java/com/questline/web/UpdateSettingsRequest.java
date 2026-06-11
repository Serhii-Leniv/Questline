package com.questline.web;

import jakarta.validation.constraints.Positive;

/**
 * Partial update of user settings; null fields are left unchanged. {@code timezone} must be a
 * valid IANA zone — validated in the service since it drives "today"/streak computation.
 */
public record UpdateSettingsRequest(
        String timezone,
        @Positive Integer dailyCapacityMinutes,
        @Positive Integer dailyTaskGoal
) {
}
