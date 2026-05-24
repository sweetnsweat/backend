package com.capstone.backend.shop.dto;

import com.capstone.backend.achievement.service.AchievementBadgeService;
import com.capstone.backend.shop.entity.Item;
import java.util.Map;

public record ShopItemResponse(
        Long id,
        String itemType,
        String itemTypeLabel,
        String category,
        String categoryLabel,
        String name,
        String description,
        Integer priceCurrency,
        Boolean sellable,
        Boolean owned,
        Integer ownedQuantity,
        Boolean purchasable,
        Boolean equipped,
        Boolean special,
        String effect,
        String imageUrl,
        Map<String, Object> metadata
) {
    public static ShopItemResponse from(Item item, int ownedQuantity, int balanceCurrency, String equippedProfileImageUrl) {
        boolean sellable = Boolean.TRUE.equals(item.getSellable());
        Map<String, Object> metadata = item.getMetadata();
        return new ShopItemResponse(
                item.getId(),
                item.getItemType(),
                itemTypeLabel(item.getItemType()),
                category(item),
                categoryLabel(category(item)),
                item.getName(),
                item.getDescription(),
                item.getPriceCurrency(),
                sellable,
                ownedQuantity > 0,
                ownedQuantity,
                sellable && balanceCurrency >= item.getPriceCurrency(),
                ownedQuantity > 0 && item.getImageUrl() != null && item.getImageUrl().equals(equippedProfileImageUrl),
                Boolean.TRUE.equals(metadata.get("special")),
                stringValue(metadata.get("effect")),
                item.getImageUrl(),
                metadata
        );
    }

    private static String category(Item item) {
        Map<String, Object> metadata = item.getMetadata();
        if (AchievementBadgeService.KIND_ACHIEVEMENT_BADGE.equals(stringValue(metadata.get("kind")))) {
            return "badge";
        }
        String itemType = item.getItemType();
        return switch (itemType == null ? "" : itemType) {
            case "skin", "profile" -> "character";
            case "ticket", "pvp_badge", "gift", "consumable" -> "pass";
            default -> itemType;
        };
    }

    private static String itemTypeLabel(String itemType) {
        return switch (itemType == null ? "" : itemType) {
            case "skin" -> "캐릭터 스킨";
            case "profile" -> "프로필";
            case "ticket" -> "이용권";
            case "pvp_badge" -> "배틀 아이템";
            case "gift" -> "선물";
            case "consumable" -> "소모품";
            default -> itemType;
        };
    }

    private static String categoryLabel(String category) {
        return switch (category == null ? "" : category) {
            case "character" -> "캐릭터";
            case "pass" -> "패스";
            case "badge" -> "배지";
            default -> category;
        };
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
