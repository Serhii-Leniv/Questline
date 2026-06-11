package com.questline.repository;

import com.questline.domain.Milestone;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    /** Scopes a milestone to a goal, so a task can only attach to a milestone of its own goal. */
    Optional<Milestone> findByIdAndGoal_Id(UUID id, UUID goalId);
}
