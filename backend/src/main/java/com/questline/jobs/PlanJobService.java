package com.questline.jobs;

import com.questline.ai.GeneratedPlan;
import com.questline.ai.LlmClient;
import com.questline.ai.PlanRequest;
import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.repository.AiJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * JobRunr worker that runs an AI plan generation off the request thread. Each status transition
 * runs in its own short transaction so the (slow) LLM call never holds a database connection open.
 * Failures are captured on the {@link AiJob} (status FAILED + error) rather than thrown, so the
 * user sees a clear message and can retry.
 */
@Service
public class PlanJobService {

    private static final Logger log = LoggerFactory.getLogger(PlanJobService.class);

    private final AiJobRepository aiJobRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PlanJobService(AiJobRepository aiJobRepository, LlmClient llmClient,
                          ObjectMapper objectMapper, PlatformTransactionManager txManager, Clock clock) {
        this.aiJobRepository = aiJobRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    /** Entry point enqueued by JobRunr. Idempotent: a job already SUCCEEDED is skipped. */
    public void generate(java.util.UUID jobId) {
        Map<String, Object> input = tx.execute(status -> startRun(jobId));
        if (input == null) {
            return; // already succeeded, or nothing to do
        }
        try {
            PlanRequest request = objectMapper.convertValue(input, PlanRequest.class);
            GeneratedPlan plan = llmClient.generatePlan(request);
            tx.executeWithoutResult(status -> markSucceeded(jobId, plan));
        } catch (RuntimeException e) {
            log.warn("Plan job {} failed: {}", jobId, e.toString());
            tx.executeWithoutResult(status -> markFailed(jobId, e));
        }
    }

    private Map<String, Object> startRun(java.util.UUID jobId) {
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        if (job.getStatus() == AiJobStatus.SUCCEEDED) {
            return null;
        }
        job.setStatus(AiJobStatus.RUNNING);
        job.setAttempts(job.getAttempts() + 1);
        job.setError(null);
        return job.getInput();
    }

    private void markSucceeded(java.util.UUID jobId, GeneratedPlan plan) {
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        job.setOutput(toMap(plan));
        job.setStatus(AiJobStatus.SUCCEEDED);
        job.setFinishedAt(Instant.now(clock));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(GeneratedPlan plan) {
        return objectMapper.convertValue(plan, Map.class);
    }

    private void markFailed(java.util.UUID jobId, RuntimeException error) {
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        String message = error.getMessage() != null ? error.getMessage() : error.toString();
        job.setError(message);
        job.setStatus(AiJobStatus.FAILED);
        job.setFinishedAt(Instant.now(clock));
    }
}
