package com.questline.web;

import com.questline.domain.ActivityDay;
import java.time.LocalDate;

/** One cell of the activity heatmap. {@code count} is the number of tasks completed that day. */
public record HeatmapEntry(LocalDate date, int count) {

    public static HeatmapEntry from(ActivityDay day) {
        return new HeatmapEntry(day.getDate(), day.getTasksCompleted());
    }
}
