package com.capstone.backend.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "world_image_url", columnDefinition = "text")
    private String worldImageUrl;

    @Column(name = "player_image_url", columnDefinition = "text")
    private String playerImageUrl;

    @Column(name = "player_description", columnDefinition = "text")
    private String playerDescription;

    @Column(name = "is_active")
    private Boolean active;

    protected Scenario() {
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getGenre() {
        return genre;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getWorldImageUrl() {
        return worldImageUrl;
    }

    public String getPlayerImageUrl() {
        return playerImageUrl;
    }

    public String getPlayerDescription() {
        return playerDescription;
    }

    public Boolean getActive() {
        return active;
    }
}
