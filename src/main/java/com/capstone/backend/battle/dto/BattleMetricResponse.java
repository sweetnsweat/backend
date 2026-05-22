package com.capstone.backend.battle.dto;

public record BattleMetricResponse(
        String metricKey,
        String label,
        String myValue,
        Integer myPercent,
        String opponentValue,
        Integer opponentPercent,
        String unit
) {
}
