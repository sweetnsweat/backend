package com.capstone.backend.ai.controller;

import com.capstone.backend.ai.service.AiProxyService;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 스토리", description = "AI 서버의 스토리 생성/진행 API를 백엔드에서 중개합니다.")
@RestController
@RequestMapping("/api")
public class AiStoryProxyController {

    private final AiProxyService aiProxyService;

    public AiStoryProxyController(AiProxyService aiProxyService) {
        this.aiProxyService = aiProxyService;
    }

    @Operation(summary = "AI 서버 상태 확인", description = "백엔드에서 AI 서버 루트 상태 확인 API를 호출합니다.")
    @GetMapping("/ai/health")
    public ApiResponse<Object> health() {
        return ApiResponse.ok("AI 서버 상태 확인에 성공했습니다.", aiProxyService.get("/"));
    }

    @Operation(summary = "AI 스토리 생성", description = "사용자 입력을 바탕으로 시나리오, 챕터, 캐릭터, 선택지를 생성하는 AI API를 호출합니다.")
    @PostMapping("/stories/generate")
    public ApiResponse<Object> generateStory(@RequestBody String request) {
        return ApiResponse.ok("AI 스토리 생성이 완료되었습니다.", aiProxyService.post("/stories/generate", request));
    }

    @Operation(summary = "AI 스토리 플레이 통합 진행", description = "프론트에서 주로 사용할 메인 API입니다. 시작, 대화, 선택지 선택, 다음 챕터 진행을 AI 서버에 중개합니다.")
    @PostMapping("/stories/play")
    public ApiResponse<Object> playStory(@RequestBody String request) {
        return ApiResponse.ok("AI 스토리 진행 응답을 조회했습니다.", aiProxyService.post("/stories/play", request));
    }

    @Operation(summary = "AI 스토리 처음부터 시작", description = "기존 진행 상태를 리셋하고 1챕터 처음부터 시작하는 AI API를 호출합니다.")
    @PostMapping("/stories/play/start")
    public ApiResponse<Object> startStory(@RequestBody String request) {
        return ApiResponse.ok("AI 스토리를 처음부터 시작했습니다.", aiProxyService.post("/stories/play/start", request));
    }

    @Operation(summary = "AI 스토리 현재 흐름 이어가기", description = "사용자 입력을 반영해 현재 챕터의 다음 흐름을 진행하는 AI API를 호출합니다.")
    @PostMapping("/stories/play/continue")
    public ApiResponse<Object> continueStory(@RequestBody String request) {
        return ApiResponse.ok("AI 스토리 현재 흐름을 이어갔습니다.", aiProxyService.post("/stories/play/continue", request));
    }

    @Operation(summary = "AI 스토리 선택지 선택", description = "현재 선택지 대기 상태에서 선택지를 골라 세부 전개를 진행하는 AI API를 호출합니다.")
    @PostMapping("/stories/play/choose")
    public ApiResponse<Object> chooseStory(@RequestBody String request) {
        return ApiResponse.ok("AI 스토리 선택지를 반영했습니다.", aiProxyService.post("/stories/play/choose", request));
    }

    @Operation(summary = "AI 스토리 다음 챕터 이동", description = "현재 챕터가 완료된 경우 다음 챕터 도입부로 이동하는 AI API를 호출합니다.")
    @PostMapping("/stories/play/next-chapter")
    public ApiResponse<Object> nextChapter(@RequestBody String request) {
        return ApiResponse.ok("AI 스토리 다음 챕터로 이동했습니다.", aiProxyService.post("/stories/play/next-chapter", request));
    }
}
