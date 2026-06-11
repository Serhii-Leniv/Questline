package com.questline.service;

import com.questline.ai.GeneratedPlan;
import com.questline.ai.PlannedMilestone;
import com.questline.ai.PlannedTask;
import com.questline.common.NotFoundException;
import com.questline.domain.Goal;
import com.questline.domain.GoalSource;
import com.questline.domain.GoalTemplate;
import com.questline.domain.Milestone;
import com.questline.domain.Task;
import com.questline.domain.Topic;
import com.questline.repository.GoalRepository;
import com.questline.repository.GoalTemplateRepository;
import com.questline.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishing and importing public roadmap templates. Publishing snapshots a goal's structure into
 * a template (no per-user data); importing recreates that structure as a new goal for the importer.
 */
@Service
public class TemplateService {

    private final GoalTemplateRepository templateRepository;
    private final GoalService goalService;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final PlanPersister planPersister;
    private final ObjectMapper objectMapper;

    public TemplateService(GoalTemplateRepository templateRepository, GoalService goalService,
                           GoalRepository goalRepository, UserRepository userRepository,
                           PlanPersister planPersister, ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.goalService = goalService;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.planPersister = planPersister;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GoalTemplate publish(UUID userId, UUID goalId) {
        Goal goal = goalService.getTree(userId, goalId); // scoped + tree initialized
        GeneratedPlan plan = toPlan(goal);

        GoalTemplate template = new GoalTemplate();
        template.setAuthor(userRepository.getReferenceById(userId));
        template.setTitle(goal.getTitle());
        template.setSummary(plan.summary());
        template.setPlan(toMap(plan));
        template.setTaskCount(plan.milestones().stream().mapToInt(m -> m.tasks().size()).sum());
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<GoalTemplate> list() {
        return templateRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public GoalTemplate get(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found"));
    }

    /** Recreates the template as a new goal owned by the importer; returns the new goal's tree. */
    @Transactional
    public Goal importTemplate(UUID userId, UUID templateId) {
        GoalTemplate template = get(templateId);
        GeneratedPlan plan = objectMapper.convertValue(template.getPlan(), GeneratedPlan.class);

        Goal goal = new Goal();
        goal.setUser(userRepository.getReferenceById(userId));
        goal.setTitle(template.getTitle());
        goal.setDescription(plan.summary());
        goal.setSource(GoalSource.IMPORTED);
        goalRepository.save(goal);

        planPersister.persist(userId, goal, plan);
        return goal;
    }

    /** Builds a plan snapshot from a goal's top-level milestones/tasks (subtasks are flattened out). */
    private static GeneratedPlan toPlan(Goal goal) {
        List<PlannedMilestone> milestones = goal.getMilestones().stream()
                .sorted(Comparator.comparingInt(Milestone::getOrderIndex))
                .map(TemplateService::toMilestone)
                .toList();
        String summary = goal.getDescription() != null ? goal.getDescription() : goal.getTarget();
        return new GeneratedPlan(summary, milestones);
    }

    private static PlannedMilestone toMilestone(Milestone milestone) {
        List<PlannedTask> tasks = milestone.getTasks().stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .map(task -> new PlannedTask(task.getTitle(), task.getDescription(),
                        task.getEstimateMinutes(), task.getTopics().stream().map(Topic::getName).toList()))
                .toList();
        return new PlannedMilestone(milestone.getTitle(), milestone.getDescription(), tasks);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> toMap(GeneratedPlan plan) {
        return objectMapper.convertValue(plan, java.util.Map.class);
    }
}
