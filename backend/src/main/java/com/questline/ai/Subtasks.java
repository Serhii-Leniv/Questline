package com.questline.ai;

import java.util.List;

/** The model's decomposition of one task into smaller subtasks (Flow C). */
public record Subtasks(List<PlannedTask> subtasks) {
}
