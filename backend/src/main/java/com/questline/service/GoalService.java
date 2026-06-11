package com.questline.service;

import com.questline.common.NotFoundException;
import com.questline.domain.Goal;
import com.questline.domain.GoalStatus;
import com.questline.repository.GoalRepository;
import com.questline.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Goal business logic. All reads/writes are scoped to the authenticated {@code userId}; callers
 * pass the id resolved from the JWT, never from a request body.
 */
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Goal create(UUID userId, String title, String description, String context,
                       String target, LocalDate targetDate) {
        Goal goal = new Goal();
        // getReferenceById avoids loading the User row just to set the FK.
        goal.setUser(userRepository.getReferenceById(userId));
        goal.setTitle(title);
        goal.setDescription(description);
        goal.setContext(context);
        goal.setTarget(target);
        goal.setTargetDate(targetDate);
        return goalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public List<Goal> list(UUID userId, GoalStatus status) {
        return status == null
                ? goalRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                : goalRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    @Transactional(readOnly = true)
    public Goal get(UUID userId, UUID goalId) {
        return goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
    }

    /**
     * Loads a goal with its {@code Milestone -> Task} tree eagerly initialized, so the web layer
     * can map it after the transaction closes (open-in-view is disabled).
     */
    @Transactional(readOnly = true)
    public Goal getTree(UUID userId, UUID goalId) {
        Goal goal = get(userId, goalId);
        goal.getMilestones().forEach(milestone -> milestone.getTasks().forEach(GoalService::initSubtasks));
        return goal;
    }

    /** Recursively initializes a task's subtask collections while the session is open. */
    private static void initSubtasks(com.questline.domain.Task task) {
        task.getSubtasks().forEach(GoalService::initSubtasks);
    }

    /** Partial update: only non-null fields are applied. */
    @Transactional
    public Goal update(UUID userId, UUID goalId, String title, String description, String context,
                       String target, LocalDate targetDate, GoalStatus status) {
        Goal goal = get(userId, goalId);
        if (title != null) {
            goal.setTitle(title);
        }
        if (description != null) {
            goal.setDescription(description);
        }
        if (context != null) {
            goal.setContext(context);
        }
        if (target != null) {
            goal.setTarget(target);
        }
        if (targetDate != null) {
            goal.setTargetDate(targetDate);
        }
        if (status != null) {
            goal.setStatus(status);
        }
        return goal; // dirty-checked, flushed on commit
    }

    @Transactional
    public Goal archive(UUID userId, UUID goalId) {
        Goal goal = get(userId, goalId);
        goal.setStatus(GoalStatus.ARCHIVED);
        return goal;
    }
}
