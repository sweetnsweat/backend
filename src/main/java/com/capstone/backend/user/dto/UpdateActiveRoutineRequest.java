package com.capstone.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveRoutineRequest(
        @NotNull(message = "routineId is required")
        Long routineId
) {
}
