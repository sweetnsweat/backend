package com.capstone.backend.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OnboardingProfileRequest(
        @NotBlank(message = "gender is required")
        @Pattern(regexp = "male|female|other|prefer_not_to_say", message = "gender is invalid")
        String gender,

        @NotNull(message = "birthDate is required")
        @Past(message = "birthDate must be in the past")
        LocalDate birthDate,

        @NotNull(message = "heightCm is required")
        @DecimalMin(value = "50.0", message = "heightCm must be at least 50.0")
        @DecimalMax(value = "250.0", message = "heightCm must be at most 250.0")
        BigDecimal heightCm,

        @NotNull(message = "weightKg is required")
        @DecimalMin(value = "20.0", message = "weightKg must be at least 20.0")
        @DecimalMax(value = "300.0", message = "weightKg must be at most 300.0")
        BigDecimal weightKg,

        @NotBlank(message = "experienceLevel is required")
        @Pattern(regexp = "beginner|intermediate|advanced", message = "experienceLevel is invalid")
        String experienceLevel,

        @NotEmpty(message = "preferredExerciseTypes is required")
        @Size(max = 10, message = "preferredExerciseTypes must have at most 10 items")
        List<@Pattern(
                regexp = "strength|cardio|stretching|bodyweight|walking",
                message = "preferredExerciseTypes contains an invalid value"
        ) String> preferredExerciseTypes
) {
}
