package com.capstone.backend.exercise.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.exercise.dto.ExerciseFavoriteResponse;
import com.capstone.backend.exercise.dto.ExerciseListResponse;
import com.capstone.backend.exercise.dto.UpdateExerciseFavoriteRequest;
import com.capstone.backend.exercise.service.ExerciseService;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내 운동", description = "내 운동 즐겨찾기 API")
@RestController
@RequestMapping("/api/users/me/exercises")
public class UserExerciseController {

    private final ExerciseService exerciseService;

    public UserExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @Operation(summary = "내 즐겨찾기 운동 목록 조회", description = "로그인 사용자가 즐겨찾기한 운동 목록을 페이지 단위로 조회합니다. 모바일 무한스크롤 기준 기본 size는 30이며, category, level, keyword로 필터링할 수 있습니다.")
    @GetMapping("/favorites")
    public ApiResponse<ExerciseListResponse> favoriteExercises(@AuthenticationPrincipal AuthUser authUser,
                                                               @RequestParam(required = false) String category,
                                                               @RequestParam(required = false) String level,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(exerciseService.getFavoriteExercises(authUser.userId(), category, level, keyword, page, size));
    }

    @Operation(summary = "운동 즐겨찾기 설정", description = "운동 목록에서 특정 운동을 즐겨찾기에 추가하거나 해제합니다.")
    @PutMapping("/{exerciseId}/favorite")
    public ApiResponse<ExerciseFavoriteResponse> updateFavorite(@AuthenticationPrincipal AuthUser authUser,
                                                                @PathVariable Long exerciseId,
                                                                @Valid @RequestBody UpdateExerciseFavoriteRequest request) {
        String message = Boolean.TRUE.equals(request.liked()) ? "운동이 즐겨찾기에 추가되었습니다." : "운동 즐겨찾기가 해제되었습니다.";
        return ApiResponse.ok(message, exerciseService.updateFavorite(authUser.userId(), exerciseId, request.liked()));
    }
}
