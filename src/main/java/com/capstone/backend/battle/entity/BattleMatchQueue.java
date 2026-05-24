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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "battle_match_queue")
public class BattleMatchQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private BattleMode mode;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BattleMatchQueueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_battle_id")
    private Battle matchedBattle;

    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Column(name = "matched_at")
    private Instant matchedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BattleMatchQueue() {
    }

    public static BattleMatchQueue waiting(User user,
                                           BattleMode mode,
                                           LocalDate periodStartDate,
                                           LocalDate periodEndDate,
                                           Instant expiresAt) {
        BattleMatchQueue queue = new BattleMatchQueue();
        queue.user = user;
        queue.mode = mode;
        queue.periodStartDate = periodStartDate;
        queue.periodEndDate = periodEndDate;
        queue.status = BattleMatchQueueStatus.WAITING;
        queue.expiresAt = expiresAt;
        return queue;
    }

    public void match(Battle battle, Instant matchedAt) {
        this.status = BattleMatchQueueStatus.MATCHED;
        this.matchedBattle = battle;
        this.matchedAt = matchedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.queuedAt == null) {
            this.queuedAt = now;
        }
        if (this.status == null) {
            this.status = BattleMatchQueueStatus.WAITING;
        }
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

    public BattleMode getMode() {
        return mode;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public BattleMatchQueueStatus getStatus() {
        return status;
    }

    public Battle getMatchedBattle() {
        return matchedBattle;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
