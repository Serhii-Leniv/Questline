package com.questline.web;

/** Dashboard summary: total XP, derived level, and current/longest streak. */
public record OverviewResponse(long xpTotal, int level, int currentStreak, int longestStreak) {
}
