package com.capstone.backend.achievement.service;

import com.capstone.backend.achievement.dto.AchievementBadgeListResponse;
import com.capstone.backend.achievement.dto.AchievementBadgeResponse;
import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.battle.entity.BattleResult;
import com.capstone.backend.battle.repository.BattleParticipantRepository;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.shop.entity.Item;
import com.capstone.backend.shop.entity.UserItem;
import com.capstone.backend.shop.repository.ItemRepository;
import com.capstone.backend.shop.repository.UserItemRepository;
import com.capstone.backend.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementBadgeService {

    public static final String KIND_ACHIEVEMENT_BADGE = "achievement_badge";

    private static final String FIRST_QUEST_COMPLETE = "FIRST_QUEST_COMPLETE";
    private static final String QUEST_STREAK_3 = "QUEST_STREAK_3";
    private static final String QUEST_10_COMPLETE = "QUEST_10_COMPLETE";
    private static final String FIRST_BATTLE_JOIN = "FIRST_BATTLE_JOIN";
    private static final String FIRST_BATTLE_WIN = "FIRST_BATTLE_WIN";
    private static final String BATTLE_SCORE_1000 = "BATTLE_SCORE_1000";

    private static final int QUEST_STREAK_TARGET = 3;
    private static final int QUEST_TOTAL_TARGET = 10;
    private static final int BATTLE_SCORE_TARGET = 1000;

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserQuestRepository userQuestRepository;
    private final BattleParticipantRepository battleParticipantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AchievementBadgeService(ItemRepository itemRepository,
                                   UserItemRepository userItemRepository,
                                   UserQuestRepository userQuestRepository,
                                   BattleParticipantRepository battleParticipantRepository) {
        this.itemRepository = itemRepository;
        this.userItemRepository = userItemRepository;
        this.userQuestRepository = userQuestRepository;
        this.battleParticipantRepository = battleParticipantRepository;
    }

    @Transactional(readOnly = true)
    public AchievementBadgeListResponse getBadges(Long userId) {
        return AchievementBadgeListResponse.from(badgeResponses(userId));
    }

    @Transactional
    public AchievementBadgeListResponse syncBadges(Long userId) {
        evaluateQuestBadges(userId);
        evaluateBattleBadges(userId);
        return AchievementBadgeListResponse.from(badgeResponses(userId));
    }

    @Transactional
    public void awardForQuestCompletion(UserQuest quest) {
        if (quest == null || quest.getUser() == null || quest.getUser().getId() == null) {
            return;
        }
        evaluateQuestBadges(quest.getUser().getId());
    }

    @Transactional
    public void awardForBattleFinalized(List<BattleParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        participants.stream()
                .map(BattleParticipant::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::evaluateBattleBadges);
    }

    private void evaluateQuestBadges(Long userId) {
        List<UserQuest> completedQuests = userQuestRepository.findCompletedByUserIdOrderByQuestDateAsc(userId);
        int completedCount = completedQuests.size();
        if (completedCount >= 1) {
            awardBadge(userId, FIRST_QUEST_COMPLETE);
        }
        if (completedCount >= QUEST_TOTAL_TARGET) {
            awardBadge(userId, QUEST_10_COMPLETE);
        }
        if (maxConsecutiveDays(completedQuests) >= QUEST_STREAK_TARGET) {
            awardBadge(userId, QUEST_STREAK_3);
        }
    }

    private void evaluateBattleBadges(Long userId) {
        if (battleParticipantRepository.countFinalizedByUserId(userId) >= 1) {
            awardBadge(userId, FIRST_BATTLE_JOIN);
        }
        if (battleParticipantRepository.countFinalizedByUserIdAndResult(userId, BattleResult.WIN) >= 1) {
            awardBadge(userId, FIRST_BATTLE_WIN);
        }
        if (battleParticipantRepository.countFinalizedByUserIdAndFinalScoreGreaterThanEqual(userId, BATTLE_SCORE_TARGET) >= 1) {
            awardBadge(userId, BATTLE_SCORE_1000);
        }
    }

    private void awardBadge(Long userId, String badgeCode) {
        Item badge = findBadgeByCode(badgeCode).orElse(null);
        if (badge == null) {
            return;
        }
        UserItem userItem = userItemRepository.findByUser_IdAndItem_Id(userId, badge.getId())
                .orElseGet(() -> UserItem.create(entityManager.getReference(User.class, userId), badge, 0));
        if (userItem.getQuantity() <= 0) {
            userItem.increaseQuantity(1);
            userItemRepository.save(userItem);
        }
    }

    private Optional<Item> findBadgeByCode(String badgeCode) {
        return achievementBadges().stream()
                .filter(item -> badgeCode.equals(stringValue(item.getMetadata().get("badgeCode"))))
                .findFirst();
    }

    private List<AchievementBadgeResponse> badgeResponses(Long userId) {
        Map<Long, UserItem> userItemsByItemId = userItemRepository.findByUser_Id(userId).stream()
                .collect(Collectors.toMap(userItem -> userItem.getItem().getId(), Function.identity(), (left, right) -> left));
        return achievementBadges().stream()
                .map(badge -> AchievementBadgeResponse.from(badge, userItemsByItemId.get(badge.getId())))
                .toList();
    }

    private List<Item> achievementBadges() {
        return itemRepository.findByActiveTrueOrderByIdAsc().stream()
                .filter(this::isAchievementBadge)
                .sorted(Comparator.comparing(Item::getId))
                .toList();
    }

    private boolean isAchievementBadge(Item item) {
        return item != null && KIND_ACHIEVEMENT_BADGE.equals(stringValue(item.getMetadata().get("kind")));
    }

    private int maxConsecutiveDays(List<UserQuest> completedQuests) {
        Set<LocalDate> dates = completedQuests.stream()
                .map(UserQuest::getQuestDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int max = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : dates) {
            if (previous == null || previous.plusDays(1).equals(date)) {
                current += 1;
            } else {
                current = 1;
            }
            max = Math.max(max, current);
            previous = date;
        }
        return max;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
