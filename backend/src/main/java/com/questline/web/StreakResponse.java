package com.questline.web;

import com.questline.domain.Streak;
import java.time.LocalDate;

public record StreakResponse(int current, int longest, LocalDate lastActiveDate,
                             int freezesAvailable) {

    public static StreakResponse from(Streak streak) {
        return new StreakResponse(streak.getCurrent(), streak.getLongest(),
                streak.getLastActiveDate(), streak.getFreezesAvailable());
    }
}
