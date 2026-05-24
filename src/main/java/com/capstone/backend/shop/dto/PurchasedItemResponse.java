package com.capstone.backend.shop.dto;

import com.capstone.backend.shop.entity.UserItem;
import java.util.Map;

public record PurchasedItemResponse(
        Long id,
        Long itemId,
        String itemType,
        String itemTypeLabel,
        String name,
        String description,
        Integer quantity,
        String imageUrl,
        Map<String, Object> metadata
) {
    public static PurchasedItemResponse from(UserItem userItem) {
        return new PurchasedItemResponse(
                userItem.getId(),
                userItem.getItem().getId(),
                userItem.getItem().getItemType(),
                itemTypeLabel(userItem.getItem().getItemType()),
                userItem.getItem().getName(),
                userItem.getItem().getDescription(),
                userItem.getQuantity(),
                userItem.getItem().getImageUrl(),
                userItem.getItem().getMetadata()
        );
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
}
