package com.questline.service;

import com.questline.domain.Goal;
import com.questline.domain.GoalStatus;
import com.questline.domain.Milestone;
import com.questline.domain.MilestoneStatus;
import com.questline.domain.Task;
import com.questline.domain.TaskStatus;
import com.questline.repository.TaskRepository;
import org.springframework.stereotype.Service;

/**
 * Recomputes cached goal/milestone progress from the current task statuses, and marks a milestone
 * DONE or a goal COMPLETED when all its tasks are done (granting the matching achievement once).
 *
 * <p>Runs inside the caller's transaction. Counts are read via the repository, which Hibernate
 * auto-flushes first, so a just-changed task status is reflected.
 */
@Service
public class ProgressService {

    private final TaskRepository taskRepository;
    private final AchievementService achievementService;

    public ProgressService(TaskRepository taskRepository, AchievementService achievementService) {
        this.taskRepository = taskRepository;
        this.achievementService = achievementService;
    }

    public void recompute(Task task) {
        Milestone milestone = task.getMilestone();
        if (milestone != null) {
            long total = taskRepository.countByMilestone_Id(milestone.getId());
            long done = taskRepository.countByMilestone_IdAndStatus(milestone.getId(), TaskStatus.DONE);
            milestone.setProgress(fraction(done, total));
            MilestoneStatus status = milestoneStatus(total, done);
            if (status == MilestoneStatus.DONE && milestone.getStatus() != MilestoneStatus.DONE) {
                achievementService.grant(task.getUser(), "MILESTONE_DONE");
            }
            milestone.setStatus(status);
        }

        Goal goal = task.getGoal();
        long total = taskRepository.countByGoal_Id(goal.getId());
        long done = taskRepository.countByGoal_IdAndStatus(goal.getId(), TaskStatus.DONE);
        goal.setProgress(fraction(done, total));
        // Auto-complete on full completion; never auto-revert (status is user-meaningful).
        if (total > 0 && done == total && goal.getStatus() != GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.COMPLETED);
            achievementService.grant(task.getUser(), "GOAL_DONE");
        }
    }

    private static double fraction(long done, long total) {
        return total == 0 ? 0 : (double) done / total;
    }

    private static MilestoneStatus milestoneStatus(long total, long done) {
        if (total > 0 && done == total) {
            return MilestoneStatus.DONE;
        }
        return done > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }
}
