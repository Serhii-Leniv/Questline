package com.questline.service;

import com.questline.domain.Topic;
import com.questline.repository.TaskRepository;
import com.questline.repository.TopicRepository;
import com.questline.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages free-text topic tags. Topics are auto-created per user (find-or-create by slug), and the
 * stats view reports completion per topic. The slug keeps Unicode letters/digits so non-Latin
 * topic names work.
 */
@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TopicService(TopicRepository topicRepository, TaskRepository taskRepository,
                        UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Resolves topic names to persistent topics for the user, creating any that don't exist. */
    @Transactional
    public List<Topic> findOrCreate(UUID userId, List<String> names) {
        List<Topic> result = new ArrayList<>();
        if (names == null) {
            return result;
        }
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.strip();
            String slug = slugify(name);
            if (slug.isEmpty()) {
                continue;
            }
            Topic topic = topicRepository.findByUser_IdAndSlug(userId, slug)
                    .orElseGet(() -> create(userId, name, slug));
            result.add(topic);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TopicProgress> progress(UUID userId) {
        return topicRepository.findByUser_IdOrderByName(userId).stream()
                .map(topic -> new TopicProgress(
                        topic.getName(),
                        topic.getSlug(),
                        taskRepository.countByTopics_Id(topic.getId()),
                        taskRepository.countByTopics_IdAndStatus(topic.getId(),
                                com.questline.domain.TaskStatus.DONE)))
                .toList();
    }

    private Topic create(UUID userId, String name, String slug) {
        Topic topic = new Topic();
        topic.setUser(userRepository.getReferenceById(userId));
        topic.setName(name);
        topic.setSlug(slug);
        return topicRepository.save(topic);
    }

    static String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /** Completion stats for one topic. */
    public record TopicProgress(String name, String slug, long total, long done) {
    }
}
