package com.questline.web;

import com.questline.domain.UserAchievement;
import java.time.Instant;

/** An unlocked achievement for the dashboard. */
public record AchievementResponse(
        String code,
        String title,
        String description,
        String icon,
        Instant unlockedAt
) {

    public static AchievementResponse from(UserAchievement unlock) {
        return new AchievementResponse(
                unlock.getAchievement().getCode(),
                unlock.getAchievement().getTitle(),
                unlock.getAchievement().getDescription(),
                unlock.getAchievement().getIcon(),
                unlock.getUnlockedAt());
    }
}
