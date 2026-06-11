package com.questline.web;

import com.questline.domain.GoalTemplate;
import java.util.Map;
import java.util.UUID;

/** Full template including its plan (raw JSON) for preview before importing. */
public record TemplateDetailResponse(UUID id, String title, String summary, int taskCount,
                                     Map<String, Object> plan) {

    public static TemplateDetailResponse from(GoalTemplate t) {
        return new TemplateDetailResponse(t.getId(), t.getTitle(), t.getSummary(),
                t.getTaskCount(), t.getPlan());
    }
}
