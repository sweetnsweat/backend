package com.capstone.backend.battle.service;

import com.capstone.backend.achievement.service.AchievementBadgeService;
import com.capstone.backend.battle.dto.BattleCurrentSummaryResponse;
import com.capstone.backend.battle.dto.BattleDetailResponse;
import com.capstone.backend.battle.dto.BattleHealthSyncResponse;
import com.capstone.backend.battle.dto.BattleHistoryItemResponse;
import com.capstone.backend.battle.dto.BattleHistoryPageResponse;
import com.capstone.backend.battle.dto.BattleMetricResponse;
import com.capstone.backend.battle.dto.BattleParticipantResponse;
import com.capstone.backend.battle.dto.BattleResultResponse;
import com.capstone.backend.battle.dto.BattleScoreResponse;
import com.capstone.backend.battle.dto.BattleSummaryResponse;
import com.capstone.backend.battle.entity.Battle;
import com.capstone.backend.battle.entity.BattleMatchQueue;
import com.capstone.backend.battle.entity.BattleMatchQueueStatus;
import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.battle.entity.BattleResult;
import com.capstone.backend.battle.entity.BattleStatus;
import com.capstone.backend.battle.repository.BattleMatchQueueRepository;
import com.capstone.backend.battle.repository.BattleParticipantRepository;
import com.capstone.backend.battle.repository.BattleRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.health.entity.HealthDailySummary;
import com.capstone.backend.health.repository.HealthDailySummaryRepository;
import com.capstone.backend.notification.service.NotificationService;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.reward.policy.BattleRewardPolicy;
import com.capstone.backend.reward.policy.BattleRewardPolicy.BattleReward;
import com.capstone.backend.reward.service.RewardService;
import com.capstone.backend.shop.service.ShopPassEffectService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleService {

    private static final int HISTORY_MAX_SIZE = 100;
    private static final int QUEST_COMPLETION_POINTS = 100;
    private static final int HEALTH_VERIFIED_POINTS = 50;
    private static final Duration HEALTH_SYNC_STALE_AFTER = Duration.ofMinutes(30);

    private final BattleRepository battleRepository;
    private final BattleMatchQueueRepository battleMatchQueueRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final HealthDailySummaryRepository healthDailySummaryRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RewardService rewardService;
    private final ShopPassEffectService shopPassEffectService;
    private final AchievementBadgeService achievementBadgeService;
    private final MediaUrlResolver mediaUrlResolver;

    public BattleService(BattleRepository battleRepository,
                         BattleMatchQueueRepository battleMatchQueueRepository,
                         BattleParticipantRepository battleParticipantRepository,
                         HealthDailySummaryRepository healthDailySummaryRepository,
                         UserQuestRepository userQuestRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         RewardService rewardService,
                         ShopPassEffectService shopPassEffectService,
                         AchievementBadgeService achievementBadgeService,
                         MediaUrlResolver mediaUrlResolver) {
        this.battleRepository = battleRepository;
        this.battleMatchQueueRepository = battleMatchQueueRepository;
        this.battleParticipantRepository = battleParticipantRepository;
        this.healthDailySummaryRepository = healthDailySummaryRepository;
        this.userQuestRepository = userQuestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.rewardService = rewardService;
        this.shopPassEffectService = shopPassEffectService;
        this.achievementBadgeService = achievementBadgeService;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional(readOnly = true)
    public BattleSummaryResponse getSummary(Long userId) {
        long wins = battleParticipantRepository.countByUserIdAndResult(userId, BattleResult.WIN);
        long losses = battleParticipantRepository.countByUserIdAndResult(userId, BattleResult.LOSS);
        long draws = battleParticipantRepository.countByUserIdAndResult(userId, BattleResult.DRAW);
        long completed = wins + losses + draws;
        int winRate = completed == 0 ? 0 : Math.round((wins * 100.0f) / completed);

        return new BattleSummaryResponse(
                rankName(wins),
                wins,
                losses,
                draws,
                winRate,
                currentBattle(userId, BattleMode.DAILY).map(this::toCurrentSummary).orElse(null),
                currentBattle(userId, BattleMode.WEEKLY).map(this::toCurrentSummary).orElse(null)
        );
    }

    @Transactional
    public BattleDetailResponse match(Long userId, BattleMode mode) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        Instant now = KoreanTime.nowInstant();
        BattlePeriod queuePeriod = BattlePeriod.fromStart(mode, now);

        Optional<Battle> currentBattle = findCurrentBattle(userId, mode, now);
        if (currentBattle.isPresent()) {
            return toDetail(currentBattle.get(), userId);
        }

        Optional<BattleMatchQueue> currentWaitingQueue = battleMatchQueueRepository
                .findByUser_IdAndModeAndStatusAndExpiresAtAfter(
                        userId,
                        mode,
                        BattleMatchQueueStatus.WAITING,
                        now
                );
        if (currentWaitingQueue.isPresent()) {
            return toWaitingDetail(currentUser, BattlePeriod.fromQueue(currentWaitingQueue.get()), currentWaitingQueue.get());
        }

        Optional<BattleMatchQueue> opponentQueue = battleMatchQueueRepository.findWaitingOpponents(
                userId,
                mode,
                now,
                PageRequest.of(0, 1)
        ).stream().findFirst();

        BattleMatchQueue myQueue = battleMatchQueueRepository.save(BattleMatchQueue.waiting(
                currentUser,
                mode,
                queuePeriod.startDate(),
                queuePeriod.endDate(),
                queuePeriod.endsAt()
        ));
        if (opponentQueue.isEmpty()) {
            return toWaitingDetail(currentUser, queuePeriod, myQueue);
        }

        User opponent = opponentQueue.get().getUser();
        Instant matchedAt = KoreanTime.nowInstant();
        BattlePeriod battlePeriod = BattlePeriod.fromStart(mode, matchedAt);
        Battle battle = battleRepository.save(Battle.create(
                mode,
                battlePeriod.startDate(),
                battlePeriod.endDate(),
                matchedAt,
                battlePeriod.endsAt()
        ));
        List<BattleParticipant> participants = battleParticipantRepository.saveAll(List.of(
                BattleParticipant.join(battle, currentUser),
                BattleParticipant.join(battle, opponent)
        ));
        participants.forEach(participant -> updateBaseline(participant, battlePeriod));
        opponentQueue.get().match(battle, matchedAt);
        myQueue.match(battle, matchedAt);
        notificationService.sendBattleMatched(battle, participants);
        return toDetail(battle, userId);
    }

    @Transactional(readOnly = true)
    public BattleDetailResponse getDetail(Long userId, Long battleId) {
        Battle battle = battleRepository.findByIdAndParticipantUserId(battleId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BATTLE_NOT_FOUND", "배틀을 찾을 수 없습니다."));
        return toDetail(battle, userId);
    }

    @Transactional
    public BattleResultResponse getResult(Long userId, Long battleId) {
        Battle battle = battleRepository.findByIdAndParticipantUserId(battleId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BATTLE_NOT_FOUND", "배틀을 찾을 수 없습니다."));
        if (!battle.isFinalized() && !KoreanTime.nowInstant().isBefore(battle.getEndsAt())) {
            finalizeBattle(battle);
        }
        return toResult(battle, userId);
    }

    @Transactional(readOnly = true)
    public BattleHistoryPageResponse getHistory(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), HISTORY_MAX_SIZE);
        Page<BattleParticipant> participantPage = battleParticipantRepository.findFinalizedHistoryByUserId(
                userId,
                PageRequest.of(safePage, safeSize)
        );
        List<BattleHistoryItemResponse> items = participantPage.getContent().stream()
                .map(participant -> toHistoryItem(participant, userId))
                .toList();
        return new BattleHistoryPageResponse(
                participantPage.getNumber(),
                participantPage.getSize(),
                participantPage.getTotalElements(),
                participantPage.getTotalPages(),
                participantPage.isFirst(),
                participantPage.isLast(),
                participantPage.hasNext(),
                participantPage.hasNext() ? participantPage.getNumber() + 1 : null,
                items
        );
    }

    private Optional<Battle> currentBattle(Long userId, BattleMode mode) {
        return findCurrentBattle(userId, mode, KoreanTime.nowInstant());
    }

    private Optional<Battle> findCurrentBattle(Long userId, BattleMode mode, Instant now) {
        return battleRepository.findCurrentBattlesForUser(
                userId,
                mode,
                List.of(BattleStatus.ACTIVE),
                now,
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    private void finalizeBattle(Battle battle) {
        List<BattleParticipant> participants = battleParticipantRepository.findByBattle_IdOrderByIdAsc(battle.getId());
        if (participants.size() != 2) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_PARTICIPANT_INVALID", "배틀 참여자 구성이 올바르지 않습니다.");
        }

        BattlePeriod period = BattlePeriod.fromBattle(battle);
        BattleParticipant first = participants.get(0);
        BattleParticipant second = participants.get(1);
        BattleStats firstStats = loadBattleStats(first, period);
        BattleStats secondStats = loadBattleStats(second, period);

        int firstScore = shopPassEffectService.applyRecordShield(
                first.getUser().getId(), battle.getMode(), battle.getId(), firstStats.totalScore());
        int secondScore = shopPassEffectService.applyRecordShield(
                second.getUser().getId(), battle.getMode(), battle.getId(), secondStats.totalScore());

        BattleResult firstResult = protectedResult(first.getUser().getId(), battle.getId(), resultFor(firstScore, secondScore));
        BattleResult secondResult = protectedResult(second.getUser().getId(), battle.getId(), resultFor(secondScore, firstScore));
        first.finalizeResult(firstScore, firstResult);
        second.finalizeResult(secondScore, secondResult);
        battle.finalizeBattle(KoreanTime.nowInstant());
        issueWinRewards(battle, participants);
        achievementBadgeService.awardForBattleFinalized(participants);
        notificationService.sendBattleResultReady(battle, participants);
    }

    private void issueWinRewards(Battle battle, List<BattleParticipant> participants) {
        BattleReward reward = BattleRewardPolicy.win(battle.getMode());
        participants.stream()
                .filter(participant -> BattleResult.WIN.equals(participant.getResult()))
                .forEach(participant -> rewardService.issueBattleWinReward(participant.getUser(), battle.getId(), reward));
    }

    private BattleResult protectedResult(Long userId, Long battleId, BattleResult result) {
        if (!BattleResult.LOSS.equals(result)) {
            return result;
        }
        return shopPassEffectService.consumeNextLossProtection(userId, battleId) ? BattleResult.DRAW : result;
    }

    private BattleDetailResponse toDetail(Battle battle, Long currentUserId) {
        BattlePeriod period = BattlePeriod.fromBattle(battle);
        BattleSnapshot snapshot = snapshot(battle, currentUserId, period);
        return new BattleDetailResponse(
                battle.getId(),
                battle.getMode(),
                battle.getStatus(),
                battle.getPeriodStartDate(),
                battle.getPeriodEndDate(),
                battle.getStartsAt(),
                battle.getEndsAt(),
                remainingSeconds(battle),
                "MATCHED",
                null,
                snapshot.participants(),
                new BattleScoreResponse(snapshot.myStats().totalScore(), snapshot.opponentStats().totalScore(), leadingUserId(snapshot)),
                metrics(snapshot.myStats(), snapshot.opponentStats()),
                healthSync(currentUserId, period)
        );
    }

    private BattleDetailResponse toWaitingDetail(User currentUser, BattlePeriod period, BattleMatchQueue queue) {
        BattleStats myStats = loadStats(currentUser.getId(), period);
        BattleStats emptyOpponentStats = BattleStats.empty();
        return new BattleDetailResponse(
                null,
                period.mode(),
                null,
                period.startDate(),
                period.endDate(),
                period.startsAt(),
                period.endsAt(),
                remainingSeconds(period.endsAt()),
                "WAITING",
                queue.getQueuedAt(),
                List.of(new BattleParticipantResponse(
                        currentUser.getId(),
                        currentUser.getNickname(),
                        mediaUrlResolver.resolve(currentUser.getProfileImageUrl()),
                        true,
                        myStats.totalScore(),
                        BattleResult.PENDING,
                        latestHealthSyncedAt(currentUser.getId(), period)
                )),
                new BattleScoreResponse(myStats.totalScore(), null, null),
                metrics(myStats, emptyOpponentStats),
                healthSync(currentUser.getId(), period)
        );
    }

    private BattleResultResponse toResult(Battle battle, Long currentUserId) {
        BattlePeriod period = BattlePeriod.fromBattle(battle);
        BattleSnapshot snapshot = snapshot(battle, currentUserId, period);
        Long winnerUserId = winnerUserId(snapshot);
        BattleResult result = resultFor(snapshot.myStats().totalScore(), snapshot.opponentStats().totalScore());
        if (battle.isFinalized()) {
            result = snapshot.me().getResult();
        }
        BattleReward reward = BattleRewardPolicy.win(battle.getMode());
        int rewardExp = battle.isFinalized() && BattleResult.WIN.equals(result) ? reward.exp() : 0;
        int rewardGold = battle.isFinalized() && BattleResult.WIN.equals(result) ? reward.currency() : 0;

        return new BattleResultResponse(
                battle.getId(),
                battle.getMode(),
                battle.getStatus(),
                battle.getPeriodStartDate(),
                battle.getPeriodEndDate(),
                battle.getStartsAt(),
                battle.getEndsAt(),
                battle.isFinalized(),
                result,
                winnerUserId,
                rewardExp,
                rewardGold,
                snapshot.myStats().totalScore(),
                snapshot.opponentStats().totalScore(),
                snapshot.participants(),
                metrics(snapshot.myStats(), snapshot.opponentStats()),
                healthSync(currentUserId, period)
        );
    }

    private BattleHealthSyncResponse healthSync(Long userId, BattlePeriod period) {
        Instant now = KoreanTime.nowInstant();
        Instant windowEnd = now.isBefore(period.endsAt()) ? now : period.endsAt();
        if (windowEnd.isBefore(period.startsAt())) {
            windowEnd = period.startsAt();
        }
        Instant latestSyncedAt = latestHealthSyncedAt(userId, period);
        boolean required = latestSyncedAt == null || latestSyncedAt.isBefore(period.startsAt());
        boolean recommended = required || latestSyncedAt.isBefore(windowEnd.minus(HEALTH_SYNC_STALE_AFTER));
        return new BattleHealthSyncResponse(
                required,
                recommended,
                latestSyncedAt,
                period.startsAt(),
                windowEnd,
                HEALTH_SYNC_STALE_AFTER.toSeconds()
        );
    }

    private Instant latestHealthSyncedAt(Long userId, BattlePeriod period) {
        return healthDailySummaryRepository
                .findLatestSyncedAtInDateRange(userId, period.startDate(), period.endDate())
                .orElse(null);
    }

    private BattleHistoryItemResponse toHistoryItem(BattleParticipant participant, Long currentUserId) {
        Battle battle = participant.getBattle();
        List<BattleParticipant> participants = battleParticipantRepository.findByBattle_IdOrderByIdAsc(battle.getId());
        BattleParticipant opponent = participants.stream()
                .filter(item -> !item.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        Integer opponentScore = opponent == null ? null : opponent.getFinalScore();
        BattleParticipantResponse opponentResponse = opponent == null
                ? null
                : new BattleParticipantResponse(
                opponent.getUser().getId(),
                opponent.getUser().getNickname(),
                mediaUrlResolver.resolve(opponent.getUser().getProfileImageUrl()),
                false,
                opponent.getFinalScore(),
                opponent.getResult(),
                latestHealthSyncedAt(opponent.getUser().getId(), BattlePeriod.fromBattle(battle))
        );
        return new BattleHistoryItemResponse(
                battle.getId(),
                battle.getMode(),
                participant.getResult(),
                battle.getPeriodStartDate(),
                battle.getPeriodEndDate(),
                battle.getFinalizedAt(),
                opponentResponse,
                participant.getFinalScore(),
                opponentScore
        );
    }

    private BattleSnapshot snapshot(Battle battle, Long currentUserId, BattlePeriod period) {
        List<BattleParticipant> participants = battleParticipantRepository.findByBattle_IdOrderByIdAsc(battle.getId());
        if (participants.size() != 2) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_PARTICIPANT_INVALID", "배틀 참여자 구성이 올바르지 않습니다.");
        }

        BattleParticipant me = participants.stream()
                .filter(participant -> participant.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BATTLE_NOT_FOUND", "배틀을 찾을 수 없습니다."));
        BattleParticipant opponent = participants.stream()
                .filter(participant -> !participant.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "BATTLE_PARTICIPANT_INVALID", "배틀 참여자 구성이 올바르지 않습니다."));

        Map<Long, BattleStats> statsByUserId = participants.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getId(),
                        participant -> loadBattleStats(participant, period)
                ));
        Map<Long, Instant> latestHealthSyncedAtByUserId = new HashMap<>();
        for (BattleParticipant participant : participants) {
            latestHealthSyncedAtByUserId.put(
                    participant.getUser().getId(),
                    latestHealthSyncedAt(participant.getUser().getId(), period)
            );
        }
        BattleStats myStats = statsByUserId.get(me.getUser().getId());
        BattleStats opponentStats = statsByUserId.get(opponent.getUser().getId());
        List<BattleParticipantResponse> participantResponses = participants.stream()
                .sorted(Comparator.comparing(participant -> !participant.getUser().getId().equals(currentUserId)))
                .map(participant -> {
                    BattleStats stats = statsByUserId.get(participant.getUser().getId());
                    return new BattleParticipantResponse(
                            participant.getUser().getId(),
                            participant.getUser().getNickname(),
                            mediaUrlResolver.resolve(participant.getUser().getProfileImageUrl()),
                            participant.getUser().getId().equals(currentUserId),
                            battle.isFinalized() && participant.getFinalScore() != null ? participant.getFinalScore() : stats.totalScore(),
                            battle.isFinalized() ? participant.getResult() : BattleResult.PENDING,
                            latestHealthSyncedAtByUserId.get(participant.getUser().getId())
                    );
                })
                .toList();
        return new BattleSnapshot(me, opponent, myStats, opponentStats, participantResponses);
    }

    private BattleStats loadStats(Long userId, BattlePeriod period) {
        List<UserQuest> quests = userQuestRepository.findCompletedBattleQuestsInWindow(
                userId,
                period.startsAt(),
                period.endsAt()
        );
        List<UserQuest> battleEligibleQuests = quests.stream()
                .filter(this::battleEligible)
                .toList();
        int completedQuestCount = quests.size();
        int routineQuestCount = (int) quests.stream()
                .filter(quest -> UserQuest.TYPE_ROUTINE.equals(quest.getQuestType()))
                .count();

        BattleHealthStats syncedHealthStats = loadSyncedHealthStats(userId, period);
        int activeMinutes = syncedHealthStats.activeMinutes();
        int steps = syncedHealthStats.steps();
        int distanceMeters = syncedHealthStats.distanceMeters();
        int activeCalories = syncedHealthStats.activeCalories();
        int healthVerifiedQuestCount = 0;

        for (UserQuest quest : battleEligibleQuests) {
            Map<String, Object> proof = quest.getProofJson();
            if (Boolean.TRUE.equals(proof.get("verified"))) {
                healthVerifiedQuestCount++;
            }
            if (!syncedHealthStats.hasData()) {
                Object metricsValue = proof.get("metrics");
                if (!(metricsValue instanceof Map<?, ?> metrics)) {
                    continue;
                }
                activeMinutes += decimal(metrics.get("exerciseMinutes")).setScale(0, RoundingMode.HALF_UP).intValue();
                steps += decimal(metrics.get("steps")).setScale(0, RoundingMode.HALF_UP).intValue();
                distanceMeters += decimal(metrics.get("distanceMeters")).setScale(0, RoundingMode.HALF_UP).intValue();
                activeCalories += decimal(metrics.get("activeCaloriesKcal")).setScale(0, RoundingMode.HALF_UP).intValue();
            }
        }

        int totalScore = completedQuestCount * QUEST_COMPLETION_POINTS
                + healthVerifiedQuestCount * HEALTH_VERIFIED_POINTS
                + activeMinutes * 10
                + Math.round(distanceMeters * 0.03f)
                + Math.round(steps * 0.01f)
                + activeCalories * 2;
        return new BattleStats(totalScore, completedQuestCount, routineQuestCount, activeMinutes, steps, distanceMeters, activeCalories, healthVerifiedQuestCount);
    }

    private void updateBaseline(BattleParticipant participant, BattlePeriod period) {
        BattleStats baseline = loadStats(participant.getUser().getId(), period);
        participant.updateBaseline(
                baseline.totalScore(),
                baseline.completedQuestCount(),
                baseline.routineQuestCount(),
                baseline.activeMinutes(),
                baseline.steps(),
                baseline.distanceMeters(),
                baseline.activeCalories(),
                baseline.healthVerifiedQuestCount()
        );
    }

    private BattleStats loadBattleStats(BattleParticipant participant, BattlePeriod period) {
        return loadStats(participant.getUser().getId(), period).minus(baselineStats(participant));
    }

    private BattleStats baselineStats(BattleParticipant participant) {
        return new BattleStats(
                participant.getBaselineScore(),
                participant.getBaselineCompletedQuestCount(),
                participant.getBaselineRoutineQuestCount(),
                participant.getBaselineExerciseMinutes(),
                participant.getBaselineSteps(),
                participant.getBaselineDistanceMeters(),
                participant.getBaselineActiveCalories(),
                participant.getBaselineHealthVerifiedQuestCount()
        );
    }

    private boolean battleEligible(UserQuest quest) {
        Map<String, Object> proof = quest.getProofJson();
        Object battleEligible = proof.get("battleEligible");
        if (battleEligible instanceof Boolean value) {
            return value;
        }
        return Boolean.TRUE.equals(proof.get("verified"));
    }

    private BattleHealthStats loadSyncedHealthStats(Long userId, BattlePeriod period) {
        List<HealthDailySummary> summaries = healthDailySummaryRepository.findByUserIdAndSummaryDateBetween(
                userId,
                period.startDate(),
                period.endDate()
        );
        int activeMinutes = 0;
        int steps = 0;
        int distanceMeters = 0;
        int activeCalories = 0;
        for (HealthDailySummary summary : summaries) {
            activeMinutes += summary.getExerciseMinutes() == null ? 0 : summary.getExerciseMinutes();
            steps += summary.getSteps() == null ? 0 : summary.getSteps();
            distanceMeters += summary.getDistanceMeters() == null ? 0 : summary.getDistanceMeters();
            activeCalories += summary.getActiveCaloriesKcal() == null ? 0 : summary.getActiveCaloriesKcal();
        }
        return new BattleHealthStats(activeMinutes, steps, distanceMeters, activeCalories, !summaries.isEmpty());
    }

    private List<BattleMetricResponse> metrics(BattleStats myStats, BattleStats opponentStats) {
        return List.of(
                metric("TOTAL_SCORE", "배틀 점수", myStats.totalScore(), opponentStats.totalScore(), "점"),
                metric("ACTIVE_MINUTES", "운동 시간", myStats.activeMinutes(), opponentStats.activeMinutes(), "분"),
                metric("DISTANCE", "이동 거리", myStats.distanceMeters(), opponentStats.distanceMeters(), "m"),
                metric("STEPS", "걸음 수", myStats.steps(), opponentStats.steps(), "걸음"),
                metric("ACTIVE_CALORIES", "활동 칼로리", myStats.activeCalories(), opponentStats.activeCalories(), "kcal"),
                metric("COMPLETED_QUESTS", "완료 퀘스트", myStats.completedQuestCount(), opponentStats.completedQuestCount(), "개")
        );
    }

    private BattleMetricResponse metric(String key, String label, int myValue, int opponentValue, String unit) {
        int max = Math.max(Math.max(myValue, opponentValue), 1);
        return new BattleMetricResponse(
                key,
                label,
                myValue + unit,
                Math.round((myValue * 100.0f) / max),
                opponentValue + unit,
                Math.round((opponentValue * 100.0f) / max),
                unit
        );
    }

    private BattleResult resultFor(int score, int otherScore) {
        if (score > otherScore) {
            return BattleResult.WIN;
        }
        if (score < otherScore) {
            return BattleResult.LOSS;
        }
        return BattleResult.DRAW;
    }

    private Long leadingUserId(BattleSnapshot snapshot) {
        if (snapshot.myStats().totalScore() == snapshot.opponentStats().totalScore()) {
            return null;
        }
        return snapshot.myStats().totalScore() > snapshot.opponentStats().totalScore()
                ? snapshot.me().getUser().getId()
                : snapshot.opponent().getUser().getId();
    }

    private Long winnerUserId(BattleSnapshot snapshot) {
        return leadingUserId(snapshot);
    }

    private long remainingSeconds(Battle battle) {
        return remainingSeconds(battle.getEndsAt());
    }

    private long remainingSeconds(Instant endsAt) {
        return Math.max(0, Duration.between(KoreanTime.nowInstant(), endsAt).toSeconds());
    }

    private BattleCurrentSummaryResponse toCurrentSummary(Battle battle) {
        return new BattleCurrentSummaryResponse(
                battle.getId(),
                battle.getMode(),
                battle.getStatus(),
                battle.getPeriodStartDate(),
                battle.getPeriodEndDate(),
                battle.getEndsAt()
        );
    }

    private String rankName(long wins) {
        if (wins >= 30) {
            return "Platinum";
        }
        if (wins >= 15) {
            return "Gold";
        }
        if (wins >= 5) {
            return "Silver";
        }
        if (wins > 0) {
            return "Bronze";
        }
        return "Unranked";
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private record BattlePeriod(BattleMode mode, LocalDate startDate, LocalDate endDate, Instant startsAt, Instant endsAt) {
        static BattlePeriod fromStart(BattleMode mode, Instant startsAt) {
            Duration duration = BattleMode.WEEKLY.equals(mode) ? Duration.ofDays(7) : Duration.ofDays(1);
            Instant endsAt = startsAt.plus(duration);
            return new BattlePeriod(
                    mode,
                    startsAt.atZone(KoreanTime.ZONE_ID).toLocalDate(),
                    endsAt.minusMillis(1).atZone(KoreanTime.ZONE_ID).toLocalDate(),
                    startsAt,
                    endsAt
            );
        }

        static BattlePeriod fromQueue(BattleMatchQueue queue) {
            return new BattlePeriod(
                    queue.getMode(),
                    queue.getPeriodStartDate(),
                    queue.getPeriodEndDate(),
                    queue.getQueuedAt(),
                    queue.getExpiresAt()
            );
        }

        static BattlePeriod fromBattle(Battle battle) {
            return new BattlePeriod(
                    battle.getMode(),
                    battle.getPeriodStartDate(),
                    battle.getPeriodEndDate(),
                    battle.getStartsAt(),
                    battle.getEndsAt()
            );
        }
    }

    private record BattleStats(int totalScore,
                               int completedQuestCount,
                               int routineQuestCount,
                               int activeMinutes,
                               int steps,
                               int distanceMeters,
                               int activeCalories,
                               int healthVerifiedQuestCount) {
        static BattleStats empty() {
            return new BattleStats(0, 0, 0, 0, 0, 0, 0, 0);
        }

        BattleStats minus(BattleStats baseline) {
            return new BattleStats(
                    nonNegative(totalScore - baseline.totalScore()),
                    nonNegative(completedQuestCount - baseline.completedQuestCount()),
                    nonNegative(routineQuestCount - baseline.routineQuestCount()),
                    nonNegative(activeMinutes - baseline.activeMinutes()),
                    nonNegative(steps - baseline.steps()),
                    nonNegative(distanceMeters - baseline.distanceMeters()),
                    nonNegative(activeCalories - baseline.activeCalories()),
                    nonNegative(healthVerifiedQuestCount - baseline.healthVerifiedQuestCount())
            );
        }

        private static int nonNegative(int value) {
            return Math.max(0, value);
        }
    }

    private record BattleHealthStats(int activeMinutes,
                                     int steps,
                                     int distanceMeters,
                                     int activeCalories,
                                     boolean hasData) {
    }

    private record BattleSnapshot(BattleParticipant me,
                                  BattleParticipant opponent,
                                  BattleStats myStats,
                                  BattleStats opponentStats,
                                  List<BattleParticipantResponse> participants) {
    }
}
