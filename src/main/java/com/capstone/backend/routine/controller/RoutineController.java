package com.capstone.backend.routine.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.dto.RoutineRecommendationResponse;
import com.capstone.backend.routine.dto.RoutineSummaryResponse;
import com.capstone.backend.routine.service.RoutineService;
import com.capstone.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운동 루틴", description = "기본 운동 루틴 조회 API")
@RestController
@RequestMapping("/api/routines")
public class RoutineController {

    private final RoutineService routineService;
    private final UserService userService;

    public RoutineController(RoutineService routineService, UserService userService) {
        this.routineService = routineService;
        this.userService = userService;
    }

    @Operation(summary = "기본 운동 루틴 목록 조회", description = "사용자가 선택할 수 있는 기본 운동 루틴 목록을 조회합니다.")
    @GetMapping("/default")
    public ApiResponse<List<RoutineSummaryResponse>> defaultRoutines() {
        return ApiResponse.ok(routineService.getDefaultRoutines());
    }

    @Operation(summary = "온보딩 기반 루틴 추천", description = "현재 로그인한 사용자의 온보딩 값을 기반으로 기본 루틴 1~2개를 추천합니다.")
    @GetMapping("/recommendations")
    public ApiResponse<List<RoutineRecommendationResponse>> recommendations(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(routineService.getRecommendations(authUser.userId()));
    }

    @Operation(summary = "운동 루틴 상세 조회", description = "루틴에 포함된 운동 목록, 세트, 반복 횟수 등 상세 정보를 조회합니다.")
    @GetMapping("/{routineId}")
    public ApiResponse<RoutineDetailResponse> routine(@PathVariable Long routineId) {
        return ApiResponse.ok(routineService.getRoutine(routineId));
    }

    @Operation(summary = "루틴 선택 및 활성화", description = "기본 루틴이면 사용자 전용 루틴으로 복사한 뒤 활성화하고, 사용자 루틴이면 그대로 활성화합니다.")
    @PostMapping("/{routineId}/activate")
    public ApiResponse<RoutineDetailResponse> activateRoutine(@AuthenticationPrincipal AuthUser authUser,
                                                              @PathVariable Long routineId) {
        return ApiResponse.ok("운동 루틴이 사용자 루틴으로 활성화되었습니다.", userService.activateRoutine(authUser.userId(), routineId));
    }
}
