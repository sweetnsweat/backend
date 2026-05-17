package com.capstone.backend.statistics.service;

import com.capstone.backend.condition.entity.ConditionLog;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.routine.entity.RoutineSession;
import com.capstone.backend.statistics.dto.AiInsightResponse;
import com.capstone.backend.statistics.dto.ConditionStatisticsResponse;
import com.capstone.backend.statistics.dto.ConditionStatisticsSummaryResponse;
import com.capstone.backend.statistics.dto.DailyConditionPointResponse;
import com.capstone.backend.statistics.dto.ExerciseEffectResponse;
import com.capstone.backend.statistics.dto.WeeklyExerciseRecordResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConditionStatisticsService {

    private static final ExerciseType NONE = new ExerciseType("none", "없음");

    private final ConditionLogRepository conditionLogRepository;
    private final UserQuestRepository userQuestRepository;

    public ConditionStatisticsService(ConditionLogRepository conditionLogRepository,
                                      UserQuestRepository userQuestRepository) {
        this.conditionLogRepository = conditionLogRepository;
        this.userQuestRepository = userQuestRepository;
    }

    @Transactional(readOnly = true)
    public ConditionStatisticsResponse getConditionStatistics(Long userId, String periodValue) {
        ConditionStatisticsPeriod period = ConditionStatisticsPeriod.from(periodValue);
        LocalDate today = KoreanTime.today();
        LocalDate startDate = period.startDate(today);
        LocalDate endDate = period.endDate(today);

        Map<LocalDate, ConditionLog> conditionByDate = conditionLogRepository
                .findByUser_IdAndLogDateBetweenOrderByLogDateAsc(userId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(ConditionLog::getLogDate, conditionLog -> conditionLog, (left, right) -> right, LinkedHashMap::new));

        Map<LocalDate, ExerciseType> exerciseByDate = completedExerciseTypes(userId, startDate, endDate);

        List<DailyConditionPointResponse> trend = buildDailyTrend(startDate, endDate, conditionByDate, exerciseByDate);
        List<ExerciseEffectResponse> exerciseEffects = buildExerciseEffects(trend);

        BigDecimal averageCondition = averageCondition(trend);
        int workoutCount = exerciseByDate.size();
        Integer improvementRate = improvementRate(userId, startDate, endDate, averageCondition);
        AiInsightResponse insight = buildInsight(exerciseEffects);

        List<WeeklyExerciseRecordResponse> weeklyRecords = trend.stream()
                .map(point -> new WeeklyExerciseRecordResponse(
                        point.date(),
                        point.dayLabel(),
                        point.exerciseType(),
                        point.exerciseLabel(),
                        point.condition(),
                        point.energy(),
                        point.stress()
                ))
                .toList();

        return new ConditionStatisticsResponse(
                period.code(),
                startDate,
                endDate,
                trend,
                new ConditionStatisticsSummaryResponse(averageCondition, workoutCount, improvementRate),
                exerciseEffects,
                insight,
                weeklyRecords
        );
    }

    private Map<LocalDate, ExerciseType> completedExerciseTypes(Long userId, LocalDate startDate, LocalDate endDate) {
        return userQuestRepository.findCompletedStatsByUserIdAndQuestDateBetween(userId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        UserQuest::getQuestDate,
                        this::exerciseType,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<DailyConditionPointResponse> buildDailyTrend(LocalDate startDate,
                                                              LocalDate endDate,
                                                              Map<LocalDate, ConditionLog> conditionByDate,
                                                              Map<LocalDate, ExerciseType> exerciseByDate) {
        List<DailyConditionPointResponse> trend = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            ConditionLog conditionLog = conditionByDate.get(date);
            ExerciseType exerciseType = exerciseByDate.getOrDefault(date, NONE);
            trend.add(new DailyConditionPointResponse(
                    date,
                    dayLabel(date),
                    conditionScore(conditionLog),
                    energyScore(conditionLog),
                    stressScore(conditionLog),
                    exerciseType.code(),
                    exerciseType.label()
            ));
        }
        return trend;
    }

    private List<ExerciseEffectResponse> buildExerciseEffects(List<DailyConditionPointResponse> trend) {
        Map<ExerciseType, List<BigDecimal>> grouped = new LinkedHashMap<>();
        for (DailyConditionPointResponse point : trend) {
            if (point.condition() == null) {
                continue;
            }
            ExerciseType exerciseType = new ExerciseType(point.exerciseType(), point.exerciseLabel());
            grouped.computeIfAbsent(exerciseType, ignored -> new ArrayList<>()).add(point.condition());
        }

        return grouped.entrySet()
                .stream()
                .map(entry -> new ExerciseEffectResponse(
                        entry.getKey().code(),
                        entry.getKey().label(),
                        average(entry.getValue()),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(ExerciseEffectResponse::averageCondition, Comparator.reverseOrder())
                        .thenComparing(ExerciseEffectResponse::displayName))
                .toList();
    }

    private Integer improvementRate(Long userId, LocalDate startDate, LocalDate endDate, BigDecimal currentAverage) {
        if (currentAverage == null) {
            return null;
        }
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        LocalDate previousStartDate = startDate.minusDays(days);
        LocalDate previousEndDate = startDate.minusDays(1);

        List<BigDecimal> previousScores = conditionLogRepository
                .findByUser_IdAndLogDateBetweenOrderByLogDateAsc(userId, previousStartDate, previousEndDate)
                .stream()
                .map(this::conditionScore)
                .filter(Objects::nonNull)
                .toList();
        BigDecimal previousAverage = average(previousScores);
        if (previousAverage == null || previousAverage.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentAverage.subtract(previousAverage)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousAverage, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private AiInsightResponse buildInsight(List<ExerciseEffectResponse> exerciseEffects) {
        Optional<ExerciseEffectResponse> bestExercise = exerciseEffects.stream()
                .filter(effect -> !"none".equals(effect.exerciseType()))
                .findFirst();
        Optional<ExerciseEffectResponse> noExercise = exerciseEffects.stream()
                .filter(effect -> "none".equals(effect.exerciseType()))
                .findFirst();

        if (bestExercise.isEmpty()) {
            return new AiInsightResponse(
                    "AI 분석 인사이트",
                    "아직 운동별 컨디션 차이를 판단할 만큼 완료된 운동 기록이 부족합니다.",
                    "컨디션 입력과 퀘스트 완료 기록이 쌓이면 잘 맞는 운동 유형을 추천해드릴게요."
            );
        }

        ExerciseEffectResponse best = bestExercise.get();
        String comparison = noExercise
                .map(effect -> {
                    Integer diffPercent = percentDifference(best.averageCondition(), effect.averageCondition());
                    if (diffPercent == null) {
                        return "운동을 하지 않은 날과의 차이는 기록이 더 쌓이면 계산할 수 있어요.";
                    }
                    return "운동을 하지 않은 날(" + formatScore(effect.averageCondition()) + "점)보다 "
                            + Math.abs(diffPercent) + "% " + (diffPercent >= 0 ? "더 높은" : "더 낮은") + " 수치입니다.";
                })
                .orElse("운동을 하지 않은 날의 비교 데이터는 아직 부족합니다.");

        return new AiInsightResponse(
                "AI 분석 인사이트",
                "최근 " + best.displayName() + "을 했을 때 평균 컨디션이 "
                        + formatScore(best.averageCondition()) + "점으로 가장 높았어요. " + comparison,
                "앞으로도 " + best.displayName() + "처럼 컨디션 반응이 좋은 운동을 중심으로 추천해드릴게요."
        );
    }

    private Integer percentDifference(BigDecimal current, BigDecimal baseline) {
        if (current == null || baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(baseline)
                .multiply(BigDecimal.valueOf(100))
                .divide(baseline, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private ExerciseType exerciseType(UserQuest quest) {
        Map<String, Object> context = quest.getQuestContextJson();
        Optional<String> contextValue = firstText(context, "exerciseType", "exerciseLabel", "exerciseCategory", "sessionType", "sessionName");
        if (contextValue.isPresent()) {
            return normalizeExerciseType(contextValue.get());
        }

        RoutineSession session = quest.getSourceSession();
        if (session == null) {
            return new ExerciseType("exercise", "운동");
        }

        String exerciseText = session.getItems()
                .stream()
                .map(RoutineItem::getExercise)
                .filter(Objects::nonNull)
                .map(this::exerciseText)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElseGet(() -> firstNonBlank(session.getSessionType(), session.getSessionName()).orElse("운동"));

        return normalizeExerciseType(exerciseText);
    }

    private String exerciseText(Exercise exercise) {
        return firstNonBlank(exercise.getCategory(), exercise.getName()).orElse("");
    }

    private Optional<String> firstText(Map<String, Object> context, String... keys) {
        for (String key : keys) {
            Object value = context.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return Optional.of(text);
            }
        }
        return Optional.empty();
    }

    private Optional<String> firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private ExerciseType normalizeExerciseType(String value) {
        String text = value == null ? "" : value.trim();
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.equals("none") || normalized.equals("off_day") || normalized.contains("없")) {
            return NONE;
        }
        if (normalized.contains("수영") || normalized.contains("swim")) {
            return new ExerciseType("swimming", "수영");
        }
        if (normalized.contains("홈트") || normalized.contains("home")) {
            return new ExerciseType("home_training", "홈트");
        }
        if (normalized.contains("요가") || normalized.contains("yoga") || normalized.contains("stretch")) {
            return new ExerciseType("yoga", "요가");
        }
        if (normalized.contains("조깅") || normalized.contains("러닝") || normalized.contains("run") || normalized.contains("jog") || normalized.contains("cardio")) {
            return new ExerciseType("running", "조깅");
        }
        return new ExerciseType(toCode(normalized), text);
    }

    private String toCode(String value) {
        String code = value.replaceAll("[^a-z0-9가-힣]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return code.isBlank() ? "exercise" : code;
    }

    private BigDecimal conditionScore(ConditionLog conditionLog) {
        if (conditionLog == null || conditionLog.getConditionScore() == null) {
            return null;
        }
        return conditionLog.getConditionScore()
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
    }

    private Integer energyScore(ConditionLog conditionLog) {
        if (conditionLog == null || conditionLog.getEnergyLevel() == null) {
            return null;
        }
        return conditionLog.getEnergyLevel() * 2;
    }

    private Integer stressScore(ConditionLog conditionLog) {
        return conditionLog == null ? null : conditionLog.getStressScore();
    }

    private BigDecimal averageCondition(List<DailyConditionPointResponse> trend) {
        List<BigDecimal> scores = trend.stream()
                .map(DailyConditionPointResponse::condition)
                .filter(Objects::nonNull)
                .toList();
        return average(scores);
    }

    private BigDecimal average(List<BigDecimal> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "-" : score.stripTrailingZeros().toPlainString();
    }

    private String dayLabel(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    private record ExerciseType(String code, String label) {
    }
}
