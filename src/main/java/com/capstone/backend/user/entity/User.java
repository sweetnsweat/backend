package com.capstone.backend.user.entity;

import com.capstone.backend.routine.entity.Routine;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "login_id", length = 50)
    private String loginId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "profile_image_url", columnDefinition = "text")
    private String profileImageUrl;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_routine_id")
    private Routine activeRoutine;

    @Column(name = "experience_level", length = 20)
    private String experienceLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_exercise_types", nullable = false)
    private List<String> preferredExerciseTypes = new ArrayList<>();

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled;

    @Column(name = "push_quest_enabled", nullable = false)
    private Boolean pushQuestEnabled;

    @Column(name = "push_routine_enabled", nullable = false)
    private Boolean pushRoutineEnabled;

    @Column(name = "push_competition_enabled", nullable = false)
    private Boolean pushCompetitionEnabled;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public static User createLocalUser(String loginId, String passwordHash, String nickname) {
        User user = new User();
        user.loginId = loginId;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.status = "active";
        user.preferredExerciseTypes = new ArrayList<>();
        user.pushEnabled = true;
        user.pushQuestEnabled = true;
        user.pushRoutineEnabled = true;
        user.pushCompetitionEnabled = true;
        return user;
    }

    public void updateExerciseProfile(String experienceLevel, List<String> preferredExerciseTypes) {
        this.experienceLevel = experienceLevel;
        this.preferredExerciseTypes = preferredExerciseTypes == null
                ? new ArrayList<>()
                : new ArrayList<>(preferredExerciseTypes);
    }

    public void updateOnboardingProfile(String gender,
                                        LocalDate birthDate,
                                        BigDecimal heightCm,
                                        BigDecimal weightKg,
                                        String experienceLevel,
                                        List<String> preferredExerciseTypes) {
        this.gender = gender;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        updateExerciseProfile(experienceLevel, preferredExerciseTypes);
    }

    public void updateActiveRoutine(Routine activeRoutine) {
        this.activeRoutine = activeRoutine;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "active";
        }
        applyDefaults();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
        applyDefaults();
    }

    private void applyDefaults() {
        if (this.preferredExerciseTypes == null) {
            this.preferredExerciseTypes = new ArrayList<>();
        }
        if (this.pushEnabled == null) {
            this.pushEnabled = true;
        }
        if (this.pushQuestEnabled == null) {
            this.pushQuestEnabled = true;
        }
        if (this.pushRoutineEnabled == null) {
            this.pushRoutineEnabled = true;
        }
        if (this.pushCompetitionEnabled == null) {
            this.pushCompetitionEnabled = true;
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public Routine getActiveRoutine() {
        return activeRoutine;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public List<String> getPreferredExerciseTypes() {
        return preferredExerciseTypes == null ? List.of() : List.copyOf(preferredExerciseTypes);
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public Boolean getPushQuestEnabled() {
        return pushQuestEnabled;
    }

    public Boolean getPushRoutineEnabled() {
        return pushRoutineEnabled;
    }

    public Boolean getPushCompetitionEnabled() {
        return pushCompetitionEnabled;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
