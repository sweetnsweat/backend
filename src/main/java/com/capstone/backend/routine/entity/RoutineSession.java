package com.capstone.backend.routine.entity;

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

@Entity
@Table(name = "routine_sessions")
public class RoutineSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek;

    @Column(name = "session_name", nullable = false, length = 100)
    private String sessionName;

    @Column(name = "session_type", length = 30)
    private String sessionType;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OrderBy("seq ASC")
    @OneToMany(mappedBy = "routineSession")
    private List<RoutineItem> items = new ArrayList<>();

    protected RoutineSession() {
    }

    public static RoutineSession copyForRoutine(Routine routine, RoutineSession sourceSession) {
        RoutineSession session = new RoutineSession();
        session.routine = routine;
        session.dayOfWeek = sourceSession.getDayOfWeek();
        session.sessionName = sourceSession.getSessionName();
        session.sessionType = sourceSession.getSessionType();
        session.seq = sourceSession.getSeq() == null ? 1 : sourceSession.getSeq();
        session.estimatedMinutes = sourceSession.getEstimatedMinutes();
        session.active = sourceSession.getActive() == null ? true : sourceSession.getActive();
        routine.addSession(session);
        return session;
    }

    public static RoutineSession create(Routine routine,
                                        String dayOfWeek,
                                        String sessionName,
                                        String sessionType,
                                        Integer seq,
                                        Integer estimatedMinutes) {
        RoutineSession session = new RoutineSession();
        session.routine = routine;
        session.dayOfWeek = dayOfWeek;
        session.sessionName = sessionName;
        session.sessionType = sessionType;
        session.seq = seq == null ? 1 : seq;
        session.estimatedMinutes = estimatedMinutes;
        session.active = true;
        routine.addSession(session);
        return session;
    }

    public void configureForExhibitionDemo(String dayOfWeek, String sessionName, String sessionType, Integer estimatedMinutes) {
        this.dayOfWeek = dayOfWeek;
        this.sessionName = sessionName;
        this.sessionType = sessionType;
        this.estimatedMinutes = estimatedMinutes;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = KoreanTime.nowInstant();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KoreanTime.nowInstant();
        if (this.active == null) {
            this.active = true;
        }
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

    public Routine getRoutine() {
        return routine;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getSessionType() {
        return sessionType;
    }

    public Integer getSeq() {
        return seq;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public Boolean getActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<RoutineItem> getItems() {
        return items == null ? List.of() : List.copyOf(items);
    }
}
