package com.capstone.backend.routine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "routine_items")
public class RoutineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_session_id")
    private RoutineSession routineSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "sets")
    private Integer sets;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "rest_sec")
    private Integer restSec;

    protected RoutineItem() {
    }

    public static RoutineItem copyForRoutine(Routine routine, RoutineSession routineSession, RoutineItem sourceItem) {
        RoutineItem item = new RoutineItem();
        item.routine = routine;
        item.routineSession = routineSession;
        item.exercise = sourceItem.getExercise();
        item.seq = sourceItem.getSeq();
        item.reps = sourceItem.getReps();
        item.sets = sourceItem.getSets();
        item.durationSec = sourceItem.getDurationSec();
        item.restSec = sourceItem.getRestSec();
        routine.addItem(item);
        if (routineSession != null) {
            routineSession.addItem(item);
        }
        return item;
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public RoutineSession getRoutineSession() {
        return routineSession;
    }

    public Integer getSeq() {
        return seq;
    }

    public Integer getReps() {
        return reps;
    }

    public Integer getSets() {
        return sets;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public Integer getRestSec() {
        return restSec;
    }
}
