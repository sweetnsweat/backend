package com.capstone.backend.shop.entity;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "user_items",
        uniqueConstraints = @UniqueConstraint(name = "user_items_user_id_item_id_key", columnNames = {"user_id", "item_id"})
)
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserItem() {
    }

    public static UserItem create(User user, Item item, int quantity) {
        UserItem userItem = new UserItem();
        userItem.user = user;
        userItem.item = item;
        userItem.quantity = Math.max(0, quantity);
        return userItem;
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            return;
        }
        this.quantity = getQuantity() + amount;
    }

    @PrePersist
    @PreUpdate
    void onUpdate() {
        if (this.quantity == null) {
            this.quantity = 0;
        }
        this.updatedAt = KoreanTime.nowInstant();
    }

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public Integer getQuantity() {
        return quantity == null ? 0 : quantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
