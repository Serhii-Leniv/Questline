package com.questline.web;

import com.questline.domain.GoalTemplate;
import java.time.Instant;
import java.util.UUID;

/** Lightweight template entry for the browse list. */
public record TemplateSummaryResponse(UUID id, String title, String summary, int taskCount,
                                      Instant createdAt) {

    public static TemplateSummaryResponse from(GoalTemplate t) {
        return new TemplateSummaryResponse(t.getId(), t.getTitle(), t.getSummary(),
                t.getTaskCount(), t.getCreatedAt());
    }
}
