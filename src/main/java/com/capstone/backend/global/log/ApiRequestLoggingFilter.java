package com.capstone.backend.global.log;

import com.capstone.backend.auth.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final String API_PREFIX = "/api/";

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
        long startedAt = System.nanoTime();
        Exception failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            writeAccessLog(request, response, durationMs, failure);
        }
    }

    private void writeAccessLog(HttpServletRequest request,
                                HttpServletResponse response,
                                long durationMs,
                                Exception failure) {
        String failurePart = failure == null ? "" : " exception=" + failure.getClass().getSimpleName();
        log.info("API_REQUEST method={} path={} status={} durationMs={} client={} user={}{}",
                request.getMethod(),
                pathWithQuery(request),
                response.getStatus(),
                durationMs,
                clientIp(request),
                currentUser(),
                failurePart);
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
