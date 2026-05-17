package com.capstone.backend.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyConditionPointResponse(
        LocalDate date,
        String dayLabel,
        BigDecimal condition,
        Integer energy,
        Integer stress,
        String exerciseType,
        String exerciseLabel
) {
}
