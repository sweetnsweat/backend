package com.capstone.backend.global.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class KoreanTime {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    public static final ZoneOffset OFFSET = ZoneOffset.ofHours(9);

    private KoreanTime() {
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZONE_ID);
    }

    public static Instant nowInstant() {
        return now().toInstant();
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }
}
