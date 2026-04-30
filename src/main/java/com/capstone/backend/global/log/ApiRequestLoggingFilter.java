package com.capstone.backend.global.log;

import com.capstone.backend.auth.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final String API_PREFIX = "/api/";
    private static final int MAX_PAYLOAD_LENGTH = 2_000;
    private static final int REQUEST_CACHE_LIMIT = 10_000;

    private final boolean enabled;

    public ApiRequestLoggingFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long startedAt = System.nanoTime();
        Exception failure = null;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            writeAccessLog(wrappedRequest, wrappedResponse, durationMs, failure);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void writeAccessLog(HttpServletRequest request,
                                HttpServletResponse response,
                                long durationMs,
                                Exception failure) {
        String failurePart = failure == null ? "" : " exception=" + failure.getClass().getSimpleName();
        String detailPart = failureDetail(request, response, failure);

        log.info("API_REQUEST method={} path={} status={} durationMs={} client={} user={}{}{}",
                request.getMethod(),
                pathWithQuery(request),
                response.getStatus(),
                durationMs,
                clientIp(request),
                currentUser(),
                failurePart,
                detailPart);
    }

    private String failureDetail(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Exception failure) {
        if (response.getStatus() < 400 && failure == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String requestBody = cachedBody(request);
        if (StringUtils.hasText(requestBody)) {
            builder.append(" requestBody=").append(requestBody);
        }

        String responseBody = cachedBody(response);
        if (StringUtils.hasText(responseBody)) {
            builder.append(" responseBody=").append(responseBody);
            builder.append(" debugHint=responseBody.detail_or_errors_are_validation_requirements");
        }

        return builder.toString();
    }

    private String pathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        if (!StringUtils.hasText(query)) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + maskSensitiveQuery(query);
    }

    private String maskSensitiveQuery(String query) {
        String[] parts = query.split("&");
        for (int i = 0; i < parts.length; i++) {
            int separatorIndex = parts[i].indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = parts[i].substring(0, separatorIndex);
            if (isSensitiveKey(key)) {
                parts[i] = key + "=***";
            }
        }
        return String.join("&", parts);
    }

    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return lowerKey.contains("password")
                || lowerKey.contains("token")
                || lowerKey.contains("secret")
                || lowerKey.contains("authorization")
                || lowerKey.contains("credential");
    }

    private String cachedBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return "";
        }
        return payload(wrapper.getContentAsByteArray(), request.getCharacterEncoding(), request.getContentType());
    }

    private String cachedBody(HttpServletResponse response) {
        if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
            return "";
        }
        return payload(wrapper.getContentAsByteArray(), response.getCharacterEncoding(), response.getContentType());
    }

    private String payload(byte[] content, String characterEncoding, String contentType) {
        if (content.length == 0 || !isReadableContentType(contentType)) {
            return "";
        }

        Charset charset = charset(contentType, characterEncoding);
        String payload = new String(content, charset)
                .replaceAll("\\s+", " ")
                .trim();

        payload = maskSensitiveValues(payload);
        if (payload.length() > MAX_PAYLOAD_LENGTH) {
            return payload.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
        }
        return payload;
    }

    private boolean isReadableContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }

        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        return lowerContentType.contains("json")
                || lowerContentType.contains("text")
                || lowerContentType.contains("xml")
                || lowerContentType.contains("form-urlencoded");
    }

    private Charset charset(String contentType, String characterEncoding) {
        if (StringUtils.hasText(contentType)) {
            String lowerContentType = contentType.toLowerCase(Locale.ROOT);
            int charsetIndex = lowerContentType.indexOf("charset=");
            if (charsetIndex >= 0) {
                String charsetName = contentType.substring(charsetIndex + "charset=".length()).split("[;\\s]", 2)[0].trim();
                try {
                    return Charset.forName(charsetName);
                } catch (RuntimeException ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
            if (lowerContentType.contains("json")) {
                return StandardCharsets.UTF_8;
            }
        }

        if (!StringUtils.hasText(characterEncoding)) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(characterEncoding);
        } catch (RuntimeException ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    private String maskSensitiveValues(String payload) {
        String masked = payload.replaceAll(
                "(?i)(\\\"[^\\\"]*(password|token|secret|authorization|credential)[^\\\"]*\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")",
                "$1***$3"
        );
        return masked.replaceAll(
                "(?i)((password|token|secret|authorization|credential)[^=&\\s]*=)[^&\\s]+",
                "$1***"
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            return authUser.loginId() + "(" + authUser.userId() + ")";
        }

        String name = authentication.getName();
        if (!StringUtils.hasText(name) || "anonymousUser".equals(name)) {
            return "anonymous";
        }
        return name;
    }
}
