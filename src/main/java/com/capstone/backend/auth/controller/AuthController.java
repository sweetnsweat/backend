package com.capstone.backend.auth.controller;

import com.capstone.backend.auth.dto.LoginRequest;
import com.capstone.backend.auth.dto.LoginResponse;
import com.capstone.backend.auth.dto.SignupRequest;
import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.auth.service.AuthService;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.global.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication APIs")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Create a demo user account")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserProfileResponse response = authService.signup(request);
        return ApiResponse.created("Signup succeeded", response);
    }

    @Operation(summary = "Login and issue JWT tokens")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login succeeded", authService.login(request));
    }

    @Operation(summary = "Logout and revoke active refresh tokens")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }

        String accessToken = extractBearerToken(request);
        authService.logout(authUser, accessToken);
        return ApiResponse.ok("Logout succeeded");
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUTH_HEADER", "Authorization header is missing");
        }
        return authHeader.substring(7).trim();
    }
}
