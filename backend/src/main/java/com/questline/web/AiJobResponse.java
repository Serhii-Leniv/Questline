package com.questline.web;

import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.domain.AiJobType;
import java.util.Map;
import java.util.UUID;

/**
 * Polling view of a job. {@code plan} is the generated plan (raw JSON) once the job has SUCCEEDED,
 * and {@code error} is set when it has FAILED.
 */
public record AiJobResponse(
        UUID id,
        AiJobType type,
        AiJobStatus status,
        String error,
        Map<String, Object> plan
) {

    public static AiJobResponse from(AiJob job) {
        return new AiJobResponse(job.getId(), job.getType(), job.getStatus(), job.getError(),
                job.getOutput());
    }
}
