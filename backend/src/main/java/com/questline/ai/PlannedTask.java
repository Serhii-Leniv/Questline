package com.questline.ai;

import java.util.List;

public record PlannedTask(String title, String description, Integer estimateMinutes,
                          List<String> topics) {

    /** Convenience for callers/tests that don't supply topics. */
    public PlannedTask(String title, String description, Integer estimateMinutes) {
        this(title, description, estimateMinutes, null);
    }
}
