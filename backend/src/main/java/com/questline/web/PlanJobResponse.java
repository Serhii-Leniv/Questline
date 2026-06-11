package com.questline.web;

import com.questline.domain.AiJob;
import java.util.UUID;

/** Returned when a plan generation is started: the job to poll and the draft goal it fills in. */
public record PlanJobResponse(UUID jobId, UUID goalId) {

    public static PlanJobResponse from(AiJob job) {
        return new PlanJobResponse(job.getId(), job.getGoal().getId());
    }
}
