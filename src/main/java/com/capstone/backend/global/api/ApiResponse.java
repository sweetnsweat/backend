package com.capstone.backend.global.api;

import com.capstone.backend.global.time.KoreanTime;
import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        OffsetDateTime timestamp,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "Request succeeded", KoreanTime.now(), data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "OK", message, KoreanTime.now(), data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, "CREATED", message, KoreanTime.now(), data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, "OK", message, KoreanTime.now(), null);
    }
}
