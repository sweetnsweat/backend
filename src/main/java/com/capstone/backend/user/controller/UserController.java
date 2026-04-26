package com.capstone.backend.user.controller;

import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.user.dto.OnboardingProfileRequest;
import com.capstone.backend.user.dto.UpdateActiveRoutineRequest;
import com.capstone.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "User profile APIs")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getMyProfile(authUser.userId()));
    }

    @Operation(summary = "Update current user onboarding profile")
    @PutMapping("/me/onboarding-profile")
    public ApiResponse<UserProfileResponse> updateOnboardingProfile(@AuthenticationPrincipal AuthUser authUser,
                                                                    @Valid @RequestBody OnboardingProfileRequest request) {
        return ApiResponse.ok("Onboarding profile updated", userService.updateOnboardingProfile(authUser.userId(), request));
    }

    @Operation(summary = "Get current user's active routine")
    @GetMapping("/me/routines/active")
    public ApiResponse<RoutineDetailResponse> getActiveRoutine(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getActiveRoutine(authUser.userId()));
    }

    @Operation(summary = "Update current user's active routine")
    @PutMapping("/me/routines/active")
    public ApiResponse<RoutineDetailResponse> updateActiveRoutine(@AuthenticationPrincipal AuthUser authUser,
                                                                  @Valid @RequestBody UpdateActiveRoutineRequest request) {
        return ApiResponse.ok("Active routine updated", userService.updateActiveRoutine(authUser.userId(), request));
    }
}
