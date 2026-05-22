package com.capstone.backend.battle.dto;

public record BattleScoreResponse(
        Integer myScore,
        Integer opponentScore,
        Long leadingUserId
) {
}
