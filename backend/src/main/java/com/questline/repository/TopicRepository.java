package com.questline.repository;

import com.questline.domain.Topic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findByUser_IdAndSlug(UUID userId, String slug);

    List<Topic> findByUser_IdOrderByName(UUID userId);
}
