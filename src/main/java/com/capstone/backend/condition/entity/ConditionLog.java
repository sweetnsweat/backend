package com.capstone.backend.condition.entity;

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

    @Column(name = "sleep_score", nullable = false)
    private Integer sleepScore;

    @Column(name = "stress_score", nullable = false)
    private Integer stressScore;

    @Column(name = "fatigue_score", nullable = false)
    private Integer fatigueScore;

    @Column(name = "condition_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal conditionScore;

    @Column(name = "exercise_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal exerciseMultiplier;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

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
                                      Integer sleepScore,
                                      Integer stressScore,
                                      Integer fatigueScore,
                                      BigDecimal conditionScore,
                                      BigDecimal exerciseMultiplier,
                                      String memo) {
        ConditionLog conditionLog = new ConditionLog();
        conditionLog.user = user;
        conditionLog.logDate = logDate;
        conditionLog.updateScores(sleepScore, stressScore, fatigueScore, conditionScore, exerciseMultiplier, memo);
        return conditionLog;
    }

    public void updateScores(Integer sleepScore,
                             Integer stressScore,
                             Integer fatigueScore,
                             BigDecimal conditionScore,
                             BigDecimal exerciseMultiplier,
                             String memo) {
        this.sleepScore = sleepScore;
        this.stressScore = stressScore;
        this.fatigueScore = fatigueScore;
        this.conditionScore = conditionScore;
        this.exerciseMultiplier = exerciseMultiplier;
        this.memo = memo;
        this.legacyFatigue = fatigueScore;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getLogDate() {
        return logDate;
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

    public BigDecimal getConditionScore() {
        return conditionScore;
    }

    public BigDecimal getExerciseMultiplier() {
        return exerciseMultiplier;
    }

    public String getMemo() {
        return memo;
    }
}
