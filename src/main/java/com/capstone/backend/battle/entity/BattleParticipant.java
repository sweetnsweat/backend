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

    @PrePersist
    void onCreate() {
        this.joinedAt = KoreanTime.nowInstant();
        if (this.result == null) {
            this.result = BattleResult.PENDING;
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
}
