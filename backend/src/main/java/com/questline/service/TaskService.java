package com.questline.service;

import com.questline.common.NotFoundException;
import com.questline.domain.Goal;
import com.questline.domain.Milestone;
import com.questline.domain.ResourceLink;
import com.questline.domain.Task;
import com.questline.domain.TaskStatus;
import com.questline.domain.User;
import com.questline.repository.GoalRepository;
import com.questline.repository.MilestoneRepository;
import com.questline.repository.TaskRepository;
import com.questline.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task business logic. Every operation is scoped to the authenticated {@code userId}; the goal
 * and milestone a task attaches to are verified to belong to that user.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, GoalRepository goalRepository,
                       MilestoneRepository milestoneRepository, UserRepository userRepository,
                       GamificationService gamificationService, Clock clock) {
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
        this.clock = clock;
    }

    @Transactional
    public Task create(UUID userId, UUID goalId, UUID milestoneId, String title, String description,
                       Integer estimateMinutes, LocalDate scheduledFor, String notes,
                       List<ResourceLink> resources) {
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        Milestone milestone = null;
        if (milestoneId != null) {
            milestone = milestoneRepository.findByIdAndGoal_Id(milestoneId, goalId)
                    .orElseThrow(() -> new NotFoundException("Milestone not found in goal"));
        }

        Task task = new Task();
        task.setUser(userRepository.getReferenceById(userId));
        task.setGoal(goal);
        task.setMilestone(milestone);
        task.setTitle(title);
        task.setDescription(description);
        task.setEstimateMinutes(estimateMinutes);
        task.setScheduledFor(scheduledFor);
        task.setNotes(notes);
        task.setResources(resources);
        // Append to the end of the goal's task list.
        task.setOrderIndex((int) taskRepository.countByGoal_Id(goalId));
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task get(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    /** Tasks scheduled for "today" in the user's IANA timezone (local midnight boundary). */
    @Transactional(readOnly = true)
    public List<Task> today(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
        return taskRepository.findByUser_IdAndScheduledForOrderByOrderIndexAscCreatedAtAsc(userId, today);
    }

    /** Partial update of editable fields — only non-null values are applied. */
    @Transactional
    public Task update(UUID userId, UUID taskId, String title, String description,
                       Integer estimateMinutes, String notes, List<ResourceLink> resources) {
        Task task = get(userId, taskId);
        if (title != null) {
            task.setTitle(title);
        }
        if (description != null) {
            task.setDescription(description);
        }
        if (estimateMinutes != null) {
            task.setEstimateMinutes(estimateMinutes);
        }
        if (notes != null) {
            task.setNotes(notes);
        }
        if (resources != null) {
            task.setResources(resources);
        }
        return task;
    }

    @Transactional
    public Task changeStatus(UUID userId, UUID taskId, TaskStatus status) {
        Task task = get(userId, taskId);
        task.setStatus(status);
        // Gamification fires exactly once per task — on its first-ever completion. completedAt is
        // the first-completion marker: it is set once and never cleared, so re-opening and
        // re-completing a task cannot inflate the day's count, the streak, or XP.
        if (status == TaskStatus.DONE && task.getCompletedAt() == null) {
            task.setCompletedAt(Instant.now(clock));
            gamificationService.onTaskCompleted(task);
        }
        return task;
    }

    /** Schedules the task on a date, or unschedules it when {@code date} is null. */
    @Transactional
    public Task schedule(UUID userId, UUID taskId, LocalDate date) {
        Task task = get(userId, taskId);
        task.setScheduledFor(date);
        return task;
    }

    /** Applies a new ordering: each task's index becomes its position in {@code ids}. */
    @Transactional
    public void reorder(UUID userId, List<UUID> ids) {
        List<Task> tasks = taskRepository.findByUser_IdAndIdIn(userId, ids);
        if (tasks.size() != ids.size()) {
            throw new NotFoundException("One or more tasks not found");
        }
        Map<UUID, Task> byId = new HashMap<>();
        tasks.forEach(task -> byId.put(task.getId(), task));
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setOrderIndex(i);
        }
    }

    @Transactional
    public void delete(UUID userId, UUID taskId) {
        taskRepository.delete(get(userId, taskId));
    }
}
