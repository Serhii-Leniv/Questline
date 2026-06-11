package com.questline.repository;

import com.questline.domain.ActivityDay;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityDayRepository extends JpaRepository<ActivityDay, UUID> {

    Optional<ActivityDay> findByUser_IdAndDate(UUID userId, LocalDate date);

    List<ActivityDay> findByUser_IdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate from, LocalDate to);
}
