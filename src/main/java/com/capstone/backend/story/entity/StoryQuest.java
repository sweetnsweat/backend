package com.capstone.backend.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "story_quests")
public class StoryQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_key", nullable = false, length = 100)
    private String userKey;

    @Column(name = "scenario_id", nullable = false)
    private Integer scenarioId;

    @Column(name = "chapter_num", nullable = false)
    private Integer chapterNum;

    @Column(name = "unit_index", nullable = false)
    private Integer unitIndex;

    @Column(name = "quest_date", nullable = false, length = 10)
    private String questDate;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "quest_json", nullable = false, columnDefinition = "text")
    private String questJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected StoryQuest() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getScenarioId() {
        return scenarioId;
    }

    public Integer getChapterNum() {
        return chapterNum;
    }

    public Integer getUnitIndex() {
        return unitIndex;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getQuestJson() {
        return questJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
