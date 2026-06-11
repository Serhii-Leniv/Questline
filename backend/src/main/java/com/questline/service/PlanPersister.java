package com.questline.service;

import com.questline.ai.GeneratedPlan;
import com.questline.ai.PlannedMilestone;
import com.questline.ai.PlannedTask;
import com.questline.domain.Goal;
import com.questline.domain.Milestone;
import com.questline.domain.Task;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Persists a {@link GeneratedPlan} as a goal's Milestone/Task tree (appending after any existing
 * milestones). Shared by AI accept/replan and template import so the mapping lives in one place.
 */
@Component
public class PlanPersister {

    private final TopicService topicService;

    public PlanPersister(TopicService topicService) {
        this.topicService = topicService;
    }

    public void persist(UUID userId, Goal goal, GeneratedPlan plan) {
        int milestoneIndex = goal.getMilestones().size(); // append after any kept milestones
        for (PlannedMilestone plannedMilestone : plan.milestones()) {
            Milestone milestone = new Milestone();
            milestone.setGoal(goal);
            milestone.setTitle(plannedMilestone.title());
            milestone.setDescription(plannedMilestone.description());
            milestone.setOrderIndex(milestoneIndex++);

            int taskIndex = 0;
            for (PlannedTask plannedTask : plannedMilestone.tasks()) {
                Task task = new Task();
                task.setUser(goal.getUser());
                task.setGoal(goal);
                task.setMilestone(milestone);
                task.setTitle(plannedTask.title());
                task.setDescription(plannedTask.description());
                task.setEstimateMinutes(plannedTask.estimateMinutes());
                task.setTopics(topicService.findOrCreate(userId, plannedTask.topics()));
                task.setOrderIndex(taskIndex++);
                milestone.getTasks().add(task);
            }
            goal.getMilestones().add(milestone);
        }
        // Goal.milestones (cascade ALL) and Milestone.tasks (cascade ALL) persist the whole tree.
    }
}
