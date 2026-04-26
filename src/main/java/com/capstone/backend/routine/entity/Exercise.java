package com.capstone.backend.routine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "intensity", length = 50)
    private String intensity;

    @Column(name = "met")
    private BigDecimal met;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "force", length = 50)
    private String force;

    @Column(name = "mechanic", length = 50)
    private String mechanic;

    @Column(name = "equipment", length = 100)
    private String equipment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "primary_muscles", nullable = false)
    private List<String> primaryMuscles = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "secondary_muscles", nullable = false)
    private List<String> secondaryMuscles = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instructions", nullable = false)
    private List<String> instructions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", nullable = false)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "source_license", length = 100)
    private String sourceLicense;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false)
    private Map<String, Object> rawData = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Exercise() {
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getIntensity() {
        return intensity;
    }

    public BigDecimal getMet() {
        return met;
    }

    public String getLevel() {
        return level;
    }

    public String getForce() {
        return force;
    }

    public String getMechanic() {
        return mechanic;
    }

    public String getEquipment() {
        return equipment;
    }

    public List<String> getPrimaryMuscles() {
        return primaryMuscles == null ? List.of() : List.copyOf(primaryMuscles);
    }

    public List<String> getSecondaryMuscles() {
        return secondaryMuscles == null ? List.of() : List.copyOf(secondaryMuscles);
    }

    public List<String> getInstructions() {
        return instructions == null ? List.of() : List.copyOf(instructions);
    }

    public List<String> getImageUrls() {
        return imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    public String getSource() {
        return source;
    }

    public String getSourceLicense() {
        return sourceLicense;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}
