package com.capstone.backend.shop.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.shop.dto.PurchaseItemRequest;
import com.capstone.backend.shop.dto.ShopEquipResponse;
import com.capstone.backend.shop.dto.ShopItemListResponse;
import com.capstone.backend.shop.dto.ShopItemUseResponse;
import com.capstone.backend.shop.dto.ShopPurchaseResponse;
import com.capstone.backend.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "상점", description = "아이템 상점 API")
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @Operation(summary = "상점 아이템 목록 조회", description = "판매/보상 아이템 목록과 내 보유 수량, 현재 골드를 함께 조회합니다.")
    @GetMapping("/items")
    public ApiResponse<ShopItemListResponse> items(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "아이템 유형 필터. 예: skin, profile, ticket")
            @RequestParam(required = false) String type
    ) {
        return ApiResponse.ok(shopService.getItems(authUser.userId(), type));
    }

    @Operation(summary = "아이템 구매", description = "골드를 차감하고 구매한 아이템을 보유 아이템에 추가합니다.")
    @PostMapping("/items/{itemId}/purchase")
    public ApiResponse<ShopPurchaseResponse> purchase(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId,
            @Valid @RequestBody PurchaseItemRequest request
    ) {
        return ApiResponse.ok("아이템을 구매했습니다.", shopService.purchaseItem(authUser.userId(), itemId, request));
    }

    @Operation(summary = "이미지 아이템 장착", description = "보유 중인 profile/skin 타입 이미지 아이템을 내 프로필 이미지로 장착합니다.")
    @PostMapping("/items/{itemId}/equip")
    public ApiResponse<ShopEquipResponse> equip(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId
    ) {
        return ApiResponse.ok("아이템을 장착했습니다.", shopService.equipItem(authUser.userId(), itemId));
    }

    @Operation(summary = "패스/소모 아이템 사용", description = "보유 중인 패스 아이템을 1개 소비하고 서버 효과를 적용합니다.")
    @PostMapping("/items/{itemId}/use")
    public ApiResponse<ShopItemUseResponse> use(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId
    ) {
        return ApiResponse.ok("아이템을 사용했습니다.", shopService.useItem(authUser.userId(), itemId));
    }
}
