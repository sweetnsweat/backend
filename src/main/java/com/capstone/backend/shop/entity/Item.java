package com.capstone.backend.shop.entity;

import com.capstone.backend.global.time.KoreanTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price_currency", nullable = false)
    private Integer priceCurrency;

    @Column(name = "is_sellable", nullable = false)
    private Boolean sellable;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Item() {
    }

    @PrePersist
    void onCreate() {
        if (this.priceCurrency == null) {
            this.priceCurrency = 0;
        }
        if (this.sellable == null) {
            this.sellable = true;
        }
        if (this.metadata == null) {
            this.metadata = new LinkedHashMap<>();
        }
        if (this.active == null) {
            this.active = true;
        }
        this.createdAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public String getItemType() {
        return itemType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPriceCurrency() {
        return priceCurrency == null ? 0 : priceCurrency;
    }

    public Boolean getSellable() {
        return sellable;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata == null ? Map.of() : new LinkedHashMap<>(metadata);
    }

    public Boolean getActive() {
        return active;
    }
}
