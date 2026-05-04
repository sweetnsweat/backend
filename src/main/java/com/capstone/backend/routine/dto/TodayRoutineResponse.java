package com.capstone.backend.routine.dto;

import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineSession;
import java.time.LocalDate;

public record TodayRoutineResponse(
        LocalDate date,
        String dayOfWeek,
        String dayOfWeekDisplayName,
        Boolean activeRoutineExists,
        Boolean routineScheduledToday,
        RoutineSummaryResponse routine,
        RoutineDetailResponse.RoutineSessionResponse session
) {
    public static TodayRoutineResponse noActiveRoutine(LocalDate date) {
        return new TodayRoutineResponse(
                date,
                date.getDayOfWeek().name(),
                displayDayOfWeek(date.getDayOfWeek().name()),
                false,
                false,
                null,
                null
        );
    }

    public static TodayRoutineResponse offDay(LocalDate date, Routine routine) {
        return new TodayRoutineResponse(
                date,
                date.getDayOfWeek().name(),
                displayDayOfWeek(date.getDayOfWeek().name()),
                true,
                false,
                RoutineSummaryResponse.from(routine, true),
                null
        );
    }

    public static TodayRoutineResponse scheduled(LocalDate date, Routine routine, RoutineSession session) {
        return new TodayRoutineResponse(
                date,
                date.getDayOfWeek().name(),
                displayDayOfWeek(date.getDayOfWeek().name()),
                true,
                true,
                RoutineSummaryResponse.from(routine, true),
                RoutineDetailResponse.RoutineSessionResponse.from(session)
        );
    }

    private static String displayDayOfWeek(String dayOfWeek) {
        return switch (dayOfWeek == null ? "" : dayOfWeek) {
            case "MONDAY" -> "월요일";
            case "TUESDAY" -> "화요일";
            case "WEDNESDAY" -> "수요일";
            case "THURSDAY" -> "목요일";
            case "FRIDAY" -> "금요일";
            case "SATURDAY" -> "토요일";
            case "SUNDAY" -> "일요일";
            default -> dayOfWeek;
        };
    }
}
