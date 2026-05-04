package com.capstone.backend.reward.entity;

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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    public static final String TX_TYPE_QUEST_REWARD = "quest_reward";
    public static final String REF_TYPE_USER_QUEST = "user_quest";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tx_type", nullable = false, length = 50)
    private String txType;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletTransaction() {
    }

    public static WalletTransaction questReward(User user, int amount, Long questId, String memo) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.user = user;
        transaction.txType = TX_TYPE_QUEST_REWARD;
        transaction.amount = amount;
        transaction.refType = REF_TYPE_USER_QUEST;
        transaction.refId = questId;
        transaction.memo = memo;
        return transaction;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = KoreanTime.nowInstant();
    }
}
