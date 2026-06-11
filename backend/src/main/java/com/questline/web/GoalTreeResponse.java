package com.questline.web;

import com.questline.domain.Goal;
import com.questline.domain.GoalSource;
import com.questline.domain.GoalStatus;
import com.questline.domain.Milestone;
import com.questline.domain.MilestoneStatus;
import com.questline.domain.Task;
import com.questline.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Full goal detail: the {@code Goal -> Milestone -> Task} tree, each level ordered by index. */
public record GoalTreeResponse(
        UUID id,
        String title,
        String description,
        String context,
        String target,
        GoalSource source,
        LocalDate targetDate,
        GoalStatus status,
        double progress,
        List<MilestoneNode> milestones
) {

    public static GoalTreeResponse from(Goal goal) {
        List<MilestoneNode> milestones = goal.getMilestones().stream()
                .sorted(Comparator.comparingInt(Milestone::getOrderIndex))
                .map(MilestoneNode::from)
                .toList();
        return new GoalTreeResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getContext(),
                goal.getTarget(),
                goal.getSource(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getProgress(),
                milestones);
    }

    public record MilestoneNode(
            UUID id,
            String title,
            String description,
            int orderIndex,
            MilestoneStatus status,
            double progress,
            LocalDate targetDate,
            List<TaskNode> tasks
    ) {

        static MilestoneNode from(Milestone milestone) {
            List<TaskNode> tasks = milestone.getTasks().stream()
                    .sorted(Comparator.comparingInt(Task::getOrderIndex))
                    .map(TaskNode::from)
                    .toList();
            return new MilestoneNode(
                    milestone.getId(),
                    milestone.getTitle(),
                    milestone.getDescription(),
                    milestone.getOrderIndex(),
                    milestone.getStatus(),
                    milestone.getProgress(),
                    milestone.getTargetDate(),
                    tasks);
        }
    }

    public record TaskNode(
            UUID id,
            String title,
            String description,
            Integer estimateMinutes,
            int orderIndex,
            TaskStatus status,
            LocalDate scheduledFor
    ) {

        static TaskNode from(Task task) {
            return new TaskNode(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getEstimateMinutes(),
                    task.getOrderIndex(),
                    task.getStatus(),
                    task.getScheduledFor());
        }
    }
}
