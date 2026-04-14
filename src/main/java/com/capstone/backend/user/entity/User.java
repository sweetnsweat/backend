package com.capstone.backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", length = 50)
    private String loginId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "experience_level", length = 20)
    private String experienceLevel;

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
        return user;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "active";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public String getStatus() {
        return status;
    }
}
