package com.capstone.backend.shop.dto;

public record ShopPurchaseResponse(
        PurchasedItemResponse item,
        Integer balanceCurrency,
        WalletTransactionResponse transaction
) {
}
