package com.questline.web;

import java.util.List;

/** A full export of a user's data (GDPR), assembled from the existing read views. */
public record ExportResponse(
        MeResponse profile,
        List<GoalTreeResponse> goals,
        StreakResponse streak,
        List<AchievementResponse> achievements,
        List<TopicProgressResponse> topics,
        List<HeatmapEntry> activity
) {
}
