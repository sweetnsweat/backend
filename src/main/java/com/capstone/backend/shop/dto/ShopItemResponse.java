package com.capstone.backend.shop.dto;

import com.capstone.backend.shop.entity.Item;
import java.util.Map;

public record ShopItemResponse(
        Long id,
        String itemType,
        String category,
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
                category(item.getItemType()),
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

    private static String category(String itemType) {
        return switch (itemType == null ? "" : itemType) {
            case "skin", "profile" -> "character";
            case "ticket", "pvp_badge", "gift", "consumable" -> "pass";
            default -> itemType;
        };
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
