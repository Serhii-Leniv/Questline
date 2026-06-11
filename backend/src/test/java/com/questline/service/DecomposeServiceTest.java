package com.questline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.questline.ai.PlannedTask;
import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.domain.Goal;
import com.questline.domain.Task;
import com.questline.domain.User;
import com.questline.repository.AiJobRepository;
import com.questline.repository.TaskRepository;
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

@ExtendWith(MockitoExtension.class)
class DecomposeServiceTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();

    @Mock
    AiJobRepository aiJobRepository;

    @Mock
    TaskRepository taskRepository;

    private DecomposeService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneId.of("UTC"));
        service = new DecomposeService(aiJobRepository, taskRepository, clock);
    }

    @Test
    void persistsSubtasksAsChildren_andMarksJobSucceeded() {
        AiJob job = new AiJob();
        job.setStatus(AiJobStatus.RUNNING);
        job.setInput(Map.of("taskId", TASK_ID.toString()));

        Task parent = new Task();
        parent.setUser(new User());
        parent.setGoal(new Goal());

        when(aiJobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(parent));
        when(taskRepository.countByParentTask_Id(TASK_ID)).thenReturn(0L);

        service.completeDecompose(JOB_ID, List.of(
                new PlannedTask("Step 1", "d", 20), new PlannedTask("Step 2", null, null)));

        verify(taskRepository, times(2)).save(any(Task.class));
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(job.getOutput().get("createdSubtasks")).isEqualTo(2);
        assertThat(job.getFinishedAt()).isNotNull();
    }
}
