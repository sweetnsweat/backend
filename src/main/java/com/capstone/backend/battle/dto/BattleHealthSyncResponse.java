package com.capstone.backend.battle.dto;

import java.time.Instant;

public record BattleHealthSyncResponse(
        boolean required,
        boolean recommended,
        Instant latestSyncedAt,
        Instant windowStart,
        Instant windowEnd,
        long staleAfterSeconds
) {
}
