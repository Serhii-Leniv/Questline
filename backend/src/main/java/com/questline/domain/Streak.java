package com.questline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "streaks")
@Getter
@Setter
public class Streak {

    /** Shares the User's id (@MapsId). */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "current", nullable = false)
    private int current = 0;

    @Column(nullable = false)
    private int longest = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "freezes_available", nullable = false)
    private int freezesAvailable = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
