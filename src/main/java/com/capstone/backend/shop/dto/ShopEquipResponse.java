package com.capstone.backend.shop.dto;

public record ShopEquipResponse(
        PurchasedItemResponse item,
        String profileImageUrl
) {
}
