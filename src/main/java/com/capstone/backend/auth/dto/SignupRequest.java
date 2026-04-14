package com.capstone.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "loginId is required")
        @Size(min = 4, max = 50, message = "loginId must be 4~50 characters")
        String loginId,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8~72 characters")
        String password,

        @NotBlank(message = "nickname is required")
        @Size(min = 2, max = 50, message = "nickname must be 2~50 characters")
        String nickname
) {
}
