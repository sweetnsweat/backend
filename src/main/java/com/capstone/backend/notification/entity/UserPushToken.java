package com.capstone.backend.notification.entity;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_push_tokens")
public class UserPushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "text")
    private String token;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPushToken() {
    }

    public static UserPushToken register(User user, String token, String platform, String deviceId) {
        UserPushToken pushToken = new UserPushToken();
        pushToken.token = token;
        pushToken.updateRegistration(user, platform, deviceId);
        return pushToken;
    }

    public void updateRegistration(User user, String platform, String deviceId) {
        this.user = user;
        this.platform = platform;
        this.deviceId = deviceId;
        this.enabled = true;
        this.lastSeenAt = KoreanTime.nowInstant();
    }

    public void disable() {
        this.enabled = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastSeenAt == null) {
            this.lastSeenAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
