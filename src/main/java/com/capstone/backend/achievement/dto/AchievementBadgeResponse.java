package com.capstone.backend.achievement.dto;

import com.capstone.backend.shop.entity.Item;
import com.capstone.backend.shop.entity.UserItem;
import java.time.Instant;
import java.util.Map;

public record AchievementBadgeResponse(
        Long itemId,
        String badgeCode,
        String name,
        String description,
        String imageUrl,
        String criteria,
        Boolean earned,
        Instant earnedAt,
        Map<String, Object> metadata
) {
    public static AchievementBadgeResponse from(Item badge, UserItem userItem) {
        Map<String, Object> metadata = badge.getMetadata();
        return new AchievementBadgeResponse(
                badge.getId(),
                stringValue(metadata.get("badgeCode")),
                badge.getName(),
                badge.getDescription(),
                badge.getImageUrl(),
                stringValue(metadata.get("criteria")),
                userItem != null && userItem.getQuantity() > 0,
                userItem == null || userItem.getQuantity() <= 0 ? null : userItem.getUpdatedAt(),
                metadata
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
