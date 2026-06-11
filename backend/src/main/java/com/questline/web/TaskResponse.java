package com.questline.web;

import com.questline.domain.ResourceLink;
import com.questline.domain.Task;
import com.questline.domain.TaskStatus;
import com.questline.domain.Topic;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID goalId,
        UUID milestoneId,
        UUID parentTaskId,
        String title,
        String description,
        Integer estimateMinutes,
        int orderIndex,
        TaskStatus status,
        LocalDate scheduledFor,
        Instant completedAt,
        List<ResourceLink> resources,
        String notes,
        List<String> topics,
        Instant createdAt,
        Instant updatedAt
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getGoal().getId(),
                task.getMilestone() == null ? null : task.getMilestone().getId(),
                task.getParentTask() == null ? null : task.getParentTask().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getEstimateMinutes(),
                task.getOrderIndex(),
                task.getStatus(),
                task.getScheduledFor(),
                task.getCompletedAt(),
                task.getResources(),
                task.getNotes(),
                task.getTopics().stream().map(Topic::getName).toList(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
