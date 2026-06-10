package com.questline.web;

import com.questline.domain.User;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String name,
        String image,
        String timezone,
        int dailyCapacityMinutes,
        int dailyTaskGoal,
        long xpTotal
) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getImage(),
                user.getTimezone(),
                user.getDailyCapacityMinutes(),
                user.getDailyTaskGoal(),
                user.getXpTotal());
    }
}
