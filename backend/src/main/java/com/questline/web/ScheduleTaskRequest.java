package com.questline.web;

import java.time.LocalDate;

/** A null {@code scheduledFor} unschedules the task (removes it from any day). */
public record ScheduleTaskRequest(LocalDate scheduledFor) {
}
