package com.capstone.backend.ai.service;

import com.capstone.backend.ai.dto.AiStoryGenerateRequest;
import com.capstone.backend.ai.dto.AiStoryPlayHistoryRequest;
import com.capstone.backend.ai.dto.AiStoryPlayRequest;
import com.capstone.backend.global.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AiStoryRequestFactory {

    private final ObjectMapper objectMapper;

    public AiStoryRequestFactory() {
        this.objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build()
                .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
    }

    public String withAuthenticatedUserId(String requestBody, Long userId) {
        ObjectNode payload = parseObject(requestBody);
        payload.put("user_id", userId);
        return writeJson(payload);
    }

    public String fromPlayRequest(AiStoryPlayRequest request, Long userId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", userId);
        payload.put("scenario_id", request.scenarioId());
        if (request.userMessage() != null) {
            payload.put("user_message", request.userMessage());
        }
        if (request.choiceId() != null) {
            payload.put("choice_id", request.choiceId());
        }
        if (request.restart() != null) {
            payload.put("restart", request.restart());
        }
        return writeJson(payload);
    }

    public String fromGenerateRequest(AiStoryGenerateRequest request) {
        return writeJson(request);
    }

    public String historyPath(AiStoryPlayHistoryRequest request, Long userId) {
        return UriComponentsBuilder.fromPath("/stories/play/history")
                .queryParam("user_id", userId)
                .queryParam("scenario_id", request.getScenarioId())
                .queryParam("limit", request.resolvedLimit())
                .queryParam("offset", request.resolvedOffset())
                .build()
                .toUriString();
    }

    private ObjectNode parseObject(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode node = objectMapper.readTree(requestBody);
            if (!node.isObject()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AI_STORY_REQUEST", "AI 스토리 요청 본문은 JSON 객체여야 합니다.");
            }
            return (ObjectNode) node;
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AI_STORY_REQUEST", "AI 스토리 요청 JSON 형식이 올바르지 않습니다.");
        }
    }

    private String writeJson(ObjectNode payload) {
        return writeJson((Object) payload);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI_STORY_REQUEST_SERIALIZE_FAILED", "AI 스토리 요청을 생성하지 못했습니다.");
        }
    }
}
