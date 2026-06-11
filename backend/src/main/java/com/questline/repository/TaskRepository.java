package com.questline.repository;

import com.questline.domain.Task;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Task access. Every finder is scoped by the owning user id (traversed via {@code user.id}).
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUser_Id(UUID id, UUID userId);

    List<Task> findByUser_IdAndScheduledForOrderByOrderIndexAscCreatedAtAsc(UUID userId, LocalDate scheduledFor);

    List<Task> findByUser_IdAndIdIn(UUID userId, List<UUID> ids);

    long countByGoal_Id(UUID goalId);
}
