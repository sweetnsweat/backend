package com.capstone.backend.health.entity;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "health_daily_summaries",
        uniqueConstraints = @UniqueConstraint(name = "health_daily_summaries_user_date_key", columnNames = {"user_id", "summary_date"})
)
public class HealthDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "steps", nullable = false)
    private Integer steps;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "active_calories_kcal", nullable = false)
    private Integer activeCaloriesKcal;

    @Column(name = "exercise_minutes", nullable = false)
    private Integer exerciseMinutes;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HealthDailySummary() {
    }

    public static HealthDailySummary create(User user, LocalDate summaryDate) {
        HealthDailySummary summary = new HealthDailySummary();
        summary.user = user;
        summary.summaryDate = summaryDate;
        summary.steps = 0;
        summary.distanceMeters = 0;
        summary.activeCaloriesKcal = 0;
        summary.exerciseMinutes = 0;
        summary.sampleCount = 0;
        summary.syncedAt = KoreanTime.nowInstant();
        return summary;
    }

    public void mergeTotals(int steps, int distanceMeters, int activeCaloriesKcal, int exerciseMinutes, int sampleCount) {
        this.steps = Math.max(this.steps == null ? 0 : this.steps, steps);
        this.distanceMeters = Math.max(this.distanceMeters == null ? 0 : this.distanceMeters, distanceMeters);
        this.activeCaloriesKcal = Math.max(this.activeCaloriesKcal == null ? 0 : this.activeCaloriesKcal, activeCaloriesKcal);
        this.exerciseMinutes = Math.max(this.exerciseMinutes == null ? 0 : this.exerciseMinutes, exerciseMinutes);
        this.sampleCount = Math.max(this.sampleCount == null ? 0 : this.sampleCount, sampleCount);
        this.syncedAt = KoreanTime.nowInstant();
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.syncedAt == null) {
            this.syncedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Integer getSteps() {
        return steps;
    }

    public LocalDate getSummaryDate() {
        return summaryDate;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public Integer getActiveCaloriesKcal() {
        return activeCaloriesKcal;
    }

    public Integer getExerciseMinutes() {
        return exerciseMinutes;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
