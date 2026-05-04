package com.capstone.backend.quest.entity;

import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineSession;
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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "user_quests",
        uniqueConstraints = @UniqueConstraint(name = "user_quests_user_id_quest_date_key", columnNames = {"user_id", "quest_date"})
)
public class UserQuest {

    public static final String STATUS_ASSIGNED = "assigned";
    public static final String STATUS_ISSUED = "issued";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_EXPIRED = "expired";

    public static final String TYPE_ROUTINE = "routine";
    public static final String TYPE_OFF_DAY = "off_day";
    public static final String TYPE_RECOVERY = "recovery";

    public static final String METRIC_ROUTINE = "routine";
    public static final String METRIC_MINUTES = "minutes";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "template_id")
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_session_id")
    private RoutineSession sourceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_log_id")
    private ConditionLog conditionLog;

    @Column(name = "quest_date", nullable = false)
    private LocalDate questDate;

    @Column(name = "quest_type", nullable = false, length = 30)
    private String questType;

    @Column(name = "target_metric", nullable = false, length = 30)
    private String targetMetric;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "target_value", nullable = false)
    private Integer targetValue;

    @Column(name = "progress_value", nullable = false)
    private Integer progressValue;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "condition_adjusted", nullable = false)
    private Boolean conditionAdjusted;

    @Column(name = "reward_currency", nullable = false)
    private Integer rewardCurrency;

    @Column(name = "reward_exp", nullable = false)
    private Integer rewardExp;

    @Column(name = "reward_item_id")
    private Long rewardItemId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proof_json", nullable = false)
    private Map<String, Object> proofJson = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quest_context_json", nullable = false)
    private Map<String, Object> questContextJson = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserQuest() {
    }

    public static UserQuest create(User user,
                                   Routine routine,
                                   RoutineSession sourceSession,
                                   ConditionLog conditionLog,
                                   LocalDate questDate,
                                   String questType,
                                   String targetMetric,
                                   String title,
                                   String description,
                                   Integer targetValue,
                                   boolean conditionAdjusted,
                                   Map<String, Object> questContextJson) {
        return create(
                user,
                routine,
                sourceSession,
                conditionLog,
                questDate,
                questType,
                targetMetric,
                title,
                description,
                targetValue,
                conditionAdjusted,
                rewardCurrencyFor(questType),
                rewardExpFor(questType),
                questContextJson
        );
    }

    public static UserQuest create(User user,
                                   Routine routine,
                                   RoutineSession sourceSession,
                                   ConditionLog conditionLog,
                                   LocalDate questDate,
                                   String questType,
                                   String targetMetric,
                                   String title,
                                   String description,
                                   Integer targetValue,
                                   boolean conditionAdjusted,
                                   Integer rewardCurrency,
                                   Integer rewardExp,
                                   Map<String, Object> questContextJson) {
        UserQuest quest = new UserQuest();
        quest.user = user;
        quest.routine = routine;
        quest.sourceSession = sourceSession;
        quest.conditionLog = conditionLog;
        quest.questDate = questDate;
        quest.questType = questType;
        quest.targetMetric = targetMetric;
        quest.title = title;
        quest.description = description;
        quest.targetValue = targetValue;
        quest.progressValue = 0;
        quest.status = STATUS_ISSUED;
        quest.conditionAdjusted = conditionAdjusted;
        quest.rewardCurrency = rewardCurrency == null ? 0 : rewardCurrency;
        quest.rewardExp = rewardExp == null ? 0 : rewardExp;
        quest.proofJson = new LinkedHashMap<>();
        quest.questContextJson = questContextJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(questContextJson);
        return quest;
    }

    private static int rewardCurrencyFor(String questType) {
        return TYPE_ROUTINE.equals(questType) ? 30 : 15;
    }

    private static int rewardExpFor(String questType) {
        return TYPE_ROUTINE.equals(questType) ? 20 : 10;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = KoreanTime.nowInstant();
        if (this.progressValue == null) {
            this.progressValue = 0;
        }
        if (this.status == null) {
            this.status = STATUS_ASSIGNED;
        }
        if (this.conditionAdjusted == null) {
            this.conditionAdjusted = false;
        }
        if (this.rewardCurrency == null) {
            this.rewardCurrency = 0;
        }
        if (this.rewardExp == null) {
            this.rewardExp = 0;
        }
        if (this.proofJson == null) {
            this.proofJson = new LinkedHashMap<>();
        }
        if (this.questContextJson == null) {
            this.questContextJson = new LinkedHashMap<>();
        }
    }

    public void markIssued() {
        if (STATUS_ASSIGNED.equals(this.status)) {
            this.status = STATUS_ISSUED;
        }
    }

    public void complete(Integer progressValue, Map<String, Object> proofJson) {
        this.progressValue = progressValue == null ? this.targetValue : Math.max(progressValue, this.targetValue);
        this.status = STATUS_COMPLETED;
        this.completedAt = KoreanTime.nowInstant();
        this.proofJson = proofJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(proofJson);
    }

    public void expire() {
        if (!STATUS_COMPLETED.equals(this.status)) {
            this.status = STATUS_EXPIRED;
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Routine getRoutine() {
        return routine;
    }

    public RoutineSession getSourceSession() {
        return sourceSession;
    }

    public ConditionLog getConditionLog() {
        return conditionLog;
    }

    public LocalDate getQuestDate() {
        return questDate;
    }

    public String getQuestType() {
        return questType;
    }

    public String getTargetMetric() {
        return targetMetric;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public Integer getProgressValue() {
        return progressValue;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getConditionAdjusted() {
        return conditionAdjusted;
    }

    public Integer getRewardCurrency() {
        return rewardCurrency;
    }

    public Integer getRewardExp() {
        return rewardExp;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Map<String, Object> getQuestContextJson() {
        return questContextJson == null ? Map.of() : Map.copyOf(questContextJson);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
