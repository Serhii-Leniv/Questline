package com.questline.web;

import com.questline.domain.User;

/** The user's current plan and the AI request limit it grants. */
public record PlanResponse(String plan, int aiDailyLimit) {

    public static PlanResponse of(User user, int aiDailyLimit) {
        return new PlanResponse(user.getPlan().name(), aiDailyLimit);
    }
}
