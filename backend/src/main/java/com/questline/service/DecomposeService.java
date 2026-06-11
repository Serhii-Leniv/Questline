package com.questline.service;

import com.questline.ai.PlannedTask;
import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.domain.Task;
import com.questline.repository.AiJobRepository;
import com.questline.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the result of a task-decomposition job: the proposed subtasks are saved as children of
 * the original task, and the job is marked SUCCEEDED. Kept separate from {@code AiPlanService} so
 * the job worker can depend on it without a dependency cycle.
 */
@Service
public class DecomposeService {

    private final AiJobRepository aiJobRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    public DecomposeService(AiJobRepository aiJobRepository, TaskRepository taskRepository, Clock clock) {
        this.aiJobRepository = aiJobRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional
    public void completeDecompose(UUID jobId, List<PlannedTask> subtasks) {
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        UUID taskId = UUID.fromString((String) job.getInput().get("taskId"));
        Task parent = taskRepository.findById(taskId).orElseThrow();

        int order = (int) taskRepository.countByParentTask_Id(taskId);
        for (PlannedTask planned : subtasks) {
            Task child = new Task();
            child.setUser(parent.getUser());
            child.setGoal(parent.getGoal());
            child.setParentTask(parent);
            child.setTitle(planned.title());
            child.setDescription(planned.description());
            child.setEstimateMinutes(planned.estimateMinutes());
            child.setOrderIndex(order++);
            taskRepository.save(child);
        }

        job.setOutput(Map.<String, Object>of("createdSubtasks", subtasks.size()));
        job.setStatus(AiJobStatus.SUCCEEDED);
        job.setFinishedAt(Instant.now(clock));
    }
}
