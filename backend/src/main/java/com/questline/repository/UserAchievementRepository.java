package com.questline.repository;

import com.questline.domain.UserAchievement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    boolean existsByUser_IdAndAchievement_Code(UUID userId, String code);

    List<UserAchievement> findByUser_IdOrderByUnlockedAtDesc(UUID userId);
}
