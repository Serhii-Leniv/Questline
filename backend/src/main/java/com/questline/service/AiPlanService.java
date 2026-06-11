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
import com.questline.domain.AiMessage;
import com.questline.domain.Milestone;
import com.questline.domain.MilestoneStatus;
import com.questline.domain.Task;
import com.questline.jobs.PlanJobService;
import com.questline.repository.AiJobRepository;
import com.questline.repository.AiMessageRepository;
import com.questline.repository.GoalRepository;
import com.questline.repository.TaskRepository;
import com.questline.repository.UserRepository;
import java.util.List;
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
    private final TaskRepository taskRepository;
    private final PlanJobService planJobService;
    private final JobScheduler jobScheduler;
    private final ObjectMapper objectMapper;
    private final AiRateLimiter rateLimiter;
    private final AiMessageRepository aiMessageRepository;
    private final TopicService topicService;

    public AiPlanService(GoalRepository goalRepository, AiJobRepository aiJobRepository,
                         UserRepository userRepository, TaskRepository taskRepository,
                         PlanJobService planJobService, JobScheduler jobScheduler,
                         ObjectMapper objectMapper, AiRateLimiter rateLimiter,
                         AiMessageRepository aiMessageRepository, TopicService topicService) {
        this.goalRepository = goalRepository;
        this.aiJobRepository = aiJobRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.planJobService = planJobService;
        this.jobScheduler = jobScheduler;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.aiMessageRepository = aiMessageRepository;
        this.topicService = topicService;
    }

    /** Creates a draft goal + a PENDING job and enqueues generation. Returns the job. */
    @Transactional
    public AiJob startPlan(UUID userId, String context, String target, LocalDate targetDate,
                           Integer weeklyCapacityMinutes) {
        rateLimiter.check(userId);
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
        jobScheduler.enqueue(() -> planJobService.run(jobId));
        return job;
    }

    /** Flow B: imports a free-text roadmap. Creates a draft goal + a PENDING parse job. */
    @Transactional
    public AiJob startParse(UUID userId, String roadmapText) {
        rateLimiter.check(userId);
        Goal goal = new Goal();
        goal.setUser(userRepository.getReferenceById(userId));
        goal.setTitle(deriveImportTitle(roadmapText));
        goal.setSource(GoalSource.IMPORTED);
        goalRepository.save(goal);

        AiJob job = new AiJob();
        job.setUser(userRepository.getReferenceById(userId));
        job.setGoal(goal);
        job.setType(AiJobType.PARSE_ROADMAP);
        job.setStatus(AiJobStatus.PENDING);
        job.setInput(Map.<String, Object>of("text", roadmapText));
        aiJobRepository.save(job);

        UUID jobId = job.getId();
        jobScheduler.enqueue(() -> planJobService.run(jobId));
        return job;
    }

    /** Flow C: enqueues breaking one task into subtasks. Creates a PENDING decompose job. */
    @Transactional
    public AiJob startDecompose(UUID userId, UUID taskId) {
        rateLimiter.check(userId);
        Task task = taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        AiJob job = new AiJob();
        job.setUser(userRepository.getReferenceById(userId));
        job.setGoal(task.getGoal());
        job.setType(AiJobType.DECOMPOSE_TASK);
        job.setStatus(AiJobStatus.PENDING);
        job.setInput(Map.<String, Object>of(
                "taskId", taskId.toString(), "context", buildDecomposeContext(task)));
        aiJobRepository.save(job);

        UUID jobId = job.getId();
        jobScheduler.enqueue(() -> planJobService.run(jobId));
        return job;
    }

    /**
     * Flow A refine: records the user's message and regenerates the plan, enriching the prompt with
     * the current plan and the running list of requested changes. Returns the new generation job.
     */
    @Transactional
    public AiJob startRefine(UUID userId, UUID goalId, String message) {
        rateLimiter.check(userId);
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));

        AiMessage userMessage = new AiMessage();
        userMessage.setGoal(goal);
        userMessage.setRole("user");
        userMessage.setContent(message);
        aiMessageRepository.save(userMessage);

        List<AiMessage> history = aiMessageRepository.findByGoal_IdOrderByCreatedAtAsc(goalId);
        GeneratedPlan priorPlan = aiJobRepository
                .findFirstByGoal_IdAndUser_IdAndStatusOrderByFinishedAtDesc(
                        goalId, userId, AiJobStatus.SUCCEEDED)
                .map(job -> objectMapper.convertValue(job.getOutput(), GeneratedPlan.class))
                .orElse(null);

        PlanRequest request = new PlanRequest(
                buildRefineContext(goal, priorPlan, history), goal.getTarget(), goal.getTargetDate(), null);
        AiJob job = new AiJob();
        job.setUser(userRepository.getReferenceById(userId));
        job.setGoal(goal);
        job.setType(AiJobType.GENERATE_PLAN);
        job.setStatus(AiJobStatus.PENDING);
        job.setInput(toMap(request));
        aiJobRepository.save(job);

        UUID jobId = job.getId();
        jobScheduler.enqueue(() -> planJobService.run(jobId));
        return job;
    }

    private static String buildRefineContext(Goal goal, GeneratedPlan priorPlan, List<AiMessage> history) {
        StringBuilder sb = new StringBuilder();
        if (goal.getContext() != null && !goal.getContext().isBlank()) {
            sb.append("Background: ").append(goal.getContext()).append("\n\n");
        }
        if (priorPlan != null) {
            sb.append("Current plan summary: ").append(priorPlan.summary()).append('\n');
            sb.append("Current milestones: ");
            sb.append(priorPlan.milestones().stream().map(PlannedMilestone::title)
                    .collect(java.util.stream.Collectors.joining(", ")));
            sb.append("\n\n");
        }
        sb.append("The user requested these changes (oldest to newest):\n");
        history.stream().filter(m -> "user".equals(m.getRole()))
                .forEach(m -> sb.append("- ").append(m.getContent()).append('\n'));
        sb.append("\nProduce an updated plan that reflects all of these changes.");
        return sb.toString();
    }

    /**
     * Flow E: regenerates the remaining work for a goal so it fits the deadline. Produces a plan
     * for review; {@link #acceptReplan} applies it.
     */
    @Transactional
    public AiJob startReplan(UUID userId, UUID goalId) {
        rateLimiter.check(userId);
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));

        long total = taskRepository.countByGoal_Id(goalId);
        long done = taskRepository.countByGoal_IdAndStatus(goalId, com.questline.domain.TaskStatus.DONE);

        PlanRequest request = new PlanRequest(
                buildReplanContext(goal, done, total), goal.getTarget(), goal.getTargetDate(), null);
        AiJob job = new AiJob();
        job.setUser(userRepository.getReferenceById(userId));
        job.setGoal(goal);
        job.setType(AiJobType.GENERATE_PLAN);
        job.setStatus(AiJobStatus.PENDING);
        job.setInput(toMap(request));
        aiJobRepository.save(job);

        UUID jobId = job.getId();
        jobScheduler.enqueue(() -> planJobService.run(jobId));
        return job;
    }

    /**
     * Applies the latest replan: removes the goal's unfinished milestones (and their tasks) and
     * appends the regenerated ones, keeping completed milestones. Lossy by design — partially-done
     * milestones are replaced wholesale.
     */
    @Transactional
    public Goal acceptReplan(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        AiJob job = aiJobRepository
                .findFirstByGoal_IdAndUser_IdAndStatusOrderByFinishedAtDesc(
                        goalId, userId, AiJobStatus.SUCCEEDED)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "NO_PLAN",
                        "There is no regenerated plan to apply for this goal"));

        // orphanRemoval on Goal.milestones deletes the removed milestones and their tasks.
        goal.getMilestones().removeIf(milestone -> milestone.getStatus() != MilestoneStatus.DONE);

        GeneratedPlan plan = objectMapper.convertValue(job.getOutput(), GeneratedPlan.class);
        persistTree(userId, goal, plan);
        // Kept milestones load their tasks lazily; initialize the tree for the response mapping.
        goal.getMilestones().forEach(milestone -> milestone.getTasks().forEach(AiPlanService::initSubtasks));
        return goal;
    }

    private static void initSubtasks(Task task) {
        task.getSubtasks().forEach(AiPlanService::initSubtasks);
    }

    private static String buildReplanContext(Goal goal, long done, long total) {
        StringBuilder sb = new StringBuilder();
        if (goal.getContext() != null && !goal.getContext().isBlank()) {
            sb.append("Background: ").append(goal.getContext()).append('\n');
        }
        sb.append("Progress so far: ").append(done).append(" of ").append(total)
                .append(" tasks completed.\n");
        if (goal.getTargetDate() != null) {
            sb.append("Deadline: ").append(goal.getTargetDate()).append('\n');
        }
        sb.append("Replan ONLY the remaining work so it realistically fits before the deadline. "
                + "Produce milestones and tasks for what is left to do.");
        return sb.toString();
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
        persistTree(userId, goal, plan);
        return goal;
    }

    private void persistTree(UUID userId, Goal goal, GeneratedPlan plan) {
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

    private static String buildDecomposeContext(Task task) {
        Goal goal = task.getGoal();
        StringBuilder sb = new StringBuilder("Goal: ")
                .append(goal.getTarget() != null ? goal.getTarget() : goal.getTitle()).append('\n')
                .append("Task to break down: ").append(task.getTitle());
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            sb.append("\nTask details: ").append(task.getDescription());
        }
        sb.append("\nBreak this task into concrete subtasks.");
        return sb.toString();
    }

    /** Title for an imported goal: the first non-empty line of the roadmap (markdown heading ok). */
    private static String deriveImportTitle(String text) {
        for (String line : text.split("\n")) {
            String cleaned = line.strip().replaceFirst("^#+\\s*", "");
            if (!cleaned.isBlank()) {
                return deriveTitle(cleaned);
            }
        }
        return "Imported roadmap";
    }
}
