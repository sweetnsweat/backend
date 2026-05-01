package com.capstone.backend.exercise.entity;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.routine.entity.Exercise;
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
        name = "user_favorite_exercises",
        uniqueConstraints = @UniqueConstraint(
                name = "user_favorite_exercises_user_id_exercise_id_key",
                columnNames = {"user_id", "exercise_id"}
        )
)
public class UserFavoriteExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserFavoriteExercise() {
    }

    public static UserFavoriteExercise create(User user, Exercise exercise) {
        UserFavoriteExercise favorite = new UserFavoriteExercise();
        favorite.user = user;
        favorite.exercise = exercise;
        return favorite;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
