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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "scenario_genres",
        uniqueConstraints = @UniqueConstraint(name = "scenario_genres_scenario_name_key", columnNames = {"scenario_id", "genre_name"})
)
public class ScenarioGenre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "genre_name", nullable = false, length = 100)
    private String genreName;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    protected ScenarioGenre() {
    }

    public Long getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getGenreName() {
        return genreName;
    }

    public Integer getSeq() {
        return seq;
    }
}
