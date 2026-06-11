package com.questline.ai;

import java.util.List;

public record PlannedMilestone(String title, String description, List<PlannedTask> tasks) {
}
