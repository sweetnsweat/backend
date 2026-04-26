package com.capstone.backend.condition.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.condition.dto.ConditionLogResponse;
import com.capstone.backend.condition.dto.ConditionTodayRequest;
import com.capstone.backend.condition.service.ConditionService;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Conditions", description = "Daily condition APIs")
@RestController
@RequestMapping("/api/conditions")
public class ConditionController {

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    @Operation(summary = "Get today's condition")
    @GetMapping("/today")
    public ApiResponse<ConditionLogResponse> getTodayCondition(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(conditionService.getTodayCondition(authUser.userId()));
    }

    @Operation(summary = "Create or update today's condition")
    @PutMapping("/today")
    public ApiResponse<ConditionLogResponse> updateTodayCondition(@AuthenticationPrincipal AuthUser authUser,
                                                                  @Valid @RequestBody ConditionTodayRequest request) {
        return ApiResponse.ok("Today's condition updated", conditionService.updateTodayCondition(authUser.userId(), request));
    }
}
