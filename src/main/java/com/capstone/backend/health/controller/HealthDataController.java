package com.capstone.backend.health.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.health.dto.HealthDataSyncRequest;
import com.capstone.backend.health.dto.HealthDataSyncResponse;
import com.capstone.backend.health.service.HealthDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "건강 데이터", description = "Health Connect / HealthKit 공통 건강 데이터 API")
@RestController
@RequestMapping("/api/health-data")
public class HealthDataController {

    private final HealthDataService healthDataService;

    public HealthDataController(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    @Operation(summary = "건강 데이터 동기화 요약", description = "Android Health Connect 또는 iOS HealthKit 원본 샘플을 공통 metric으로 정규화하고 타입별 요약을 반환합니다.")
    @PostMapping("/sync")
    public ApiResponse<HealthDataSyncResponse> sync(@AuthenticationPrincipal AuthUser authUser,
                                                    @RequestBody HealthDataSyncRequest request) {
        return ApiResponse.ok("건강 데이터가 동기화되었습니다.", healthDataService.summarize(authUser.userId(), request));
    }
}
