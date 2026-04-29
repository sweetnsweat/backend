package com.capstone.backend.routine.controller;

import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.dto.RoutineSummaryResponse;
import com.capstone.backend.routine.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운동 루틴", description = "기본 운동 루틴 조회 API")
@RestController
@RequestMapping("/api/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @Operation(summary = "기본 운동 루틴 목록 조회", description = "사용자가 선택할 수 있는 기본 운동 루틴 목록을 조회합니다.")
    @GetMapping("/default")
    public ApiResponse<List<RoutineSummaryResponse>> defaultRoutines() {
        return ApiResponse.ok(routineService.getDefaultRoutines());
    }

    @Operation(summary = "운동 루틴 상세 조회", description = "루틴에 포함된 운동 목록, 세트, 반복 횟수 등 상세 정보를 조회합니다.")
    @GetMapping("/{routineId}")
    public ApiResponse<RoutineDetailResponse> routine(@PathVariable Long routineId) {
        return ApiResponse.ok(routineService.getRoutine(routineId));
    }
}
