package com.questline.service;

import com.questline.ai.GeneratedPlan;
import com.questline.ai.PlanRequest;
import com.questline.ai.PlannedMilestone;
import com.questline.ai.PlannedTask;
import com.questline.common.ApiException;
import com.questline.common.NotFoundException;
import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.domain.AiJobType;
import com.questline.domain.Goal;
import com.questline.domain.GoalSource;
import com.questline.domain.Milestone;
import com.questline.domain.Task;
import com.questline.jobs.PlanJobService;
import com.questline.repository.AiJobRepository;
import com.questline.repository.GoalRepository;
import com.questline.repository.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Orchestrates Flow A (generate a plan from a goal). Creating the plan enqueues a durable JobRunr
 * job — the LLM never runs on the request thread. The generated plan is held on the {@link AiJob}
 * until the user accepts it, at which point it is persisted as the goal's Milestone/Task tree.
 */
@Service
public class AiPlanService {

    private static final int MAX_TITLE_LENGTH = 120;

    private final GoalRepository goalRepository;
    private final AiJobRepository aiJobRepository;
    private final UserRepository userRepository;
    private final PlanJobService planJobService;
    private final JobScheduler jobScheduler;
    private final ObjectMapper objectMapper;

    public AiPlanService(GoalRepository goalRepository, AiJobRepository aiJobRepository,
                         UserRepository userRepository, PlanJobService planJobService,
                         JobScheduler jobScheduler, ObjectMapper objectMapper) {
        this.goalRepository = goalRepository;
        this.aiJobRepository = aiJobRepository;
        this.userRepository = userRepository;
        this.planJobService = planJobService;
        this.jobScheduler = jobScheduler;
        this.objectMapper = objectMapper;
    }

    /** Creates a draft goal + a PENDING job and enqueues generation. Returns the job. */
    @Transactional
    public AiJob startPlan(UUID userId, String context, String target, LocalDate targetDate,
                           Integer weeklyCapacityMinutes) {
        Goal goal = new Goal();
        goal.setUser(userRepository.getReferenceById(userId));
        goal.setTitle(deriveTitle(target));
        goal.setContext(context);
        goal.setTarget(target);
        goal.setTargetDate(targetDate);
        goal.setSource(GoalSource.AI_GENERATED);
        goalRepository.save(goal);

        PlanRequest request = new PlanRequest(context, target, targetDate, weeklyCapacityMinutes);
        AiJob job = new AiJob();
        job.setUser(userRepository.getReferenceById(userId));
        job.setGoal(goal);
        job.setType(AiJobType.GENERATE_PLAN);
        job.setStatus(AiJobStatus.PENDING);
        job.setInput(toMap(request));
        aiJobRepository.save(job);

        UUID jobId = job.getId();
        jobScheduler.enqueue(() -> planJobService.generate(jobId));
        return job;
    }

    @Transactional(readOnly = true)
    public AiJob getJob(UUID userId, UUID jobId) {
        return aiJobRepository.findByIdAndUser_Id(jobId, userId)
                .orElseThrow(() -> new NotFoundException("AI job not found"));
    }

    /**
     * Persists the most recent successfully-generated plan as the goal's tree. Idempotent guard:
     * refuses if the goal already has milestones.
     */
    @Transactional
    public Goal accept(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        if (!goal.getMilestones().isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_ALREADY_ACCEPTED",
                    "This goal already has a plan");
        }
        AiJob job = aiJobRepository
                .findFirstByGoal_IdAndUser_IdAndStatusOrderByFinishedAtDesc(
                        goalId, userId, AiJobStatus.SUCCEEDED)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "NO_PLAN",
                        "There is no generated plan to accept for this goal"));

        GeneratedPlan plan = objectMapper.convertValue(job.getOutput(), GeneratedPlan.class);
        persistTree(goal, plan);
        return goal;
    }

    private void persistTree(Goal goal, GeneratedPlan plan) {
        int milestoneIndex = 0;
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
                task.setOrderIndex(taskIndex++);
                milestone.getTasks().add(task);
            }
            goal.getMilestones().add(milestone);
        }
        // Goal.milestones (cascade ALL) and Milestone.tasks (cascade ALL) persist the whole tree.
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(PlanRequest request) {
        return objectMapper.convertValue(request, Map.class);
    }

    private static String deriveTitle(String target) {
        String trimmed = target.strip();
        return trimmed.length() > MAX_TITLE_LENGTH
                ? trimmed.substring(0, MAX_TITLE_LENGTH - 1).strip() + "…"
                : trimmed;
    }
}
