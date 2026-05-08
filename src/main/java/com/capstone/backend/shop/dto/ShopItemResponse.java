package com.capstone.backend.shop.dto;

import com.capstone.backend.shop.entity.Item;
import java.util.Map;

public record ShopItemResponse(
        Long id,
        String itemType,
        String name,
        String description,
        Integer priceCurrency,
        Boolean sellable,
        Boolean owned,
        Integer ownedQuantity,
        Boolean purchasable,
        String imageUrl,
        Map<String, Object> metadata
) {
    public static ShopItemResponse from(Item item, int ownedQuantity, int balanceCurrency) {
        boolean sellable = Boolean.TRUE.equals(item.getSellable());
        return new ShopItemResponse(
                item.getId(),
                item.getItemType(),
                item.getName(),
                item.getDescription(),
                item.getPriceCurrency(),
                sellable,
                ownedQuantity > 0,
                ownedQuantity,
                sellable && balanceCurrency >= item.getPriceCurrency(),
                item.getImageUrl(),
                item.getMetadata()
        );
    }
}
