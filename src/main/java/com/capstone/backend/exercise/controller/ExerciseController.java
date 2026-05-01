package com.capstone.backend.exercise.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.exercise.dto.ExerciseCategoryResponse;
import com.capstone.backend.exercise.dto.ExerciseDetailResponse;
import com.capstone.backend.exercise.dto.ExerciseListResponse;
import com.capstone.backend.exercise.service.ExerciseService;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운동 목록", description = "운동 카탈로그 조회 API")
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @Operation(summary = "운동 카테고리 조회", description = "운동 목록 필터에 사용할 종목 카테고리를 조회합니다.")
    @GetMapping("/categories")
    public ApiResponse<List<ExerciseCategoryResponse>> categories() {
        return ApiResponse.ok(exerciseService.getCategories());
    }

    @Operation(summary = "운동 목록 조회", description = "운동 목록 화면에 필요한 그룹형 카드 목록을 조회합니다. scope는 all, favorite, recent를 지원합니다.")
    @GetMapping
    public ApiResponse<ExerciseListResponse> exercises(@AuthenticationPrincipal AuthUser authUser,
                                                       @RequestParam(defaultValue = "all") String scope,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String level,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(exerciseService.getExercises(authUser.userId(), scope, category, level, keyword, page, size));
    }

    @Operation(summary = "운동 상세 조회", description = "운동 상세 정보와 현재 사용자의 즐겨찾기 여부를 조회합니다.")
    @GetMapping("/{exerciseId}")
    public ApiResponse<ExerciseDetailResponse> exercise(@AuthenticationPrincipal AuthUser authUser,
                                                        @PathVariable Long exerciseId) {
        return ApiResponse.ok(exerciseService.getExercise(authUser.userId(), exerciseId));
    }
}
