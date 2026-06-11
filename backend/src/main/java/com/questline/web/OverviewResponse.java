package com.questline.web;

/** Dashboard summary: total XP, derived level, current/longest streak, and freezes in hand. */
public record OverviewResponse(long xpTotal, int level, int currentStreak, int longestStreak,
                               int freezesAvailable) {
}
