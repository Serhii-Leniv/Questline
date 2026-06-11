package com.questline.repository;

import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

    Optional<AiJob> findByIdAndUser_Id(UUID id, UUID userId);

    /** The most recently finished job of a given status for a goal — used when accepting a plan. */
    Optional<AiJob> findFirstByGoal_IdAndUser_IdAndStatusOrderByFinishedAtDesc(
            UUID goalId, UUID userId, AiJobStatus status);
}
