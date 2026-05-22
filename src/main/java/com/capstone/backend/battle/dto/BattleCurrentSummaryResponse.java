package com.capstone.backend.battle.dto;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BattleCurrentSummaryResponse(
        Long battleId,
        BattleMode mode,
        BattleStatus status,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Instant endsAt
) {
}
