package com.capstone.backend.routine.entity;

import com.capstone.backend.user.entity.User;
import com.capstone.backend.global.time.KoreanTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_routine_id")
    private Routine sourceRoutine;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultRoutine;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "target_experience_level", length = 20)
    private String targetExperienceLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_current_exercise_statuses")
    private List<String> targetCurrentExerciseStatuses = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "goal_types")
    private List<String> goalTypes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "place_types")
    private List<String> placeTypes = new ArrayList<>();

    @Column(name = "weekly_frequency")
    private Integer weeklyFrequency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommended_exercise_types")
    private List<String> recommendedExerciseTypes = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OrderBy("seq ASC")
    @OneToMany(mappedBy = "routine")
    private List<RoutineItem> items = new ArrayList<>();

    @OrderBy("seq ASC")
    @OneToMany(mappedBy = "routine")
    private List<RoutineSession> sessions = new ArrayList<>();

    protected Routine() {
    }

    public static Routine copyFromDefault(Routine sourceRoutine, User user) {
        Routine routine = new Routine();
        routine.user = user;
        routine.sourceRoutine = sourceRoutine;
        routine.name = sourceRoutine.getName();
        routine.description = sourceRoutine.getDescription();
        routine.defaultRoutine = false;
        routine.difficulty = sourceRoutine.getDifficulty();
        routine.estimatedMinutes = sourceRoutine.getEstimatedMinutes();
        routine.targetExperienceLevel = sourceRoutine.getTargetExperienceLevel();
        routine.targetCurrentExerciseStatuses = new ArrayList<>(sourceRoutine.getTargetCurrentExerciseStatuses());
        routine.goalTypes = new ArrayList<>(sourceRoutine.getGoalTypes());
        routine.placeTypes = new ArrayList<>(sourceRoutine.getPlaceTypes());
        routine.weeklyFrequency = sourceRoutine.getWeeklyFrequency();
        routine.recommendedExerciseTypes = new ArrayList<>(sourceRoutine.getRecommendedExerciseTypes());
        routine.active = true;
        return routine;
    }

    public static Routine createCustom(User user, String name, String description, Integer estimatedMinutes) {
        Routine routine = new Routine();
        routine.user = user;
        routine.name = name;
        routine.description = description;
        routine.defaultRoutine = false;
        routine.difficulty = "custom";
        routine.estimatedMinutes = estimatedMinutes;
        routine.active = true;
        return routine;
    }

    public void updateCustom(String name, String description, Integer estimatedMinutes) {
        this.name = name;
        this.description = description;
        this.estimatedMinutes = estimatedMinutes;
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        applyDefaults();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
        applyDefaults();
    }

    private void applyDefaults() {
        if (this.defaultRoutine == null) {
            this.defaultRoutine = false;
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.targetCurrentExerciseStatuses == null) {
            this.targetCurrentExerciseStatuses = new ArrayList<>();
        }
        if (this.goalTypes == null) {
            this.goalTypes = new ArrayList<>();
        }
        if (this.placeTypes == null) {
            this.placeTypes = new ArrayList<>();
        }
        if (this.recommendedExerciseTypes == null) {
            this.recommendedExerciseTypes = new ArrayList<>();
        }
    }

    void addSession(RoutineSession session) {
        if (this.sessions == null) {
            this.sessions = new ArrayList<>();
        }
        this.sessions.add(session);
    }

    void addItem(RoutineItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Routine getSourceRoutine() {
        return sourceRoutine;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getDefaultRoutine() {
        return defaultRoutine;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public String getTargetExperienceLevel() {
        return targetExperienceLevel;
    }

    public List<String> getTargetCurrentExerciseStatuses() {
        return targetCurrentExerciseStatuses == null ? List.of() : List.copyOf(targetCurrentExerciseStatuses);
    }

    public List<String> getGoalTypes() {
        return goalTypes == null ? List.of() : List.copyOf(goalTypes);
    }

    public List<String> getPlaceTypes() {
        return placeTypes == null ? List.of() : List.copyOf(placeTypes);
    }

    public Integer getWeeklyFrequency() {
        return weeklyFrequency;
    }

    public List<String> getRecommendedExerciseTypes() {
        return recommendedExerciseTypes == null ? List.of() : List.copyOf(recommendedExerciseTypes);
    }

    public Boolean getActive() {
        return active;
    }

    public List<RoutineItem> getItems() {
        return items == null ? List.of() : List.copyOf(items);
    }

    public List<RoutineSession> getSessions() {
        return sessions == null ? List.of() : List.copyOf(sessions);
    }
}
