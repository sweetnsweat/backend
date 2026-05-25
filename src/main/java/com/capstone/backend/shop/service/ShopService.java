package com.capstone.backend.shop.service;

import com.capstone.backend.achievement.service.AchievementBadgeService;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.reward.entity.Wallet;
import com.capstone.backend.reward.entity.WalletTransaction;
import com.capstone.backend.reward.repository.WalletRepository;
import com.capstone.backend.reward.repository.WalletTransactionRepository;
import com.capstone.backend.quest.dto.QuestResponse;
import com.capstone.backend.quest.service.QuestService;
import com.capstone.backend.shop.dto.PurchaseItemRequest;
import com.capstone.backend.shop.dto.PurchasedItemResponse;
import com.capstone.backend.shop.dto.ShopEquipResponse;
import com.capstone.backend.shop.dto.ShopItemListResponse;
import com.capstone.backend.shop.dto.ShopItemResponse;
import com.capstone.backend.shop.dto.ShopItemUseResponse;
import com.capstone.backend.shop.dto.ShopPurchaseResponse;
import com.capstone.backend.shop.dto.WalletTransactionResponse;
import com.capstone.backend.shop.entity.Item;
import com.capstone.backend.shop.entity.UserItem;
import com.capstone.backend.shop.entity.UserItemEffect;
import com.capstone.backend.shop.repository.ItemRepository;
import com.capstone.backend.shop.repository.UserItemRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopService {

    private static final Set<String> IMAGE_EQUIPPABLE_TYPES = Set.of("profile", "skin");
    private static final Set<String> CHARACTER_TYPES = Set.of("profile", "skin");
    private static final Set<String> PASS_TYPES = Set.of("ticket", "pvp_badge", "gift", "consumable");

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ShopPassEffectService shopPassEffectService;
    private final QuestService questService;
    private final MediaUrlResolver mediaUrlResolver;

    public ShopService(ItemRepository itemRepository,
                       UserItemRepository userItemRepository,
                       UserRepository userRepository,
                       WalletRepository walletRepository,
                       WalletTransactionRepository walletTransactionRepository,
                       ShopPassEffectService shopPassEffectService,
                       QuestService questService,
                       MediaUrlResolver mediaUrlResolver) {
        this.itemRepository = itemRepository;
        this.userItemRepository = userItemRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.shopPassEffectService = shopPassEffectService;
        this.questService = questService;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional(readOnly = true)
    public ShopItemListResponse getItems(Long userId, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        List<Item> items = itemsByType(type);
        int balanceCurrency = walletRepository.findById(userId)
                .map(Wallet::getBalanceCurrency)
                .orElse(0);
        Map<Long, Integer> ownedQuantities = userItemRepository.findByUser_Id(userId).stream()
                .collect(Collectors.toMap(userItem -> userItem.getItem().getId(), UserItem::getQuantity));

        List<ShopItemResponse> itemResponses = items.stream()
                .map(item -> ShopItemResponse.from(
                        item,
                        ownedQuantities.getOrDefault(item.getId(), 0),
                        balanceCurrency,
                        user.getProfileImageUrl(),
                        mediaUrlResolver
                ))
                .toList();
        return new ShopItemListResponse(itemResponses, balanceCurrency);
    }

    private List<Item> itemsByType(String type) {
        if (!StringUtils.hasText(type)) {
            return itemRepository.findByActiveTrueOrderByIdAsc().stream()
                    .filter(item -> !isAchievementBadge(item))
                    .toList();
        }
        String normalizedType = type.trim();
        if ("character".equals(normalizedType)) {
            return itemRepository.findByItemTypeInAndActiveTrueOrderByIdAsc(CHARACTER_TYPES);
        }
        if ("pass".equals(normalizedType)) {
            return itemRepository.findByItemTypeInAndActiveTrueOrderByIdAsc(PASS_TYPES).stream()
                    .filter(item -> !isAchievementBadge(item))
                    .toList();
        }
        if ("badge".equals(normalizedType)) {
            return itemRepository.findByActiveTrueOrderByIdAsc().stream()
                    .filter(this::isAchievementBadge)
                    .toList();
        }
        return itemRepository.findByItemTypeAndActiveTrueOrderByIdAsc(normalizedType).stream()
                .filter(item -> !isAchievementBadge(item))
                .toList();
    }

    @Transactional
    public ShopPurchaseResponse purchaseItem(Long userId, Long itemId, PurchaseItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        Item item = itemRepository.findByIdAndActiveTrue(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "구매할 아이템을 찾을 수 없습니다."));
        if (!Boolean.TRUE.equals(item.getSellable())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_SELLABLE", "판매 중인 아이템이 아닙니다.");
        }

        int quantity = request.quantity();
        int totalPrice = totalPrice(item, quantity);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.create(user)));
        if (wallet.getBalanceCurrency() < totalPrice) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "골드가 부족합니다.");
        }

        wallet.debit(totalPrice);
        UserItem userItem = userItemRepository.findByUser_IdAndItem_Id(userId, itemId)
                .orElseGet(() -> UserItem.create(user, item, 0));
        userItem.increaseQuantity(quantity);
        UserItem savedUserItem = userItemRepository.save(userItem);
        walletTransactionRepository.save(WalletTransaction.purchase(user, totalPrice, item.getId(), "아이템 구매"));

        return new ShopPurchaseResponse(
                PurchasedItemResponse.from(savedUserItem),
                wallet.getBalanceCurrency(),
                new WalletTransactionResponse(WalletTransaction.TX_TYPE_PURCHASE, -totalPrice)
        );
    }

    @Transactional
    public ShopEquipResponse equipItem(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        UserItem userItem = userItemRepository.findByUser_IdAndItem_Id(userId, itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_OWNED", "보유한 아이템만 장착할 수 있습니다."));
        if (userItem.getQuantity() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_OWNED", "보유한 아이템만 장착할 수 있습니다.");
        }

        Item item = userItem.getItem();
        if (!isImageEquippable(item)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_EQUIPPABLE", "이미지 장착이 가능한 아이템이 아닙니다.");
        }

        user.updateProfileSettings(null, item.getImageUrl());
        return new ShopEquipResponse(PurchasedItemResponse.from(userItem), user.getProfileImageUrl());
    }

    @Transactional
    public ShopItemUseResponse useItem(Long userId, Long itemId) {
        UserItem userItem = userItemRepository.findByUser_IdAndItem_Id(userId, itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_OWNED", "보유한 아이템만 사용할 수 있습니다."));
        if (userItem.getQuantity() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_OWNED", "보유한 아이템만 사용할 수 있습니다.");
        }

        Item item = userItem.getItem();
        if ("퀘스트 스킵권".equals(item.getName())) {
            QuestResponse quest = questService.completeTodayQuestWithShopPass(userId, item.getId(), item.getName());
            userItem.decreaseQuantity(1);
            return ShopItemUseResponse.questSkipped(userItem, quest, "오늘 퀘스트를 스킵권으로 완료했습니다.");
        }

        String effectType = shopPassEffectService.effectType(item);
        if (effectType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_USABLE", "사용 효과가 없는 아이템입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        userItem.decreaseQuantity(1);
        UserItemEffect effect = shopPassEffectService.activate(user, item);
        return ShopItemUseResponse.activated(userItem, effect, useMessage(effectType));
    }

    private int totalPrice(Item item, int quantity) {
        long totalPrice = (long) item.getPriceCurrency() * quantity;
        if (totalPrice > Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PURCHASE_AMOUNT_TOO_LARGE", "구매 금액이 너무 큽니다.");
        }
        return (int) totalPrice;
    }

    private boolean isImageEquippable(Item item) {
        return item != null
                && IMAGE_EQUIPPABLE_TYPES.contains(item.getItemType())
                && StringUtils.hasText(item.getImageUrl());
    }

    private boolean isAchievementBadge(Item item) {
        return item != null
                && AchievementBadgeService.KIND_ACHIEVEMENT_BADGE.equals(String.valueOf(item.getMetadata().get("kind")));
    }

    private String useMessage(String effectType) {
        return switch (effectType) {
            case UserItemEffect.EFFECT_EXP_BOOST -> "24시간 동안 EXP 2배 효과가 적용됩니다.";
            case UserItemEffect.EFFECT_RECORD_SHIELD -> "다음 배틀 기록 하락 시 최고 기록을 방어합니다.";
            case UserItemEffect.EFFECT_WIN_RATE_SHIELD -> "다음 배틀 패배 시 승률 하락을 방어합니다.";
            case UserItemEffect.EFFECT_BATTLE_RETRY -> "다음 배틀 패배 시 패배 처리를 방어합니다.";
            default -> "아이템 효과를 활성화했습니다.";
        };
    }
}
