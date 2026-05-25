package com.capstone.backend.battle.entity;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "battle_participants",
        uniqueConstraints = @UniqueConstraint(name = "battle_participants_battle_user_key", columnNames = {"battle_id", "user_id"})
)
public class BattleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "baseline_score", nullable = false)
    private Integer baselineScore;

    @Column(name = "baseline_completed_quest_count", nullable = false)
    private Integer baselineCompletedQuestCount;

    @Column(name = "baseline_routine_quest_count", nullable = false)
    private Integer baselineRoutineQuestCount;

    @Column(name = "baseline_exercise_minutes", nullable = false)
    private Integer baselineExerciseMinutes;

    @Column(name = "baseline_steps", nullable = false)
    private Integer baselineSteps;

    @Column(name = "baseline_distance_meters", nullable = false)
    private Integer baselineDistanceMeters;

    @Column(name = "baseline_active_calories", nullable = false)
    private Integer baselineActiveCalories;

    @Column(name = "baseline_health_verified_quest_count", nullable = false)
    private Integer baselineHealthVerifiedQuestCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private BattleResult result;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected BattleParticipant() {
    }

    public static BattleParticipant join(Battle battle, User user) {
        BattleParticipant participant = new BattleParticipant();
        participant.battle = battle;
        participant.user = user;
        participant.result = BattleResult.PENDING;
        return participant;
    }

    public void finalizeResult(int finalScore, BattleResult result) {
        this.finalScore = finalScore;
        this.result = result;
    }

    public void updateBaseline(int score,
                               int completedQuestCount,
                               int routineQuestCount,
                               int exerciseMinutes,
                               int steps,
                               int distanceMeters,
                               int activeCalories,
                               int healthVerifiedQuestCount) {
        this.baselineScore = Math.max(0, score);
        this.baselineCompletedQuestCount = Math.max(0, completedQuestCount);
        this.baselineRoutineQuestCount = Math.max(0, routineQuestCount);
        this.baselineExerciseMinutes = Math.max(0, exerciseMinutes);
        this.baselineSteps = Math.max(0, steps);
        this.baselineDistanceMeters = Math.max(0, distanceMeters);
        this.baselineActiveCalories = Math.max(0, activeCalories);
        this.baselineHealthVerifiedQuestCount = Math.max(0, healthVerifiedQuestCount);
    }

    @PrePersist
    void onCreate() {
        this.joinedAt = KoreanTime.nowInstant();
        if (this.result == null) {
            this.result = BattleResult.PENDING;
        }
        if (this.baselineScore == null) {
            this.baselineScore = 0;
        }
        if (this.baselineCompletedQuestCount == null) {
            this.baselineCompletedQuestCount = 0;
        }
        if (this.baselineRoutineQuestCount == null) {
            this.baselineRoutineQuestCount = 0;
        }
        if (this.baselineExerciseMinutes == null) {
            this.baselineExerciseMinutes = 0;
        }
        if (this.baselineSteps == null) {
            this.baselineSteps = 0;
        }
        if (this.baselineDistanceMeters == null) {
            this.baselineDistanceMeters = 0;
        }
        if (this.baselineActiveCalories == null) {
            this.baselineActiveCalories = 0;
        }
        if (this.baselineHealthVerifiedQuestCount == null) {
            this.baselineHealthVerifiedQuestCount = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public Battle getBattle() {
        return battle;
    }

    public User getUser() {
        return user;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public BattleResult getResult() {
        return result;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Integer getBaselineScore() {
        return baselineScore == null ? 0 : baselineScore;
    }

    public Integer getBaselineCompletedQuestCount() {
        return baselineCompletedQuestCount == null ? 0 : baselineCompletedQuestCount;
    }

    public Integer getBaselineRoutineQuestCount() {
        return baselineRoutineQuestCount == null ? 0 : baselineRoutineQuestCount;
    }

    public Integer getBaselineExerciseMinutes() {
        return baselineExerciseMinutes == null ? 0 : baselineExerciseMinutes;
    }

    public Integer getBaselineSteps() {
        return baselineSteps == null ? 0 : baselineSteps;
    }

    public Integer getBaselineDistanceMeters() {
        return baselineDistanceMeters == null ? 0 : baselineDistanceMeters;
    }

    public Integer getBaselineActiveCalories() {
        return baselineActiveCalories == null ? 0 : baselineActiveCalories;
    }

    public Integer getBaselineHealthVerifiedQuestCount() {
        return baselineHealthVerifiedQuestCount == null ? 0 : baselineHealthVerifiedQuestCount;
    }
}
