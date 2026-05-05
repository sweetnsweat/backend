package com.capstone.backend.user.controller;

import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.dto.RoutineSummaryResponse;
import com.capstone.backend.user.dto.MyPageResponse;
import com.capstone.backend.user.dto.OnboardingProfileRequest;
import com.capstone.backend.user.dto.UpdateActiveRoutineRequest;
import com.capstone.backend.user.dto.UpdateProfileSettingsRequest;
import com.capstone.backend.user.dto.UpdateUserInfoRequest;
import com.capstone.backend.user.dto.WeeklyStatsResponse;
import com.capstone.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "내 프로필, 온보딩, 활성 루틴 설정 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 기본 정보와 온보딩 설정값을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getMyProfile(authUser.userId()));
    }

    @Operation(summary = "마이페이지 조회", description = "프로필, 레벨/경험치, 보유 재화, 활성 루틴, 이번 주 운동 통계를 한 번에 조회합니다.")
    @GetMapping("/me/mypage")
    public ApiResponse<MyPageResponse> myPage(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getMyPage(authUser.userId()));
    }

    @Operation(summary = "온보딩 프로필 저장", description = "최초 로그인 온보딩에서 입력한 신체 정보와 루틴 추천용 운동 성향을 저장합니다.")
    @PutMapping("/me/onboarding-profile")
    public ApiResponse<UserProfileResponse> updateOnboardingProfile(@AuthenticationPrincipal AuthUser authUser,
                                                                    @Valid @RequestBody OnboardingProfileRequest request) {
        return ApiResponse.ok("온보딩 프로필이 저장되었습니다.", userService.updateOnboardingProfile(authUser.userId(), request));
    }

    @Operation(summary = "사용자 정보 수정", description = "마이페이지에서 닉네임, 이메일, 휴대전화 번호 같은 계정 정보를 수정합니다.")
    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateUserInfo(@AuthenticationPrincipal AuthUser authUser,
                                                           @Valid @RequestBody UpdateUserInfoRequest request) {
        return ApiResponse.ok("사용자 정보가 수정되었습니다.", userService.updateUserInfo(authUser.userId(), request));
    }

    @Operation(summary = "프로필 설정", description = "마이페이지 프로필 카드에 표시할 닉네임과 프로필 이미지 URL을 설정합니다.")
    @PutMapping("/me/profile")
    public ApiResponse<UserProfileResponse> updateProfileSettings(@AuthenticationPrincipal AuthUser authUser,
                                                                  @Valid @RequestBody UpdateProfileSettingsRequest request) {
        return ApiResponse.ok("프로필 설정이 저장되었습니다.", userService.updateProfileSettings(authUser.userId(), request));
    }

    @Operation(summary = "활성 운동 루틴 조회", description = "사용자가 현재 선택한 운동 루틴을 상세 조회합니다.")
    @GetMapping("/me/routines/active")
    public ApiResponse<RoutineDetailResponse> getActiveRoutine(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getActiveRoutine(authUser.userId()));
    }

    @Operation(summary = "내 운동 루틴 목록 조회", description = "현재 사용자가 직접 만들었거나 추천 루틴을 활성화하며 복사된 사용자 루틴 목록을 조회합니다.")
    @GetMapping("/me/routines")
    public ApiResponse<List<RoutineSummaryResponse>> getMyRoutines(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getMyRoutines(authUser.userId()));
    }

    @Operation(summary = "이번 주 운동 통계 조회", description = "KST 기준 이번 주 월요일부터 일요일까지 완료한 운동 횟수, 최대 연속 달성일, 예상 소모 칼로리, 획득 경험치를 조회합니다.")
    @GetMapping("/me/weekly-stats")
    public ApiResponse<WeeklyStatsResponse> getWeeklyStats(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getWeeklyStats(authUser.userId()));
    }

    @Operation(summary = "활성 운동 루틴 설정", description = "사용자가 사용할 운동 루틴을 활성 루틴으로 설정합니다.")
    @PutMapping("/me/routines/active")
    public ApiResponse<RoutineDetailResponse> updateActiveRoutine(@AuthenticationPrincipal AuthUser authUser,
                                                                  @Valid @RequestBody UpdateActiveRoutineRequest request) {
        return ApiResponse.ok("활성 운동 루틴이 설정되었습니다.", userService.updateActiveRoutine(authUser.userId(), request));
    }
}
