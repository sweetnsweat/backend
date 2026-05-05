package com.capstone.backend.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileSettingsRequest(
        @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다.")
        String nickname,

        @Size(max = 1000, message = "프로필 이미지 URL은 1000자 이하여야 합니다.")
        String profileImageUrl
) {
}
