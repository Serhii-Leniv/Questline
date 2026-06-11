package com.questline.repository;

import com.questline.domain.Goal;
import com.questline.domain.GoalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Goal access. Every finder is scoped by the owning user id (traversed via {@code user.id}),
 * so a query can never return another user's goal.
 */
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<Goal> findByUser_IdAndStatusOrderByCreatedAtDesc(UUID userId, GoalStatus status);

    Optional<Goal> findByIdAndUser_Id(UUID id, UUID userId);
}
