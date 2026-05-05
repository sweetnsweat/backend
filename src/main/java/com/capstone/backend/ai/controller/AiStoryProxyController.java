package com.capstone.backend.ai.controller;

import com.capstone.backend.ai.dto.AiStoryGenerateRequest;
import com.capstone.backend.ai.dto.AiStoryPlayHistoryRequest;
import com.capstone.backend.ai.dto.AiStoryPlayRequest;
import com.capstone.backend.ai.dto.AiStoryQuestListRequest;
import com.capstone.backend.ai.dto.AiStoryQuestTodayRequest;
import com.capstone.backend.ai.service.AiProxyService;
import com.capstone.backend.ai.service.AiStoryRequestFactory;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@Tag(name = "AI 스토리", description = "AI 서버의 스토리 생성/진행 API를 백엔드에서 중개합니다.")
@RestController
@RequestMapping("/api")
public class AiStoryProxyController {

    private final AiProxyService aiProxyService;
    private final AiStoryRequestFactory aiStoryRequestFactory;

    public AiStoryProxyController(AiProxyService aiProxyService, AiStoryRequestFactory aiStoryRequestFactory) {
        this.aiProxyService = aiProxyService;
        this.aiStoryRequestFactory = aiStoryRequestFactory;
    }

    @Operation(summary = "AI 서버 상태 확인", description = "백엔드에서 AI 서버 루트 상태 확인 API를 호출합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI 서버 상태 확인 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = AiStorySwaggerExamples.HEALTH_RESPONSE))
    )
    @GetMapping("/ai/health")
    public ApiResponse<Object> health() {
        return ApiResponse.ok("AI 서버 상태 확인에 성공했습니다.", aiProxyService.get("/"));
    }

    @Operation(summary = "AI 스토리 생성", description = "사용자 입력을 바탕으로 시나리오, 챕터, 캐릭터, 선택지를 생성하는 AI API를 호출합니다. AI 서버의 StoryTemplateInput과 동일한 필드명을 사용합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI 스토리 생성 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = AiStorySwaggerExamples.GENERATE_RESPONSE))
    )
    @PostMapping("/stories/generate")
    public ApiResponse<Object> generateStory(@Valid @RequestBody AiStoryGenerateRequest request) {
        return ApiResponse.ok("AI 스토리 생성이 완료되었습니다.", aiProxyService.post("/stories/generate", aiStoryRequestFactory.fromGenerateRequest(request)));
    }

    @Operation(summary = "AI 스토리 플레이 통합 진행", description = "프론트에서 주로 사용할 메인 API입니다. user_id는 요청에 넣지 않고 백엔드가 로그인 사용자 ID를 AI 서버 요청에 주입합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI 스토리 진행 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = AiStorySwaggerExamples.PLAY_RESPONSE))
    )
    @PostMapping("/stories/play")
    public ApiResponse<Object> playStory(@AuthenticationPrincipal AuthUser authUser,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                         @Valid @RequestBody AiStoryPlayRequest request) {
        return ApiResponse.ok("AI 스토리 진행 응답을 조회했습니다.", aiProxyService.post("/stories/play", aiStoryRequestFactory.fromPlayRequest(request, authUser.userId()), authorization));
    }

    @Operation(summary = "AI 스토리 대화 히스토리 조회", description = "프론트 채팅 화면 복원용 API입니다. user_id는 요청에 넣지 않고 백엔드가 로그인 사용자 ID를 AI 서버 요청에 주입합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI 스토리 대화 히스토리 조회 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = AiStorySwaggerExamples.HISTORY_RESPONSE))
    )
    @GetMapping("/stories/play/history")
    public ApiResponse<Object> playHistory(@AuthenticationPrincipal AuthUser authUser,
                                           @Valid @ParameterObject @ModelAttribute AiStoryPlayHistoryRequest request) {
        return ApiResponse.ok("AI 스토리 대화 히스토리를 조회했습니다.", aiProxyService.get(aiStoryRequestFactory.historyPath(request, authUser.userId())));
    }

    @Operation(summary = "AI 세계관 목록 조회", description = "AI 서버의 세계관 선택용 활성 시나리오 카드 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI 세계관 목록 조회 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = AiStorySwaggerExamples.SCENARIOS_RESPONSE))
    )
    @GetMapping("/stories/scenarios")
    public ApiResponse<Object> scenarios() {
        return ApiResponse.ok("AI 세계관 목록을 조회했습니다.", aiProxyService.get("/stories/scenarios"));
    }

    @Operation(summary = "AI 세계관 상세 조회", description = "AI 서버의 시나리오 상세 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI 세계관 상세 조회 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = AiStorySwaggerExamples.SCENARIO_DETAIL_RESPONSE))
    )
    @GetMapping("/stories/scenarios/{scenarioId}")
    public ApiResponse<Object> scenario(
            @Parameter(description = "조회할 시나리오 ID", required = true, example = "4")
            @PathVariable @Positive Long scenarioId
    ) {
        return ApiResponse.ok("AI 세계관 상세를 조회했습니다.", aiProxyService.get(aiStoryRequestFactory.scenarioDetailPath(scenarioId)));
    }

    @Hidden
    @Operation(summary = "AI 스토리 퀘스트 생성/조회", description = "AI 서버의 오늘 스토리 퀘스트 생성/조회 API를 호출합니다. user_id는 요청에 넣지 않고 백엔드가 로그인 사용자 ID를 AI 서버 요청에 주입합니다.")
    @GetMapping("/stories/quests/today")
    public ApiResponse<Object> todayStoryQuest(@AuthenticationPrincipal AuthUser authUser,
                                               @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                               @Valid @ParameterObject @ModelAttribute AiStoryQuestTodayRequest request) {
        return ApiResponse.ok("AI 스토리 퀘스트를 조회했습니다.", aiProxyService.get(aiStoryRequestFactory.storyQuestTodayPath(request, authUser.userId()), authorization));
    }

    @Hidden
    @Operation(summary = "AI 스토리 퀘스트 목록 조회", description = "AI 서버에 저장된 스토리 퀘스트 목록을 조회합니다. user_id는 요청에 넣지 않고 백엔드가 로그인 사용자 ID를 AI 서버 요청에 주입합니다.")
    @GetMapping("/stories/quests")
    public ApiResponse<Object> storyQuests(@AuthenticationPrincipal AuthUser authUser,
                                           @Valid @ParameterObject @ModelAttribute AiStoryQuestListRequest request) {
        return ApiResponse.ok("AI 스토리 퀘스트 목록을 조회했습니다.", aiProxyService.get(aiStoryRequestFactory.storyQuestListPath(request, authUser.userId())));
    }

    @Hidden
    @Operation(summary = "AI 스토리 퀘스트 ID 조회", description = "AI 서버에 저장된 스토리 퀘스트를 ID로 조회합니다.")
    @GetMapping("/stories/quests/{questId}")
    public ApiResponse<Object> storyQuest(
            @Parameter(description = "조회할 스토리 퀘스트 ID", required = true, example = "1")
            @PathVariable @Positive Long questId
    ) {
        return ApiResponse.ok("AI 스토리 퀘스트를 조회했습니다.", aiProxyService.get(aiStoryRequestFactory.storyQuestDetailPath(questId)));
    }

    @Hidden
    @Operation(summary = "AI 스토리 처음부터 시작", description = "기존 진행 상태를 리셋하고 1챕터 처음부터 시작합니다. user_id는 백엔드가 로그인 사용자 ID로 주입합니다.")
    @PostMapping("/stories/play/start")
    public ApiResponse<Object> startStory(@AuthenticationPrincipal AuthUser authUser,
                                          @RequestBody String request) {
        return ApiResponse.ok("AI 스토리를 처음부터 시작했습니다.", aiProxyService.post("/stories/play/start", aiStoryRequestFactory.withAuthenticatedUserId(request, authUser.userId())));
    }

    @Hidden
    @Operation(summary = "AI 스토리 현재 흐름 이어가기", description = "사용자 입력을 반영해 현재 챕터의 다음 흐름을 진행합니다. user_id는 백엔드가 로그인 사용자 ID로 주입합니다.")
    @PostMapping("/stories/play/continue")
    public ApiResponse<Object> continueStory(@AuthenticationPrincipal AuthUser authUser,
                                             @RequestBody String request) {
        return ApiResponse.ok("AI 스토리 현재 흐름을 이어갔습니다.", aiProxyService.post("/stories/play/continue", aiStoryRequestFactory.withAuthenticatedUserId(request, authUser.userId())));
    }

    @Hidden
    @Operation(summary = "AI 스토리 선택지 선택", description = "현재 선택지 대기 상태에서 선택지를 골라 세부 전개를 진행합니다. user_id는 백엔드가 로그인 사용자 ID로 주입합니다.")
    @PostMapping("/stories/play/choose")
    public ApiResponse<Object> chooseStory(@AuthenticationPrincipal AuthUser authUser,
                                           @RequestBody String request) {
        return ApiResponse.ok("AI 스토리 선택지를 반영했습니다.", aiProxyService.post("/stories/play/choose", aiStoryRequestFactory.withAuthenticatedUserId(request, authUser.userId())));
    }

    @Hidden
    @Operation(summary = "AI 스토리 다음 챕터 이동", description = "현재 챕터가 완료된 경우 다음 챕터 도입부로 이동합니다. user_id는 백엔드가 로그인 사용자 ID로 주입합니다.")
    @PostMapping("/stories/play/next-chapter")
    public ApiResponse<Object> nextChapter(@AuthenticationPrincipal AuthUser authUser,
                                           @RequestBody String request) {
        return ApiResponse.ok("AI 스토리 다음 챕터로 이동했습니다.", aiProxyService.post("/stories/play/next-chapter", aiStoryRequestFactory.withAuthenticatedUserId(request, authUser.userId())));
    }
}
