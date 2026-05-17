package com.capstone.backend.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyExerciseRecordResponse(
        LocalDate date,
        String dayLabel,
        String exerciseType,
        String exerciseLabel,
        BigDecimal condition,
        Integer energy,
        Integer stress
) {
}
