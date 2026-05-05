package com.capstone.backend.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "story_progress")
public class StoryProgress {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_key", nullable = false, length = 100)
    private String userKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "current_chapter_num")
    private Integer currentChapterNum;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "last_output", columnDefinition = "text")
    private String lastOutput;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected StoryProgress() {
    }

    public Integer getId() {
        return id;
    }

    public String getUserKey() {
        return userKey;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCurrentChapterNum() {
        return currentChapterNum;
    }

    public String getPhase() {
        return phase;
    }

    public String getLastOutput() {
        return lastOutput;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
