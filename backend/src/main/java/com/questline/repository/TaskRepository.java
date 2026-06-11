package com.questline.repository;

import com.questline.domain.Task;
import com.questline.domain.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Task access. Every finder is scoped by the owning user id (traversed via {@code user.id}).
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUser_Id(UUID id, UUID userId);

    List<Task> findByUser_IdAndScheduledForOrderByOrderIndexAscCreatedAtAsc(UUID userId, LocalDate scheduledFor);

    List<Task> findByUser_IdAndScheduledForBetweenOrderByScheduledForAscOrderIndexAsc(
            UUID userId, LocalDate from, LocalDate to);

    List<Task> findByUser_IdAndIdIn(UUID userId, List<UUID> ids);

    long countByGoal_Id(UUID goalId);

    long countByGoal_IdAndStatus(UUID goalId, TaskStatus status);

    long countByMilestone_Id(UUID milestoneId);

    long countByMilestone_IdAndStatus(UUID milestoneId, TaskStatus status);

    long countByParentTask_Id(UUID parentTaskId);

    long countByTopics_Id(UUID topicId);

    long countByTopics_IdAndStatus(UUID topicId, TaskStatus status);

    /**
     * Unscheduled tasks eligible for auto-planning, ordered by goal deadline (soonest first,
     * undated last) then position. The goal is fetched to avoid N+1 when reading its deadline.
     */
    @Query("""
            select t from Task t join fetch t.goal g
            where t.user.id = :userId and t.scheduledFor is null and t.status = :status
            order by case when g.targetDate is null then 1 else 0 end, g.targetDate asc, t.orderIndex asc
            """)
    List<Task> findPlannable(@Param("userId") UUID userId, @Param("status") TaskStatus status);
}
