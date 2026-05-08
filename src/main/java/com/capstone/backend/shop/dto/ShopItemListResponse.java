package com.capstone.backend.shop.dto;

import java.util.List;

public record ShopItemListResponse(
        List<ShopItemResponse> items,
        Integer balanceCurrency
) {
}
