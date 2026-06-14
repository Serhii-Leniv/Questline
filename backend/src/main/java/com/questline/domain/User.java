package com.questline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String image;

    /** IANA timezone — critical for "today"/streak computation. */
    @Column(nullable = false)
    private String timezone = "Europe/Kyiv";

    @Column(name = "daily_capacity_minutes", nullable = false)
    private int dailyCapacityMinutes = 120;

    /** How many completed tasks/day count the streak. */
    @Column(name = "daily_task_goal", nullable = false)
    private int dailyTaskGoal = 1;

    @Column(name = "xp_total", nullable = false)
    private long xpTotal = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType plan = PlanType.FREE;

    /** BYOK: the user's own OpenAI-compatible provider. The key is stored encrypted. */
    @Column(name = "ai_base_url")
    private String aiBaseUrl;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "ai_api_key_enc")
    private String aiApiKeyEnc;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
