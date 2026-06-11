package com.questline.repository;

import com.questline.domain.GoalTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalTemplateRepository extends JpaRepository<GoalTemplate, UUID> {

    List<GoalTemplate> findAllByOrderByCreatedAtDesc();
}
