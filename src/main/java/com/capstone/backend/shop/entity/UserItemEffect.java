package com.capstone.backend.shop.entity;

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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_item_effects")
public class UserItemEffect {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_USED = "USED";

    public static final String EFFECT_EXP_BOOST = "EXP_BOOST";
    public static final String EFFECT_RECORD_SHIELD = "RECORD_SHIELD";
    public static final String EFFECT_WIN_RATE_SHIELD = "WIN_RATE_SHIELD";
    public static final String EFFECT_BATTLE_RETRY = "BATTLE_RETRY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "effect_type", nullable = false, length = 50)
    private String effectType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserItemEffect() {
    }

    public static UserItemEffect active(User user,
                                        Item item,
                                        String effectType,
                                        Instant startsAt,
                                        Instant expiresAt,
                                        Map<String, Object> metadata) {
        UserItemEffect effect = new UserItemEffect();
        effect.user = user;
        effect.item = item;
        effect.effectType = effectType;
        effect.status = STATUS_ACTIVE;
        effect.startsAt = startsAt;
        effect.expiresAt = expiresAt;
        effect.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        return effect;
    }

    public void markUsed(String refType, Long refId, Instant usedAt) {
        this.status = STATUS_USED;
        this.refType = refType;
        this.refId = refId;
        this.usedAt = usedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = STATUS_ACTIVE;
        }
        if (this.startsAt == null) {
            this.startsAt = now;
        }
        if (this.metadata == null) {
            this.metadata = new LinkedHashMap<>();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public String getEffectType() {
        return effectType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
