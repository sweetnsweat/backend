package com.capstone.backend.ai.service;

import com.capstone.backend.global.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient.RequestBodySpec;

@Service
public class AiProxyService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiProxyService(@Value("${app.ai.base-url}") String aiBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiBaseUrl)
                .build();
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    public Object get(String path) {
        return forward(HttpMethod.GET, path, null, null);
    }

    public Object get(String path, String authorization) {
        return forward(HttpMethod.GET, path, null, authorization);
    }

    public Object post(String path, String requestBody) {
        return forward(HttpMethod.POST, path, requestBody, null);
    }

    public Object post(String path, String requestBody, String authorization) {
        return forward(HttpMethod.POST, path, requestBody, authorization);
    }

    private Object forward(HttpMethod method, String path, String requestBody, String authorization) {
        RequestBodySpec requestSpec = restClient.method(method)
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        if (StringUtils.hasText(authorization)) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, authorization);
        }

        if (requestBody != null) {
            requestSpec.body(requestBody);
        }

        return requestSpec.exchange((request, response) -> {
            byte[] bodyBytes = response.getBody().readAllBytes();
            Object responseBody = parseBody(bodyBytes);

            if (response.getStatusCode().isError()) {
                String message = extractErrorMessage(responseBody);
                HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
                throw new ApiException(
                        status == null ? HttpStatus.BAD_GATEWAY : status,
                        "AI_SERVER_ERROR",
                        message
                );
            }

            return responseBody;
        });
    }

    private Object parseBody(byte[] bodyBytes) throws IOException {
        if (bodyBytes.length == 0) {
            return Map.of();
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(body, Object.class);
        } catch (IOException exception) {
            return Map.of("rawBody", body);
        }
    }

    private String extractErrorMessage(Object responseBody) {
        if (responseBody instanceof Map<?, ?> responseMap) {
            Object detail = responseMap.get("detail");
            if (detail != null) {
                return detail.toString();
            }
            Object message = responseMap.get("message");
            if (message != null) {
                return message.toString();
            }
            Object rawBody = responseMap.get("rawBody");
            if (rawBody != null) {
                return rawBody.toString();
            }
        }
        return "AI 서버 요청에 실패했습니다.";
    }
}
