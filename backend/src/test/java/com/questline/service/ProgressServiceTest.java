package com.questline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.questline.domain.Goal;
import com.questline.domain.GoalStatus;
import com.questline.domain.Milestone;
import com.questline.domain.MilestoneStatus;
import com.questline.domain.Task;
import com.questline.domain.TaskStatus;
import com.questline.domain.User;
import com.questline.repository.TaskRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final UUID GOAL_ID = UUID.randomUUID();
    private static final UUID MILESTONE_ID = UUID.randomUUID();

    @Mock
    TaskRepository taskRepository;

    @Mock
    AchievementService achievementService;

    private ProgressService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ProgressService(taskRepository, achievementService);
        user = new User();
        user.setId(UUID.randomUUID());
    }

    @Test
    void fullCompletion_marksDone_andGrantsAchievements() {
        Goal goal = goal(GoalStatus.ACTIVE);
        Milestone milestone = milestone(MilestoneStatus.IN_PROGRESS);
        stubCounts(2, 2, 2, 2);

        service.recompute(task(goal, milestone));

        assertThat(milestone.getProgress()).isEqualTo(1.0);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.DONE);
        assertThat(goal.getProgress()).isEqualTo(1.0);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        verify(achievementService).grant(user, "MILESTONE_DONE");
        verify(achievementService).grant(user, "GOAL_DONE");
    }

    @Test
    void partialCompletion_setsProgress_noAchievements() {
        Goal goal = goal(GoalStatus.ACTIVE);
        Milestone milestone = milestone(MilestoneStatus.NOT_STARTED);
        stubCounts(4, 1, 4, 1);

        service.recompute(task(goal, milestone));

        assertThat(milestone.getProgress()).isEqualTo(0.25);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.IN_PROGRESS);
        assertThat(goal.getProgress()).isEqualTo(0.25);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        verify(achievementService, never()).grant(eq(user), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void taskWithoutMilestone_recomputesGoalOnly() {
        Goal goal = goal(GoalStatus.ACTIVE);
        when(taskRepository.countByGoal_Id(GOAL_ID)).thenReturn(2L);
        when(taskRepository.countByGoal_IdAndStatus(GOAL_ID, TaskStatus.DONE)).thenReturn(2L);

        service.recompute(task(goal, null));

        assertThat(goal.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        verify(achievementService).grant(user, "GOAL_DONE");
        verify(achievementService, never()).grant(user, "MILESTONE_DONE");
    }

    private void stubCounts(long mTotal, long mDone, long gTotal, long gDone) {
        when(taskRepository.countByMilestone_Id(MILESTONE_ID)).thenReturn(mTotal);
        when(taskRepository.countByMilestone_IdAndStatus(MILESTONE_ID, TaskStatus.DONE)).thenReturn(mDone);
        when(taskRepository.countByGoal_Id(GOAL_ID)).thenReturn(gTotal);
        when(taskRepository.countByGoal_IdAndStatus(GOAL_ID, TaskStatus.DONE)).thenReturn(gDone);
    }

    private Goal goal(GoalStatus status) {
        Goal goal = new Goal();
        goal.setId(GOAL_ID);
        goal.setStatus(status);
        return goal;
    }

    private Milestone milestone(MilestoneStatus status) {
        Milestone milestone = new Milestone();
        milestone.setId(MILESTONE_ID);
        milestone.setStatus(status);
        return milestone;
    }

    private Task task(Goal goal, Milestone milestone) {
        Task task = new Task();
        task.setUser(user);
        task.setGoal(goal);
        task.setMilestone(milestone);
        return task;
    }
}
