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

@Entity
@Table(name = "character_profiles")
public class CharacterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "character_title", length = 255)
    private String characterTitle;

    @Column(name = "character_type", length = 100)
    private String characterType;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "mid_story_line", columnDefinition = "text")
    private String midStoryLine;

    @Column(name = "is_representative")
    private Boolean representative;

    protected CharacterProfile() {
    }

    public Integer getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getName() {
        return name;
    }

    public String getCharacterTitle() {
        return characterTitle;
    }

    public String getCharacterType() {
        return characterType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getMidStoryLine() {
        return midStoryLine;
    }

    public Boolean getRepresentative() {
        return representative;
    }
}
