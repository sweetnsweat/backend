package com.capstone.backend.shop.dto;

import com.capstone.backend.quest.dto.QuestResponse;
import com.capstone.backend.shop.entity.UserItem;
import com.capstone.backend.shop.entity.UserItemEffect;
import java.time.Instant;

public record ShopItemUseResponse(
        PurchasedItemResponse item,
        String effectType,
        String status,
        String message,
        Instant expiresAt,
        QuestResponse quest
) {
    public static ShopItemUseResponse activated(UserItem userItem, UserItemEffect effect, String message) {
        return new ShopItemUseResponse(
                PurchasedItemResponse.from(userItem),
                effect.getEffectType(),
                "ACTIVE",
                message,
                effect.getExpiresAt(),
                null
        );
    }

    public static ShopItemUseResponse questSkipped(UserItem userItem, QuestResponse quest, String message) {
        return new ShopItemUseResponse(
                PurchasedItemResponse.from(userItem),
                "QUEST_SKIP",
                "USED",
                message,
                null,
                quest
        );
    }
}
