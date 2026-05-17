package com.capstone.backend.statistics.service;

import com.capstone.backend.global.exception.ApiException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.http.HttpStatus;

enum ConditionStatisticsPeriod {
    WEEK("week"),
    MONTH("month"),
    YEAR("year");

    private final String code;

    ConditionStatisticsPeriod(String code) {
        this.code = code;
    }

    static ConditionStatisticsPeriod from(String value) {
        String normalized = value == null ? "week" : value.trim().toLowerCase();
        for (ConditionStatisticsPeriod period : values()) {
            if (period.code.equals(normalized) || period.name().equalsIgnoreCase(normalized)) {
                return period;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATISTICS_PERIOD", "period must be week, month, or year");
    }

    String code() {
        return code;
    }

    LocalDate startDate(LocalDate today) {
        return switch (this) {
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> today.withDayOfMonth(1);
            case YEAR -> today.withDayOfYear(1);
        };
    }

    LocalDate endDate(LocalDate today) {
        return switch (this) {
            case WEEK -> startDate(today).plusDays(6);
            case MONTH -> today.withDayOfMonth(today.lengthOfMonth());
            case YEAR -> today.withDayOfYear(today.lengthOfYear());
        };
    }
}
