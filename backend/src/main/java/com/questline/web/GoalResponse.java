package com.questline.web;

import com.questline.domain.Goal;
import com.questline.domain.GoalSource;
import com.questline.domain.GoalStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Flat goal view (no tree). Used by list/create/update/archive responses. */
public record GoalResponse(
        UUID id,
        String title,
        String description,
        String context,
        String target,
        GoalSource source,
        LocalDate targetDate,
        GoalStatus status,
        double progress,
        Instant createdAt,
        Instant updatedAt
) {

    public static GoalResponse from(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getContext(),
                goal.getTarget(),
                goal.getSource(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getProgress(),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}
