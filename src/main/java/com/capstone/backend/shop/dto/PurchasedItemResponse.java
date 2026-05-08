package com.capstone.backend.shop.dto;

import com.capstone.backend.shop.entity.UserItem;
import java.util.Map;

public record PurchasedItemResponse(
        Long id,
        Long itemId,
        String itemType,
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
                userItem.getItem().getName(),
                userItem.getItem().getDescription(),
                userItem.getQuantity(),
                userItem.getItem().getImageUrl(),
                userItem.getItem().getMetadata()
        );
    }
}
