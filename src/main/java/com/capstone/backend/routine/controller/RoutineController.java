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

@Tag(name = "Routines", description = "Workout routine APIs")
@RestController
@RequestMapping("/api/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @Operation(summary = "Get default workout routines")
    @GetMapping("/default")
    public ApiResponse<List<RoutineSummaryResponse>> defaultRoutines() {
        return ApiResponse.ok(routineService.getDefaultRoutines());
    }

    @Operation(summary = "Get workout routine detail")
    @GetMapping("/{routineId}")
    public ApiResponse<RoutineDetailResponse> routine(@PathVariable Long routineId) {
        return ApiResponse.ok(routineService.getRoutine(routineId));
    }
}
