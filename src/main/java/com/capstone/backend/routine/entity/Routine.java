package com.capstone.backend.routine.entity;

import com.capstone.backend.user.entity.User;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

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

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OrderBy("seq ASC")
    @OneToMany(mappedBy = "routine")
    private List<RoutineItem> items = new ArrayList<>();

    protected Routine() {
    }

    public Long getId() {
        return id;
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

    public Boolean getActive() {
        return active;
    }

    public List<RoutineItem> getItems() {
        return items == null ? List.of() : List.copyOf(items);
    }
}
