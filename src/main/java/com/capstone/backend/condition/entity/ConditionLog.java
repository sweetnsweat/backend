package com.capstone.backend.condition.entity;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "condition_logs",
        uniqueConstraints = @UniqueConstraint(name = "condition_logs_user_id_log_date_key", columnNames = {"user_id", "log_date"})
)
public class ConditionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "condition_level")
    private Integer conditionLevel;

    @Column(name = "sleep_score", nullable = false)
    private Integer sleepScore;

    @Column(name = "stress_score", nullable = false)
    private Integer stressScore;

    @Column(name = "fatigue_score", nullable = false)
    private Integer fatigueScore;

    @Column(name = "energy_level")
    private Integer energyLevel;

    @Column(name = "condition_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal conditionScore;

    @Column(name = "exercise_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal exerciseMultiplier;

    @Column(name = "fatigue", nullable = false)
    private Integer legacyFatigue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConditionLog() {
    }

    public static ConditionLog create(User user,
                                      LocalDate logDate,
                                      Integer conditionLevel,
                                      Integer sleepScore,
                                      Integer stressScore,
                                      Integer fatigueScore,
                                      Integer energyLevel,
                                      BigDecimal conditionScore,
                                      BigDecimal exerciseMultiplier) {
        ConditionLog conditionLog = new ConditionLog();
        conditionLog.user = user;
        conditionLog.logDate = logDate;
        conditionLog.updateScores(conditionLevel, sleepScore, stressScore, fatigueScore, energyLevel, conditionScore, exerciseMultiplier);
        return conditionLog;
    }

    public void updateScores(Integer conditionLevel,
                             Integer sleepScore,
                             Integer stressScore,
                             Integer fatigueScore,
                             Integer energyLevel,
                             BigDecimal conditionScore,
                             BigDecimal exerciseMultiplier) {
        this.conditionLevel = conditionLevel;
        this.sleepScore = sleepScore;
        this.stressScore = stressScore;
        this.fatigueScore = fatigueScore;
        this.energyLevel = energyLevel;
        this.conditionScore = conditionScore;
        this.exerciseMultiplier = exerciseMultiplier;
        this.legacyFatigue = fatigueScore;
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public Integer getConditionLevel() {
        return conditionLevel;
    }

    public Integer getSleepScore() {
        return sleepScore;
    }

    public Integer getStressScore() {
        return stressScore;
    }

    public Integer getFatigueScore() {
        return fatigueScore;
    }

    public Integer getEnergyLevel() {
        return energyLevel;
    }

    public BigDecimal getConditionScore() {
        return conditionScore;
    }

    public BigDecimal getExerciseMultiplier() {
        return exerciseMultiplier;
    }
}
