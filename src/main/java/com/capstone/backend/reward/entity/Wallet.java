package com.capstone.backend.reward.entity;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance_currency", nullable = false)
    private Integer balanceCurrency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
    }

    public static Wallet create(User user) {
        Wallet wallet = new Wallet();
        wallet.user = user;
        wallet.balanceCurrency = 0;
        return wallet;
    }

    public void credit(int amount) {
        if (amount <= 0) {
            return;
        }
        this.balanceCurrency = getBalanceCurrency() + amount;
    }

    public void debit(int amount) {
        if (amount <= 0) {
            return;
        }
        this.balanceCurrency = getBalanceCurrency() - amount;
    }

    @PrePersist
    @PreUpdate
    void onUpdate() {
        if (this.balanceCurrency == null) {
            this.balanceCurrency = 0;
        }
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getBalanceCurrency() {
        return balanceCurrency == null ? 0 : balanceCurrency;
    }
}
