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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "user_exp_logs",
        uniqueConstraints = @UniqueConstraint(name = "user_exp_logs_user_ref_key", columnNames = {"user_id", "ref_type", "ref_id"})
)
public class UserExpLog {

    public static final String REF_TYPE_USER_QUEST = "user_quest";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "before_total_exp", nullable = false)
    private Integer beforeTotalExp;

    @Column(name = "after_total_exp", nullable = false)
    private Integer afterTotalExp;

    @Column(name = "before_level", nullable = false)
    private Integer beforeLevel;

    @Column(name = "after_level", nullable = false)
    private Integer afterLevel;

    @Column(name = "ref_type", nullable = false, length = 50)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserExpLog() {
    }

    public static UserExpLog create(User user,
                                    int amount,
                                    int beforeTotalExp,
                                    int afterTotalExp,
                                    int beforeLevel,
                                    int afterLevel,
                                    String refType,
                                    Long refId,
                                    String memo) {
        UserExpLog log = new UserExpLog();
        log.user = user;
        log.amount = amount;
        log.beforeTotalExp = beforeTotalExp;
        log.afterTotalExp = afterTotalExp;
        log.beforeLevel = beforeLevel;
        log.afterLevel = afterLevel;
        log.refType = refType;
        log.refId = refId;
        log.memo = memo;
        return log;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = KoreanTime.nowInstant();
    }
}
