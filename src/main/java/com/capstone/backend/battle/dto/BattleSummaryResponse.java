package com.capstone.backend.battle.dto;

public record BattleSummaryResponse(
        String rankName,
        Long wins,
        Long losses,
        Long draws,
        Integer winRate,
        BattleCurrentSummaryResponse currentDailyBattle,
        BattleCurrentSummaryResponse currentWeeklyBattle
) {
}
