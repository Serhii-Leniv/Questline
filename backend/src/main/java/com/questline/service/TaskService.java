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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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

    /** Assumed minutes for a task with no estimate, used by capacity-based planning. */
    private static final int DEFAULT_TASK_MINUTES = 30;

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final ProgressService progressService;
    private final TopicService topicService;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, GoalRepository goalRepository,
                       MilestoneRepository milestoneRepository, UserRepository userRepository,
                       GamificationService gamificationService, ProgressService progressService,
                       TopicService topicService, Clock clock) {
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
        this.progressService = progressService;
        this.topicService = topicService;
        this.clock = clock;
    }

    @Transactional
    public Task create(UUID userId, UUID goalId, UUID milestoneId, String title, String description,
                       Integer estimateMinutes, LocalDate scheduledFor, String notes,
                       List<ResourceLink> resources, List<String> topics) {
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
        task.setTopics(topicService.findOrCreate(userId, topics));
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
        return taskRepository.findByUser_IdAndScheduledForOrderByOrderIndexAscCreatedAtAsc(
                userId, today(user));
    }

    /**
     * Auto-schedules unscheduled TODO tasks onto today until the user's remaining daily capacity
     * is used up, taking the highest-priority tasks first (soonest goal deadline, then position).
     * Returns today's task list after planning.
     */
    @Transactional
    public List<Task> planToday(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDate today = today(user);

        List<Task> alreadyToday =
                taskRepository.findByUser_IdAndScheduledForOrderByOrderIndexAscCreatedAtAsc(userId, today);
        int committed = alreadyToday.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .mapToInt(TaskService::estimate)
                .sum();
        int remaining = Math.max(0, user.getDailyCapacityMinutes() - committed);

        for (Task candidate : taskRepository.findPlannable(userId, TaskStatus.TODO)) {
            int cost = estimate(candidate);
            if (cost <= remaining) {
                candidate.setScheduledFor(today);
                remaining -= cost;
            }
        }
        return taskRepository.findByUser_IdAndScheduledForOrderByOrderIndexAscCreatedAtAsc(userId, today);
    }

    /** Tasks scheduled within the 7-day week starting Monday (or {@code start} if given). */
    @Transactional(readOnly = true)
    public List<Task> week(UUID userId, LocalDate start) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDate from = start != null
                ? start
                : today(user).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return taskRepository.findByUser_IdAndScheduledForBetweenOrderByScheduledForAscOrderIndexAsc(
                userId, from, from.plusDays(6));
    }

    private LocalDate today(User user) {
        return LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
    }

    /** Minutes a task is expected to take; tasks without an estimate use a default. */
    private static int estimate(Task task) {
        return task.getEstimateMinutes() != null ? task.getEstimateMinutes() : DEFAULT_TASK_MINUTES;
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
        // Recompute on every status change — progress depends on the current done counts.
        progressService.recompute(task);
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
