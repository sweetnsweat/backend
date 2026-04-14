package com.capstone.backend.global.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        Instant timestamp,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "Request succeeded", Instant.now(), data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "OK", message, Instant.now(), data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, "CREATED", message, Instant.now(), data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, "OK", message, Instant.now(), null);
    }
}
