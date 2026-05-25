package com.capstone.backend.stats.service;

import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.health.entity.HealthDailySummary;
import com.capstone.backend.health.repository.HealthDailySummaryRepository;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.stats.dto.RecordStatsPeriod;
import com.capstone.backend.stats.dto.RecordStatsResponse;
import com.capstone.backend.stats.dto.RecordStatsResponse.ConditionTrendPoint;
import com.capstone.backend.stats.dto.RecordStatsResponse.DailyRecord;
import com.capstone.backend.stats.dto.RecordStatsResponse.ExerciseEffect;
import com.capstone.backend.stats.dto.RecordStatsResponse.Insight;
import com.capstone.backend.stats.dto.RecordStatsResponse.RecordStatsSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordStatsService {

    private final ConditionLogRepository conditionLogRepository;
    private final HealthDailySummaryRepository healthDailySummaryRepository;
    private final UserQuestRepository userQuestRepository;

    public RecordStatsService(ConditionLogRepository conditionLogRepository,
                              HealthDailySummaryRepository healthDailySummaryRepository,
                              UserQuestRepository userQuestRepository) {
        this.conditionLogRepository = conditionLogRepository;
        this.healthDailySummaryRepository = healthDailySummaryRepository;
        this.userQuestRepository = userQuestRepository;
    }

    @Transactional(readOnly = true)
    public RecordStatsResponse getStats(Long userId, RecordStatsPeriod period) {
        RecordStatsPeriod effectivePeriod = period == null ? RecordStatsPeriod.WEEKLY : period;
        DateRange range = dateRange(effectivePeriod);
        Map<LocalDate, ConditionLog> conditionsByDate = conditionLogRepository
                .findByUser_IdAndLogDateBetweenOrderByLogDateAsc(userId, range.startDate(), range.endDate())
                .stream()
                .collect(Collectors.toMap(ConditionLog::getLogDate, Function.identity(), (left, right) -> right, LinkedHashMap::new));
        Map<LocalDate, HealthDailySummary> healthByDate = healthDailySummaryRepository
                .findByUserIdAndSummaryDateBetween(userId, range.startDate(), range.endDate())
                .stream()
                .collect(Collectors.toMap(HealthDailySummary::getSummaryDate, Function.identity(), (left, right) -> right, LinkedHashMap::new));
        List<UserQuest> completedQuests = userQuestRepository.findCompletedStatsByUserIdAndQuestDateBetween(userId, range.startDate(), range.endDate());
        Map<LocalDate, List<UserQuest>> questsByDate = completedQuests.stream()
                .collect(Collectors.groupingBy(UserQuest::getQuestDate, LinkedHashMap::new, Collectors.toList()));

        List<LocalDate> dates = range.dates();
        List<ConditionTrendPoint> conditionTrend = conditionTrend(effectivePeriod, range, dates, conditionsByDate, healthByDate, questsByDate);
        List<DailyRecord> dailyRecords = dailyRecords(effectivePeriod, range, dates, conditionsByDate, healthByDate, questsByDate);
        List<ExerciseEffect> exerciseEffects = exerciseEffects(completedQuests, conditionsByDate, healthByDate, dates);
        RecordStatsSummary summary = summary(conditionsByDate, healthByDate, completedQuests, questsByDate);
        Insight insight = insight(summary, exerciseEffects);

        return new RecordStatsResponse(
                effectivePeriod,
                range.startDate(),
                range.endDate(),
                summary,
                conditionTrend,
                exerciseEffects,
                dailyRecords,
                insight
        );
    }

    private DateRange dateRange(RecordStatsPeriod period) {
        LocalDate today = KoreanTime.today();
        return switch (period) {
            case WEEKLY -> new DateRange(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today
            );
            case MONTHLY -> new DateRange(
                    today.withDayOfMonth(1),
                    today
            );
            case YEARLY -> new DateRange(
                    today.withDayOfYear(1),
                    today
            );
        };
    }

    private List<ConditionTrendPoint> conditionTrend(RecordStatsPeriod period,
                                                     DateRange range,
                                                     List<LocalDate> dates,
                                                     Map<LocalDate, ConditionLog> conditionsByDate,
                                                     Map<LocalDate, HealthDailySummary> healthByDate,
                                                     Map<LocalDate, List<UserQuest>> questsByDate) {
        if (period != RecordStatsPeriod.YEARLY) {
            return dates.stream()
                    .map(date -> trendPoint(date, conditionsByDate.get(date), healthByDate.get(date), questsByDate.getOrDefault(date, List.of()).size()))
                    .toList();
        }
        return monthRanges(range).stream()
                .map(monthRange -> monthlyTrendPoint(monthRange, conditionsByDate, healthByDate, questsByDate))
                .toList();
    }

    private List<DailyRecord> dailyRecords(RecordStatsPeriod period,
                                           DateRange range,
                                           List<LocalDate> dates,
                                           Map<LocalDate, ConditionLog> conditionsByDate,
                                           Map<LocalDate, HealthDailySummary> healthByDate,
                                           Map<LocalDate, List<UserQuest>> questsByDate) {
        if (period != RecordStatsPeriod.YEARLY) {
            return dates.stream()
                    .map(date -> dailyRecord(date, conditionsByDate.get(date), healthByDate.get(date), questsByDate.getOrDefault(date, List.of())))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return monthRanges(range).stream()
                .map(monthRange -> monthlyRecord(monthRange, conditionsByDate, healthByDate, questsByDate))
                .filter(Objects::nonNull)
                .toList();
    }

    private ConditionTrendPoint trendPoint(LocalDate date,
                                           ConditionLog condition,
                                           HealthDailySummary health,
                                           int completedQuestCount) {
        return new ConditionTrendPoint(
                date,
                labelFor(date),
                conditionLevel(condition),
                condition == null ? null : conditionScoreForStats(condition.getConditionScore()),
                energyLevel(condition),
                condition == null ? null : condition.getStressScore(),
                value(health == null ? null : health.getExerciseMinutes()),
                value(health == null ? null : health.getSteps()),
                value(health == null ? null : health.getDistanceMeters()),
                value(health == null ? null : health.getActiveCaloriesKcal()),
                completedQuestCount
        );
    }

    private DailyRecord dailyRecord(LocalDate date,
                                    ConditionLog condition,
                                    HealthDailySummary health,
                                    List<UserQuest> quests) {
        if (quests.isEmpty() && value(health == null ? null : health.getExerciseMinutes()) <= 0) {
            return null;
        }
        return new DailyRecord(
                date,
                dayOfWeekLabel(date.getDayOfWeek()),
                exerciseLabel(quests, health),
                conditionLevel(condition),
                condition == null ? null : conditionScoreForStats(condition.getConditionScore()),
                energyLevel(condition),
                condition == null ? null : condition.getStressScore(),
                value(health == null ? null : health.getExerciseMinutes()),
                value(health == null ? null : health.getSteps()),
                value(health == null ? null : health.getActiveCaloriesKcal()),
                quests.size()
        );
    }

    private ConditionTrendPoint monthlyTrendPoint(DateRange monthRange,
                                                  Map<LocalDate, ConditionLog> conditionsByDate,
                                                  Map<LocalDate, HealthDailySummary> healthByDate,
                                                  Map<LocalDate, List<UserQuest>> questsByDate) {
        List<ConditionLog> conditions = conditionsInRange(monthRange, conditionsByDate);
        List<HealthDailySummary> healthSummaries = healthInRange(monthRange, healthByDate);
        List<UserQuest> quests = questsInRange(monthRange, questsByDate);
        return new ConditionTrendPoint(
                monthRange.startDate(),
                monthLabel(monthRange.startDate()),
                roundedAverageInteger(conditions.stream().map(this::conditionLevel).toList()),
                averageConditionScoreForStats(conditions.stream().map(ConditionLog::getConditionScore).toList()),
                roundedAverageInteger(conditions.stream().map(this::energyLevel).toList()),
                roundedAverageInteger(conditions.stream().map(ConditionLog::getStressScore).toList()),
                healthSummaries.stream().mapToInt(summary -> value(summary.getExerciseMinutes())).sum(),
                healthSummaries.stream().mapToInt(summary -> value(summary.getSteps())).sum(),
                healthSummaries.stream().mapToInt(summary -> value(summary.getDistanceMeters())).sum(),
                healthSummaries.stream().mapToInt(summary -> value(summary.getActiveCaloriesKcal())).sum(),
                quests.size()
        );
    }

    private DailyRecord monthlyRecord(DateRange monthRange,
                                      Map<LocalDate, ConditionLog> conditionsByDate,
                                      Map<LocalDate, HealthDailySummary> healthByDate,
                                      Map<LocalDate, List<UserQuest>> questsByDate) {
        List<ConditionLog> conditions = conditionsInRange(monthRange, conditionsByDate);
        List<HealthDailySummary> healthSummaries = healthInRange(monthRange, healthByDate);
        List<UserQuest> quests = questsInRange(monthRange, questsByDate);
        if (quests.isEmpty() && healthSummaries.stream().mapToInt(summary -> value(summary.getExerciseMinutes())).sum() <= 0) {
            return null;
        }
        return new DailyRecord(
                monthRange.startDate(),
                monthLabel(monthRange.startDate()),
                exerciseLabel(quests, healthSummaries),
                roundedAverageInteger(conditions.stream().map(this::conditionLevel).toList()),
                averageConditionScoreForStats(conditions.stream().map(ConditionLog::getConditionScore).toList()),
                roundedAverageInteger(conditions.stream().map(this::energyLevel).toList()),
                roundedAverageInteger(conditions.stream().map(ConditionLog::getStressScore).toList()),
                healthSummaries.stream().mapToInt(summary -> value(summary.getExerciseMinutes())).sum(),
                healthSummaries.stream().mapToInt(summary -> value(summary.getSteps())).sum(),
                healthSummaries.stream().mapToInt(summary -> value(summary.getActiveCaloriesKcal())).sum(),
                quests.size()
        );
    }

    private List<DateRange> monthRanges(DateRange range) {
        List<DateRange> monthRanges = new ArrayList<>();
        LocalDate current = range.startDate().withDayOfMonth(1);
        while (!current.isAfter(range.endDate())) {
            LocalDate endOfMonth = current.withDayOfMonth(current.lengthOfMonth());
            monthRanges.add(new DateRange(current, endOfMonth));
            current = current.plusMonths(1);
        }
        return monthRanges;
    }

    private List<ConditionLog> conditionsInRange(DateRange range, Map<LocalDate, ConditionLog> conditionsByDate) {
        return range.dates().stream()
                .map(conditionsByDate::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<HealthDailySummary> healthInRange(DateRange range, Map<LocalDate, HealthDailySummary> healthByDate) {
        return range.dates().stream()
                .map(healthByDate::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<UserQuest> questsInRange(DateRange range, Map<LocalDate, List<UserQuest>> questsByDate) {
        return range.dates().stream()
                .flatMap(date -> questsByDate.getOrDefault(date, List.of()).stream())
                .toList();
    }

    private RecordStatsSummary summary(Map<LocalDate, ConditionLog> conditionsByDate,
                                       Map<LocalDate, HealthDailySummary> healthByDate,
                                       List<UserQuest> completedQuests,
                                       Map<LocalDate, List<UserQuest>> questsByDate) {
        List<ConditionLog> conditions = new ArrayList<>(conditionsByDate.values());
        int healthOnlyExerciseDays = (int) healthByDate.entrySet().stream()
                .filter(entry -> value(entry.getValue().getExerciseMinutes()) > 0)
                .filter(entry -> !questsByDate.containsKey(entry.getKey()))
                .count();
        int totalExerciseMinutes = healthByDate.values().stream().mapToInt(summary -> value(summary.getExerciseMinutes())).sum();
        int totalSteps = healthByDate.values().stream().mapToInt(summary -> value(summary.getSteps())).sum();
        int totalDistanceMeters = healthByDate.values().stream().mapToInt(summary -> value(summary.getDistanceMeters())).sum();
        int totalActiveCaloriesKcal = healthByDate.values().stream().mapToInt(summary -> value(summary.getActiveCaloriesKcal())).sum();

        return new RecordStatsSummary(
                averageConditionScoreForStats(conditions.stream().map(ConditionLog::getConditionScore).toList()),
                averageInteger(conditions.stream().map(this::conditionLevel).filter(Objects::nonNull).toList()),
                averageInteger(conditions.stream().map(this::energyLevel).filter(Objects::nonNull).toList()),
                averageInteger(conditions.stream().map(ConditionLog::getStressScore).filter(Objects::nonNull).toList()),
                completedQuests.size() + healthOnlyExerciseDays,
                completedQuests.size(),
                healthByDate.size(),
                improvementRatePercent(conditions),
                totalExerciseMinutes,
                totalSteps,
                totalDistanceMeters,
                totalActiveCaloriesKcal
        );
    }

    private List<ExerciseEffect> exerciseEffects(List<UserQuest> completedQuests,
                                                 Map<LocalDate, ConditionLog> conditionsByDate,
                                                 Map<LocalDate, HealthDailySummary> healthByDate,
                                                 List<LocalDate> dates) {
        Map<String, ExerciseBucket> buckets = new LinkedHashMap<>();
        for (UserQuest quest : completedQuests) {
            ExerciseKey exerciseKey = exerciseKey(quest);
            ExerciseBucket bucket = buckets.computeIfAbsent(exerciseKey.key(), key -> new ExerciseBucket(exerciseKey.key(), exerciseKey.label()));
            ConditionLog condition = conditionsByDate.get(quest.getQuestDate());
            HealthDailySummary health = healthByDate.get(quest.getQuestDate());
            bucket.add(
                    condition == null ? null : condition.getConditionScore(),
                    previousConditionDelta(quest.getQuestDate(), conditionsByDate),
                    condition == null ? null : condition.getStressScore(),
                    health == null ? targetMinutes(quest) : value(health.getExerciseMinutes())
            );
        }

        return buckets.values().stream()
                .map(ExerciseBucket::toResponse)
                .sorted(Comparator
                        .comparing(ExerciseEffect::completedCount, Comparator.reverseOrder())
                        .thenComparing(effect -> nullToZero(effect.averageConditionScore()), Comparator.reverseOrder()))
                .toList();
    }

    private BigDecimal previousConditionDelta(LocalDate date, Map<LocalDate, ConditionLog> conditionsByDate) {
        ConditionLog current = conditionsByDate.get(date);
        if (current == null) {
            return null;
        }
        ConditionLog previous = conditionsByDate.get(date.minusDays(1));
        if (previous == null) {
            return null;
        }
        return current.getConditionScore().subtract(previous.getConditionScore());
    }

    private Insight insight(RecordStatsSummary summary, List<ExerciseEffect> exerciseEffects) {
        Optional<ExerciseEffect> bestExercise = exerciseEffects.stream()
                .filter(effect -> !"none".equals(effect.exerciseType()))
                .filter(effect -> effect.averageConditionScore() != null)
                .max(Comparator.comparing(ExerciseEffect::averageConditionScore));
        if (summary.averageConditionScore() == null) {
            return new Insight(
                    "분석할 기록이 부족합니다.",
                    "컨디션 기록이나 건강 데이터가 쌓이면 기간별 변화와 운동별 효과를 계산할 수 있습니다.",
                    "오늘 컨디션을 입력하고 운동 후 건강 데이터를 동기화해 주세요."
            );
        }
        if (bestExercise.isPresent()) {
            ExerciseEffect effect = bestExercise.get();
            return new Insight(
                    "분석 인사이트",
                    "최근 기록에서는 " + asExerciseName(effect.label()) + "을 한 날의 평균 컨디션이 "
                            + effect.averageConditionScore() + "점으로 가장 좋았습니다.",
                    "비슷한 컨디션의 날에는 " + asExerciseName(effect.label())
                            + "을 먼저 고려해 보세요. 스트레스가 높은 날에는 강도를 낮추고 스트레칭이나 회복 운동으로 마무리하는 편이 좋습니다."
            );
        }
        return new Insight(
                "분석 인사이트",
                "이번 기간 평균 컨디션은 " + summary.averageConditionScore() + "점입니다.",
                "운동 완료 기록과 건강 데이터가 함께 쌓이면 운동별 효과를 더 안정적으로 비교할 수 있습니다."
        );
    }

    private ExerciseKey exerciseKey(UserQuest quest) {
        Map<String, Object> context = quest.getQuestContextJson();
        String direct = firstText(
                context.get("exerciseName"),
                context.get("recommendedExerciseName"),
                context.get("exerciseCategory"),
                context.get("category"),
                context.get("recommendedAction")
        );
        if (hasText(direct)) {
            return keyAndLabel(direct);
        }
        Object exercises = context.get("exercises");
        if (exercises instanceof List<?> exerciseList && !exerciseList.isEmpty() && exerciseList.getFirst() instanceof Map<?, ?> firstExercise) {
            String nested = firstText(
                    firstExercise.get("category"),
                    firstExercise.get("exerciseName"),
                    firstExercise.get("name"),
                    firstExercise.get("type")
            );
            if (hasText(nested)) {
                return keyAndLabel(nested);
            }
        }
        String exerciseType = firstText(context.get("exerciseType"));
        if (hasText(exerciseType)) {
            return keyAndLabel(exerciseType);
        }
        if (quest.getSourceSession() != null) {
            for (RoutineItem item : quest.getSourceSession().getItems()) {
                Exercise exercise = item.getExercise();
                if (exercise != null) {
                    String value = firstNonBlank(exercise.getCategory(), exercise.getName());
                    if (hasText(value)) {
                        return keyAndLabel(value);
                    }
                }
            }
        }
        String fromSession = Optional.ofNullable(quest.getSourceSession())
                .map(session -> firstNonBlank(session.getSessionType(), session.getSessionName()))
                .orElse(null);
        if (hasText(fromSession)) {
            return keyAndLabel(fromSession);
        }
        return keyAndLabel(quest.getQuestType());
    }

    private String exerciseLabel(List<UserQuest> quests) {
        if (quests.isEmpty()) {
            return "없음";
        }
        Set<String> seen = new HashSet<>();
        return quests.stream()
                .map(quest -> exerciseKey(quest).label())
                .filter(seen::add)
                .collect(Collectors.joining(", "));
    }

    private String exerciseLabel(List<UserQuest> quests, HealthDailySummary health) {
        if (!quests.isEmpty()) {
            return exerciseLabel(quests);
        }
        return value(health == null ? null : health.getExerciseMinutes()) > 0 ? "운동" : "";
    }

    private String exerciseLabel(List<UserQuest> quests, List<HealthDailySummary> healthSummaries) {
        if (!quests.isEmpty()) {
            return exerciseLabel(quests);
        }
        int exerciseMinutes = healthSummaries.stream().mapToInt(summary -> value(summary.getExerciseMinutes())).sum();
        return exerciseMinutes > 0 ? "운동" : "";
    }

    private ExerciseKey keyAndLabel(String rawValue) {
        String normalized = rawValue == null ? "unknown" : rawValue.trim().toLowerCase(Locale.ROOT);
        String key = normalized.replaceAll("[^a-z0-9가-힣]+", "_").replaceAll("^_+|_+$", "");
        if (!hasText(key)) {
            key = "unknown";
        }
        return new ExerciseKey(key, displayLabel(normalized, rawValue));
    }

    private String displayLabel(String normalized, String rawValue) {
        if (normalized.contains("full_body") || normalized.contains("full body") || normalized.contains("전신")) {
            return "전신운동";
        }
        if (normalized.contains("upper_body") || normalized.contains("upper body") || normalized.contains("상체")) {
            return "상체운동";
        }
        if (normalized.contains("lower_body") || normalized.contains("lower body") || normalized.contains("하체")) {
            return "하체운동";
        }
        if (normalized.contains("bodyweight") || normalized.contains("calisthenics") || normalized.contains("맨몸")) {
            return "맨몸운동";
        }
        if (normalized.contains("strength") || normalized.contains("weight") || normalized.contains("근력")) {
            return "근력운동";
        }
        if (normalized.contains("cardio") || normalized.contains("aerobic") || normalized.contains("유산소")) {
            return "유산소";
        }
        if (normalized.contains("running") || normalized.contains("run") || normalized.contains("조깅") || normalized.contains("러닝")) {
            return "러닝";
        }
        if (normalized.contains("walking") || normalized.contains("walk") || normalized.contains("걷기")) {
            return "걷기";
        }
        if (normalized.contains("yoga") || normalized.contains("요가")) {
            return "요가";
        }
        if (normalized.contains("swim") || normalized.contains("수영")) {
            return "수영";
        }
        if (normalized.contains("recovery") || normalized.contains("stretch") || normalized.contains("회복") || normalized.contains("스트레칭")) {
            return "회복운동";
        }
        if (UserQuest.TYPE_OFF_DAY.equals(normalized)) {
            return "가벼운 활동";
        }
        if (UserQuest.TYPE_RECOVERY.equals(normalized)) {
            return "회복운동";
        }
        if (UserQuest.TYPE_ROUTINE.equals(normalized)) {
            return "루틴 운동";
        }
        return rawValue == null || rawValue.isBlank() ? "운동" : rawValue.trim();
    }

    private String asExerciseName(String text) {
        String label = hasText(text) ? text.trim() : "운동";
        return label.endsWith("운동") ? label : label + " 운동";
    }

    private int targetMinutes(UserQuest quest) {
        if (UserQuest.METRIC_MINUTES.equals(quest.getTargetMetric())) {
            return value(quest.getTargetValue());
        }
        return 0;
    }

    private Integer improvementRatePercent(List<ConditionLog> conditions) {
        if (conditions.size() < 2) {
            return 0;
        }
        int midpoint = conditions.size() / 2;
        BigDecimal firstAverage = averageBigDecimal(conditions.subList(0, midpoint).stream().map(ConditionLog::getConditionScore).toList());
        BigDecimal secondAverage = averageBigDecimal(conditions.subList(midpoint, conditions.size()).stream().map(ConditionLog::getConditionScore).toList());
        if (firstAverage == null || firstAverage.compareTo(BigDecimal.ZERO) == 0 || secondAverage == null) {
            return 0;
        }
        return secondAverage.subtract(firstAverage)
                .multiply(BigDecimal.valueOf(100))
                .divide(firstAverage, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal averageBigDecimal(List<BigDecimal> values) {
        List<BigDecimal> filtered = values.stream().filter(Objects::nonNull).toList();
        if (filtered.isEmpty()) {
            return null;
        }
        BigDecimal sum = filtered.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(filtered.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal averageInteger(List<Integer> values) {
        List<Integer> filtered = values.stream().filter(Objects::nonNull).toList();
        if (filtered.isEmpty()) {
            return null;
        }
        int sum = filtered.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(filtered.size()), 1, RoundingMode.HALF_UP);
    }

    private Integer roundedAverageInteger(List<Integer> values) {
        List<Integer> filtered = values.stream().filter(Objects::nonNull).toList();
        if (filtered.isEmpty()) {
            return null;
        }
        int sum = filtered.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(filtered.size()), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal averageConditionScoreForStats(List<BigDecimal> values) {
        return conditionScoreForStats(averageBigDecimal(values));
    }

    private static BigDecimal conditionScoreForStats(BigDecimal value) {
        return value == null ? null : value.divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer conditionLevel(ConditionLog conditionLog) {
        if (conditionLog == null) {
            return null;
        }
        if (conditionLog.getConditionLevel() != null) {
            return conditionLog.getConditionLevel();
        }
        BigDecimal conditionScore = conditionLog.getConditionScore();
        if (conditionScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return 5;
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return 4;
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return 3;
        }
        if (conditionScore.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return 2;
        }
        return 1;
    }

    private Integer energyLevel(ConditionLog conditionLog) {
        if (conditionLog == null) {
            return null;
        }
        if (conditionLog.getEnergyLevel() != null) {
            return conditionLog.getEnergyLevel();
        }
        return 6 - conditionLog.getFatigueScore();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String labelFor(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    private String dayOfWeekLabel(DayOfWeek dayOfWeek) {
        return labelFor(LocalDate.of(2024, 1, 1).with(TemporalAdjusters.nextOrSame(dayOfWeek)));
    }

    private String monthLabel(LocalDate date) {
        return date.getMonthValue() + "월";
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value instanceof String text && hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates() {
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dates.add(current);
                current = current.plusDays(1);
            }
            return dates;
        }
    }

    private record ExerciseKey(String key, String label) {
    }

    private static final class ExerciseBucket {
        private final String key;
        private final String label;
        private int count;
        private int exerciseMinutes;
        private final List<BigDecimal> conditionScores = new ArrayList<>();
        private final List<BigDecimal> conditionDeltas = new ArrayList<>();
        private final List<Integer> stressScores = new ArrayList<>();

        private ExerciseBucket(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private void add(BigDecimal conditionScore, BigDecimal conditionDelta, Integer stressScore, int exerciseMinutes) {
            this.count++;
            this.exerciseMinutes += exerciseMinutes;
            if (conditionScore != null) {
                this.conditionScores.add(conditionScore);
            }
            if (conditionDelta != null) {
                this.conditionDeltas.add(conditionDelta);
            }
            if (stressScore != null) {
                this.stressScores.add(stressScore);
            }
        }

        private ExerciseEffect toResponse() {
            return new ExerciseEffect(
                    key,
                    label,
                    count,
                    exerciseMinutes,
                    conditionScoreForStats(averageBigDecimalStatic(conditionScores)),
                    conditionScoreForStats(averageBigDecimalStatic(conditionDeltas)),
                    averageIntegerStatic(stressScores)
            );
        }

        private static BigDecimal averageBigDecimalStatic(List<BigDecimal> values) {
            if (values.isEmpty()) {
                return null;
            }
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
        }

        private static BigDecimal averageIntegerStatic(List<Integer> values) {
            if (values.isEmpty()) {
                return null;
            }
            int sum = values.stream().mapToInt(Integer::intValue).sum();
            return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
        }
    }
}
