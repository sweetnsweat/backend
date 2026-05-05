package com.capstone.backend.auth.dto;

public record NicknameAvailabilityResponse(
        String nickname,
        boolean available,
        boolean duplicated
) {
    public static NicknameAvailabilityResponse of(String nickname, boolean duplicated) {
        return new NicknameAvailabilityResponse(nickname, !duplicated, duplicated);
    }
}
