package com.capstone.backend.global.health;

import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.global.time.KoreanTime;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "헬스체크", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "서버 상태 확인", description = "백엔드 API 서버가 정상 동작 중인지 확인합니다.")
    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(new HealthResponse("UP", KoreanTime.now()));
    }

    public record HealthResponse(String status, OffsetDateTime timestamp) {
    }
}
