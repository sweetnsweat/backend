package com.capstone.backend.battle.entity;

import com.capstone.backend.global.time.KoreanTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "battles")
public class Battle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private BattleMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BattleStatus status;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Battle() {
    }

    public static Battle create(BattleMode mode,
                                LocalDate periodStartDate,
                                LocalDate periodEndDate,
                                Instant startsAt,
                                Instant endsAt) {
        Battle battle = new Battle();
        battle.mode = mode;
        battle.status = BattleStatus.ACTIVE;
        battle.periodStartDate = periodStartDate;
        battle.periodEndDate = periodEndDate;
        battle.startsAt = startsAt;
        battle.endsAt = endsAt;
        return battle;
    }

    public void finalizeBattle(Instant finalizedAt) {
        this.status = BattleStatus.FINALIZED;
        this.finalizedAt = finalizedAt;
    }

    public boolean isFinalized() {
        return BattleStatus.FINALIZED.equals(status);
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = BattleStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public BattleMode getMode() {
        return mode;
    }

    public BattleStatus getStatus() {
        return status;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }
}
