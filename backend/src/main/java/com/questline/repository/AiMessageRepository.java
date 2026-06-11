package com.questline.repository;

import com.questline.domain.AiMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findByGoal_IdOrderByCreatedAtAsc(UUID goalId);
}
