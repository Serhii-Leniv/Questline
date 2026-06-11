package com.questline.repository;

import com.questline.domain.Streak;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link Streak} shares the User's id (@MapsId), so the primary key is the user id. */
public interface StreakRepository extends JpaRepository<Streak, UUID> {

    List<Streak> findByCurrentGreaterThan(int value);
}
