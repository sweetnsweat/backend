package com.capstone.backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserProfileResponse user
) {
}
