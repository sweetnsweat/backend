package com.capstone.backend.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseItemRequest(
        @NotNull(message = "구매 수량을 입력해 주세요.")
        @Min(value = 1, message = "구매 수량은 1 이상이어야 합니다.")
        @Max(value = 99, message = "구매 수량은 99 이하여야 합니다.")
        Integer quantity
) {
}
