package com.capstone.backend.auth.dto;

public record FindLoginIdResponse(
        String loginId,
        String nickname,
        String matchedBy
) {
}
