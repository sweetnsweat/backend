package com.capstone.backend.shop.service;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.shop.entity.Item;
import com.capstone.backend.shop.entity.UserItemEffect;
import com.capstone.backend.shop.repository.UserItemEffectRepository;
import com.capstone.backend.user.entity.User;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopPassEffectService {

    public static final String REF_TYPE_BATTLE = "battle";

    private final UserItemEffectRepository userItemEffectRepository;

    public ShopPassEffectService(UserItemEffectRepository userItemEffectRepository) {
        this.userItemEffectRepository = userItemEffectRepository;
    }

    public String effectType(Item item) {
        return switch (item.getName()) {
            case "EXP 2배권" -> UserItemEffect.EFFECT_EXP_BOOST;
            case "기록 방어권" -> UserItemEffect.EFFECT_RECORD_SHIELD;
            case "승률하락 방어권" -> UserItemEffect.EFFECT_WIN_RATE_SHIELD;
            case "배틀 부활권" -> UserItemEffect.EFFECT_BATTLE_RETRY;
            default -> null;
        };
    }

    @Transactional
    public UserItemEffect activate(User user, Item item) {
        String effectType = effectType(item);
        Instant now = KoreanTime.nowInstant();
        Instant expiresAt = UserItemEffect.EFFECT_EXP_BOOST.equals(effectType) ? now.plus(Duration.ofHours(24)) : null;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("itemName", item.getName());
        metadata.put("itemType", item.getItemType());
        metadata.put("effect", item.getMetadata().get("effect"));
        return userItemEffectRepository.save(UserItemEffect.active(user, item, effectType, now, expiresAt, metadata));
    }

    public boolean hasActiveExpBoost(Long userId) {
        return userItemEffectRepository.existsActiveEffect(
                userId,
                UserItemEffect.EFFECT_EXP_BOOST,
                KoreanTime.nowInstant()
        );
    }

    public int applyExpBoost(Long userId, int rewardExp) {
        if (rewardExp <= 0 || !hasActiveExpBoost(userId)) {
            return rewardExp;
        }
        return rewardExp * 2;
    }

    @Transactional
    public boolean consumeNextLossProtection(Long userId, Long battleId) {
        if (consumeActiveEffect(userId, UserItemEffect.EFFECT_WIN_RATE_SHIELD, REF_TYPE_BATTLE, battleId)) {
            return true;
        }
        return consumeActiveEffect(userId, UserItemEffect.EFFECT_BATTLE_RETRY, REF_TYPE_BATTLE, battleId);
    }

    @Transactional
    public int applyRecordShield(Long userId, BattleMode mode, Long battleId, int finalScore) {
        Integer bestScore = userItemEffectRepository.findBestFinalScore(userId, mode, battleId);
        if (bestScore == null || finalScore >= bestScore) {
            return finalScore;
        }
        if (!consumeActiveEffect(userId, UserItemEffect.EFFECT_RECORD_SHIELD, REF_TYPE_BATTLE, battleId)) {
            return finalScore;
        }
        return bestScore;
    }

    private boolean consumeActiveEffect(Long userId, String effectType, String refType, Long refId) {
        List<UserItemEffect> effects = userItemEffectRepository.findActiveEffectsForUpdate(
                userId,
                effectType,
                KoreanTime.nowInstant(),
                PageRequest.of(0, 1)
        );
        if (effects.isEmpty()) {
            return false;
        }
        effects.get(0).markUsed(refType, refId, KoreanTime.nowInstant());
        return true;
    }
}
