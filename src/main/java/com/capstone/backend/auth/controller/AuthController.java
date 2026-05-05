package com.capstone.backend.auth.controller;

import com.capstone.backend.auth.dto.FindLoginIdRequest;
import com.capstone.backend.auth.dto.FindLoginIdResponse;
import com.capstone.backend.auth.dto.LoginRequest;
import com.capstone.backend.auth.dto.LoginResponse;
import com.capstone.backend.auth.dto.ResetPasswordRequest;
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

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "회원가입", description = "로그인 테스트와 데모 진행에 사용할 로컬 사용자 계정을 생성합니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserProfileResponse response = authService.signup(request);
        return ApiResponse.created("회원가입이 완료되었습니다.", response);
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호를 검증하고 JWT access token과 refresh token을 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("로그인에 성공했습니다.", authService.login(request));
    }

    @Operation(summary = "아이디 찾기", description = "가입 시 등록한 이메일로 로그인 아이디를 조회합니다.")
    @PostMapping("/find-login-id")
    public ApiResponse<FindLoginIdResponse> findLoginId(@Valid @RequestBody FindLoginIdRequest request) {
        return ApiResponse.ok("아이디를 조회했습니다.", authService.findLoginId(request));
    }

    @Operation(summary = "비밀번호 재설정", description = "로그인 아이디와 가입 시 등록한 이메일이 일치하면 새 비밀번호로 변경합니다.")
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("비밀번호가 재설정되었습니다.");
    }

    @Operation(summary = "로그아웃", description = "현재 access token을 검증하고 사용자의 활성 refresh token을 폐기합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }

        String accessToken = extractBearerToken(request);
        authService.logout(authUser, accessToken);
        return ApiResponse.ok("로그아웃이 완료되었습니다.");
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUTH_HEADER", "Authorization header is missing");
        }
        return authHeader.substring(7).trim();
    }
}
