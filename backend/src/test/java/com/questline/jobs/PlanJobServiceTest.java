package com.questline.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.questline.ai.GeneratedPlan;
import com.questline.ai.LlmClient;
import com.questline.ai.PlanGenerationException;
import com.questline.ai.PlanRequest;
import com.questline.ai.PlannedMilestone;
import com.questline.ai.PlannedTask;
import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.domain.AiJobType;
import com.questline.domain.User;
import com.questline.repository.AiJobRepository;
import com.questline.service.AiSettingsService;
import com.questline.service.DecomposeService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for the JobRunr plan worker, with the LLM and persistence mocked. Verifies the
 * success/failure state machine and idempotency — no Spring, no database, no real model.
 */
@ExtendWith(MockitoExtension.class)
class PlanJobServiceTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final GeneratedPlan PLAN = new GeneratedPlan("summary", List.of(
            new PlannedMilestone("M", "d", List.of(new PlannedTask("T", "d", 30)))));

    @Mock
    AiJobRepository aiJobRepository;

    @Mock
    LlmClient llmClient;

    @Mock
    PlatformTransactionManager txManager;

    @Mock
    DecomposeService decomposeService;

    @Mock
    AiSettingsService aiSettingsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PlanJobService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneId.of("UTC"));
        service = new PlanJobService(aiJobRepository, llmClient, decomposeService, aiSettingsService,
                objectMapper, txManager, clock);
    }

    @Test
    void successPath_storesPlanAndMarksSucceeded() {
        AiJob job = pendingJob();
        when(aiJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(llmClient.generatePlan(any(PlanRequest.class), any())).thenReturn(PLAN);

        service.run(JOB_ID);

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getOutput()).isNotNull();
        assertThat(job.getOutput().get("summary")).isEqualTo("summary");
        assertThat(job.getFinishedAt()).isNotNull();
        assertThat(job.getError()).isNull();
    }

    @Test
    void parseJob_callsParseRoadmap_andMarksSucceeded() {
        AiJob job = new AiJob();
        job.setStatus(AiJobStatus.PENDING);
        job.setType(AiJobType.PARSE_ROADMAP);
        job.setUser(testUser());
        job.setInput(Map.of("text", "Milestone 1\n- task a"));
        when(aiJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(llmClient.parseRoadmap(eq("Milestone 1\n- task a"), any())).thenReturn(PLAN);

        service.run(JOB_ID);

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(job.getOutput()).isNotNull();
    }

    @Test
    void failurePath_capturesErrorAndMarksFailed() {
        AiJob job = pendingJob();
        when(aiJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(llmClient.generatePlan(any(PlanRequest.class), any()))
                .thenThrow(new PlanGenerationException("model gave up", null));

        service.run(JOB_ID);

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.getError()).isEqualTo("model gave up");
        assertThat(job.getFinishedAt()).isNotNull();
    }

    @Test
    void alreadySucceeded_isSkipped() {
        AiJob job = pendingJob();
        job.setStatus(AiJobStatus.SUCCEEDED);
        when(aiJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        service.run(JOB_ID);

        verify(llmClient, never()).generatePlan(any(), any());
    }

    @SuppressWarnings("unchecked")
    private AiJob pendingJob() {
        AiJob job = new AiJob();
        job.setStatus(AiJobStatus.PENDING);
        job.setType(AiJobType.GENERATE_PLAN);
        job.setUser(testUser());
        job.setInput(objectMapper.convertValue(
                new PlanRequest("mid-level", "senior", null, 300), Map.class));
        return job;
    }

    private static User testUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }
}
