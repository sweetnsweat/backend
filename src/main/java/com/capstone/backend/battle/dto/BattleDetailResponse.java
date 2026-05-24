package com.capstone.backend.battle.dto;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BattleDetailResponse(
        Long battleId,
        BattleMode mode,
        BattleStatus status,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Instant startsAt,
        Instant endsAt,
        Long remainingSeconds,
        String matchStatus,
        Instant queuedAt,
        List<BattleParticipantResponse> participants,
        BattleScoreResponse score,
        List<BattleMetricResponse> metrics
) {
}
