package com.questline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activity_days",
        uniqueConstraints = @UniqueConstraint(name = "uq_activity_days_user_date",
                columnNames = {"user_id", "date"}))
@Getter
@Setter
public class ActivityDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** User-local date. */
    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "tasks_completed", nullable = false)
    private int tasksCompleted = 0;

    @Column(name = "minutes_spent", nullable = false)
    private int minutesSpent = 0;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned = 0;

    /** Whether the daily goal was met — counts toward the streak. */
    @Column(name = "goal_met", nullable = false)
    private boolean goalMet = false;
}
