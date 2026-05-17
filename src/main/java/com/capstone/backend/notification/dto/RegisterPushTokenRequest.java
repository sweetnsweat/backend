package com.capstone.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterPushTokenRequest(
        @NotBlank
        @Size(max = 4096)
        String token,

        @NotBlank
        @Pattern(regexp = "(?i)android|ios", message = "platform must be android or ios")
        String platform,

        @Size(max = 100)
        String deviceId
) {
}
