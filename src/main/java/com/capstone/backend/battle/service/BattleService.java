package com.capstone.backend.battle.service;

import com.capstone.backend.battle.dto.BattleCurrentSummaryResponse;
import com.capstone.backend.battle.dto.BattleDetailResponse;
import com.capstone.backend.battle.dto.BattleHistoryItemResponse;
import com.capstone.backend.battle.dto.BattleHistoryPageResponse;
import com.capstone.backend.battle.dto.BattleMetricResponse;
import com.capstone.backend.battle.dto.BattleParticipantResponse;
import com.capstone.backend.battle.dto.BattleResultResponse;
import com.capstone.backend.battle.dto.BattleScoreResponse;
import com.capstone.backend.battle.dto.BattleSummaryResponse;
import com.capstone.backend.battle.entity.Battle;
import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.battle.entity.BattleResult;
import com.capstone.backend.battle.entity.BattleStatus;
import com.capstone.backend.battle.repository.BattleParticipantRepository;
import com.capstone.backend.battle.repository.BattleRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.notification.service.NotificationService;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.reward.policy.BattleRewardPolicy;
import com.capstone.backend.reward.policy.BattleRewardPolicy.BattleReward;
import com.capstone.backend.reward.service.RewardService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
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

    private final BattleRepository battleRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RewardService rewardService;

    public BattleService(BattleRepository battleRepository,
                         BattleParticipantRepository battleParticipantRepository,
                         UserQuestRepository userQuestRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         RewardService rewardService) {
        this.battleRepository = battleRepository;
        this.battleParticipantRepository = battleParticipantRepository;
        this.userQuestRepository = userQuestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.rewardService = rewardService;
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
        BattlePeriod period = periodFor(mode);

        Optional<Battle> currentBattle = findCurrentBattle(userId, mode, period);
        if (currentBattle.isPresent()) {
            return toDetail(currentBattle.get(), userId);
        }

        User opponent = chooseOpponent(userId, mode, period);
        Battle battle = battleRepository.save(Battle.create(
                mode,
                period.startDate(),
                period.endDate(),
                period.startsAt(),
                period.endsAt()
        ));
        List<BattleParticipant> participants = battleParticipantRepository.saveAll(List.of(
                BattleParticipant.join(battle, currentUser),
                BattleParticipant.join(battle, opponent)
        ));
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
        BattlePeriod period = periodFor(mode);
        return findCurrentBattle(userId, mode, period);
    }

    private Optional<Battle> findCurrentBattle(Long userId, BattleMode mode, BattlePeriod period) {
        return battleRepository.findCurrentBattlesForUser(
                userId,
                mode,
                period.startDate(),
                period.endDate(),
                List.of(BattleStatus.ACTIVE),
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    private User chooseOpponent(Long userId, BattleMode mode, BattlePeriod period) {
        List<User> candidates = userRepository.findAvailableBattleOpponents(
                userId,
                mode,
                period.startDate(),
                period.endDate()
        );
        if (candidates.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_OPPONENT_NOT_FOUND", "매칭 가능한 상대가 없습니다.");
        }
        List<User> candidatesWithScoredRecords = candidates.stream()
                .filter(candidate -> loadStats(candidate.getId(), period).totalScore() > 0)
                .toList();
        List<User> candidatesWithCompletedQuests = candidates.stream()
                .filter(candidate -> loadStats(candidate.getId(), period).completedQuestCount() > 0)
                .toList();
        List<User> opponentPool = !candidatesWithScoredRecords.isEmpty()
                ? candidatesWithScoredRecords
                : candidatesWithCompletedQuests.isEmpty() ? candidates : candidatesWithCompletedQuests;
        return opponentPool.get(ThreadLocalRandom.current().nextInt(opponentPool.size()));
    }

    private void finalizeBattle(Battle battle) {
        List<BattleParticipant> participants = battleParticipantRepository.findByBattle_IdOrderByIdAsc(battle.getId());
        if (participants.size() != 2) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_PARTICIPANT_INVALID", "배틀 참여자 구성이 올바르지 않습니다.");
        }

        BattlePeriod period = BattlePeriod.fromBattle(battle);
        BattleParticipant first = participants.get(0);
        BattleParticipant second = participants.get(1);
        BattleStats firstStats = loadStats(first.getUser().getId(), period);
        BattleStats secondStats = loadStats(second.getUser().getId(), period);

        BattleResult firstResult = resultFor(firstStats.totalScore(), secondStats.totalScore());
        BattleResult secondResult = resultFor(secondStats.totalScore(), firstStats.totalScore());
        first.finalizeResult(firstStats.totalScore(), firstResult);
        second.finalizeResult(secondStats.totalScore(), secondResult);
        battle.finalizeBattle(KoreanTime.nowInstant());
        issueWinRewards(battle, participants);
        notificationService.sendBattleResultReady(battle, participants);
    }

    private void issueWinRewards(Battle battle, List<BattleParticipant> participants) {
        BattleReward reward = BattleRewardPolicy.win(battle.getMode());
        participants.stream()
                .filter(participant -> BattleResult.WIN.equals(participant.getResult()))
                .forEach(participant -> rewardService.issueBattleWinReward(participant.getUser(), battle.getId(), reward));
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
                snapshot.participants(),
                new BattleScoreResponse(snapshot.myStats().totalScore(), snapshot.opponentStats().totalScore(), leadingUserId(snapshot)),
                metrics(snapshot.myStats(), snapshot.opponentStats())
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
                metrics(snapshot.myStats(), snapshot.opponentStats())
        );
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
                opponent.getUser().getProfileImageUrl(),
                false,
                opponent.getFinalScore(),
                opponent.getResult()
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
                .map(BattleParticipant::getUser)
                .collect(Collectors.toMap(User::getId, user -> loadStats(user.getId(), period)));
        BattleStats myStats = statsByUserId.get(me.getUser().getId());
        BattleStats opponentStats = statsByUserId.get(opponent.getUser().getId());
        List<BattleParticipantResponse> participantResponses = participants.stream()
                .sorted(Comparator.comparing(participant -> !participant.getUser().getId().equals(currentUserId)))
                .map(participant -> {
                    BattleStats stats = statsByUserId.get(participant.getUser().getId());
                    return new BattleParticipantResponse(
                            participant.getUser().getId(),
                            participant.getUser().getNickname(),
                            participant.getUser().getProfileImageUrl(),
                            participant.getUser().getId().equals(currentUserId),
                            battle.isFinalized() && participant.getFinalScore() != null ? participant.getFinalScore() : stats.totalScore(),
                            battle.isFinalized() ? participant.getResult() : BattleResult.PENDING
                    );
                })
                .toList();
        return new BattleSnapshot(me, opponent, myStats, opponentStats, participantResponses);
    }

    private BattleStats loadStats(Long userId, BattlePeriod period) {
        List<UserQuest> quests = userQuestRepository.findCompletedBattleQuests(
                userId,
                period.startDate(),
                period.endDate()
        );
        List<UserQuest> battleEligibleQuests = quests.stream()
                .filter(this::battleEligible)
                .toList();
        int completedQuestCount = quests.size();
        int battleEligibleQuestCount = battleEligibleQuests.size();
        int routineQuestCount = (int) quests.stream()
                .filter(quest -> UserQuest.TYPE_ROUTINE.equals(quest.getQuestType()))
                .count();

        int activeMinutes = 0;
        int steps = 0;
        int distanceMeters = 0;
        int activeCalories = 0;
        int healthVerifiedQuestCount = 0;

        for (UserQuest quest : battleEligibleQuests) {
            Map<String, Object> proof = quest.getProofJson();
            if (Boolean.TRUE.equals(proof.get("verified"))) {
                healthVerifiedQuestCount++;
            }
            Object metricsValue = proof.get("metrics");
            if (!(metricsValue instanceof Map<?, ?> metrics)) {
                continue;
            }
            activeMinutes += decimal(metrics.get("exerciseMinutes")).setScale(0, RoundingMode.HALF_UP).intValue();
            steps += decimal(metrics.get("steps")).setScale(0, RoundingMode.HALF_UP).intValue();
            distanceMeters += decimal(metrics.get("distanceMeters")).setScale(0, RoundingMode.HALF_UP).intValue();
            activeCalories += decimal(metrics.get("activeCaloriesKcal")).setScale(0, RoundingMode.HALF_UP).intValue();
        }

        int totalScore = battleEligibleQuestCount * QUEST_COMPLETION_POINTS
                + healthVerifiedQuestCount * HEALTH_VERIFIED_POINTS
                + activeMinutes * 10
                + Math.round(distanceMeters * 0.03f)
                + Math.round(steps * 0.01f)
                + activeCalories * 2;
        return new BattleStats(totalScore, completedQuestCount, routineQuestCount, activeMinutes, steps, distanceMeters, activeCalories, healthVerifiedQuestCount);
    }

    private boolean battleEligible(UserQuest quest) {
        Map<String, Object> proof = quest.getProofJson();
        Object battleEligible = proof.get("battleEligible");
        if (battleEligible instanceof Boolean value) {
            return value;
        }
        return Boolean.TRUE.equals(proof.get("verified"));
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
        return Math.max(0, Duration.between(KoreanTime.nowInstant(), battle.getEndsAt()).toSeconds());
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

    private BattlePeriod periodFor(BattleMode mode) {
        LocalDate today = KoreanTime.today();
        if (BattleMode.WEEKLY.equals(mode)) {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            return BattlePeriod.fromDates(weekStart, weekEnd);
        }
        return BattlePeriod.fromDates(today, today);
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

    private record BattlePeriod(LocalDate startDate, LocalDate endDate, Instant startsAt, Instant endsAt) {
        static BattlePeriod fromDates(LocalDate startDate, LocalDate endDate) {
            return new BattlePeriod(
                    startDate,
                    endDate,
                    startDate.atStartOfDay(KoreanTime.ZONE_ID).toInstant(),
                    endDate.plusDays(1).atStartOfDay(KoreanTime.ZONE_ID).toInstant()
            );
        }

        static BattlePeriod fromBattle(Battle battle) {
            return new BattlePeriod(
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
    }

    private record BattleSnapshot(BattleParticipant me,
                                  BattleParticipant opponent,
                                  BattleStats myStats,
                                  BattleStats opponentStats,
                                  List<BattleParticipantResponse> participants) {
    }
}
