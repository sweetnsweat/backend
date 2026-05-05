package com.capstone.backend.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "story_play_logs")
public class StoryPlayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_key", nullable = false, length = 100)
    private String userKey;

    @Column(name = "scenario_id", nullable = false)
    private Integer scenarioId;

    @Column(name = "chapter_num", nullable = false)
    private Integer chapterNum;

    @Column(name = "choice_id")
    private Integer choiceId;

    @Column(name = "detail_id")
    private Integer detailId;

    @Column(name = "unit_index", nullable = false)
    private Integer unitIndex;

    @Column(name = "user_message", columnDefinition = "text")
    private String userMessage;

    @Column(name = "narration_text", columnDefinition = "text")
    private String narrationText;

    @Column(name = "dialogue_text", columnDefinition = "text")
    private String dialogueText;

    @Column(name = "output_text", nullable = false, columnDefinition = "text")
    private String outputText;

    @Column(name = "created_at")
    private Instant createdAt;

    protected StoryPlayLog() {
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

    public Integer getChoiceId() {
        return choiceId;
    }

    public Integer getDetailId() {
        return detailId;
    }

    public Integer getUnitIndex() {
        return unitIndex;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getNarrationText() {
        return narrationText;
    }

    public String getDialogueText() {
        return dialogueText;
    }

    public String getOutputText() {
        return outputText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
