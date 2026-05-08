package com.capstone.backend.shop.dto;

public record WalletTransactionResponse(
        String txType,
        Integer amount
) {
}
