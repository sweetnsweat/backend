package com.capstone.backend.auth.service;

import com.capstone.backend.auth.dto.ChangePasswordRequest;
import com.capstone.backend.auth.dto.FindLoginIdRequest;
import com.capstone.backend.auth.dto.LoginRequest;
import com.capstone.backend.auth.dto.LoginResponse;
import com.capstone.backend.auth.dto.PasswordResetRequest;
import com.capstone.backend.auth.dto.SignupRequest;
import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.auth.entity.RefreshToken;
import com.capstone.backend.auth.repository.RefreshTokenRepository;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.reward.repository.WalletRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ConditionLogRepository conditionLogRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AuthMailService authMailService;

    private AuthService authService;

    @BeforeEach
    void setup() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                conditionLogRepository,
                walletRepository,
                passwordEncoder,
                jwtTokenService,
                authMailService
        );
    }

    @Test
    void signupSuccess() {
        SignupRequest request = new SignupRequest("demoUser", "password123", "Demo Nick", "demo@example.com");

        when(userRepository.existsByLoginId("demoUser")).thenReturn(false);
        when(userRepository.existsByNickname("Demo Nick")).thenReturn(false);
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserProfileResponse response = authService.signup(request);

        assertEquals(1L, response.id());
        assertEquals("demoUser", response.loginId());
        assertEquals("Demo Nick", response.nickname());
        assertEquals("demo@example.com", response.email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("demoUser", userCaptor.getValue().getLoginId());
        assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());
        assertEquals("demo@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void signupFailsWhenLoginIdAlreadyExists() {
        SignupRequest request = new SignupRequest("demoUser", "password123", "Demo Nick");
        when(userRepository.existsByLoginId("demoUser")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.signup(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("LOGIN_ID_ALREADY_EXISTS", exception.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void checkNicknameReturnsAvailableWhenNicknameDoesNotExist() {
        when(userRepository.existsByNickname("Demo Nick")).thenReturn(false);

        var response = authService.checkNickname("  Demo Nick  ");

        assertEquals("Demo Nick", response.nickname());
        assertEquals(true, response.available());
        assertEquals(false, response.duplicated());
    }

    @Test
    void checkNicknameReturnsDuplicatedWhenNicknameExists() {
        when(userRepository.existsByNickname("Demo Nick")).thenReturn(true);

        var response = authService.checkNickname("Demo Nick");

        assertEquals(false, response.available());
        assertEquals(true, response.duplicated());
    }

    @Test
    void checkNicknameFailsWhenNicknameIsBlank() {
        ApiException exception = assertThrows(ApiException.class, () -> authService.checkNickname(" "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("NICKNAME_REQUIRED", exception.getCode());
    }

    @Test
    void loginSuccess() {
        User user = createUser(1L, "demoUser", "encoded-password", "Demo Nick", "active");
        LoginRequest request = new LoginRequest("demoUser", "password123");

        Instant refreshExpiresAt = Instant.now().plusSeconds(60 * 60);

        when(userRepository.findByLoginId("demoUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(1L)).thenReturn(List.of());
        when(jwtTokenService.issueTokenPair(user)).thenReturn(new JwtTokenService.TokenPair("access-token", "refresh-token"));
        when(jwtTokenService.parseAndValidateRefreshToken("refresh-token"))
                .thenReturn(new JwtTokenService.DecodedRefreshToken(1L, "refresh-jti", refreshExpiresAt));
        when(jwtTokenService.hashToken("refresh-token")).thenReturn("refresh-hash");
        when(conditionLogRepository.findByUser_IdAndLogDate(eq(1L), any())).thenReturn(Optional.empty());
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        LoginResponse response = authService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1L, response.user().id());
        assertEquals(false, response.user().onboardingCompleted());
        assertEquals(true, response.user().requiresOnboarding());
        assertEquals(false, response.user().todayConditionCompleted());
        assertEquals(null, response.user().activeRoutineId());
        assertEquals(false, response.user().routineSetupRequired());

        verify(jwtTokenService).storeRefreshTokenHash("refresh-hash", 1L, refreshExpiresAt);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatch() {
        User user = createUser(1L, "demoUser", "encoded-password", "Demo Nick", "active");
        LoginRequest request = new LoginRequest("demoUser", "wrong-password");

        when(userRepository.findByLoginId("demoUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("INVALID_CREDENTIALS", exception.getCode());
    }

    @Test
    void findLoginIdByEmail() {
        User user = createUser(1L, "demoUser", "encoded-password", "Demo Nick", "active");
        ReflectionTestUtils.setField(user, "email", "demo@example.com");
        when(userRepository.findFirstByEmail("demo@example.com")).thenReturn(Optional.of(user));

        authService.sendLoginIdEmail(new FindLoginIdRequest("demo@example.com"));

        verify(authMailService).sendLoginId("demo@example.com", "Demo Nick", "demoUser");
    }

    @Test
    void requestPasswordResetIssuesTemporaryPasswordAndRevokesRefreshTokens() {
        User user = createUser(1L, "demoUser", "old-encoded-password", "Demo Nick", "active");
        RefreshToken refreshToken = RefreshToken.issue(user, "refresh-hash", Instant.now().plusSeconds(7200));
        ReflectionTestUtils.setField(user, "email", "demo@example.com");

        when(userRepository.findFirstByEmail("demo@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("temporary-encoded-password");
        when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(1L)).thenReturn(List.of(refreshToken));

        authService.requestPasswordReset(new PasswordResetRequest("demo@example.com"));

        assertEquals("temporary-encoded-password", user.getPasswordHash());
        verify(authMailService).sendTemporaryPassword(eq("demo@example.com"), eq("Demo Nick"), anyString());
        verify(jwtTokenService).deleteRefreshTokenHash("refresh-hash");
        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    void changePasswordUpdatesPasswordAndRevokesRefreshTokens() {
        User user = createUser(1L, "demoUser", "old-encoded-password", "Demo Nick", "active");
        RefreshToken refreshToken = RefreshToken.issue(user, "refresh-hash", Instant.now().plusSeconds(7200));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword123", "old-encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");
        when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(1L)).thenReturn(List.of(refreshToken));

        authService.changePassword(1L, new ChangePasswordRequest("oldPassword123", "newPassword123"));

        assertEquals("new-encoded-password", user.getPasswordHash());
        verify(jwtTokenService).deleteRefreshTokenHash("refresh-hash");
        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    void changePasswordFailsWhenCurrentPasswordDoesNotMatch() {
        User user = createUser(1L, "demoUser", "old-encoded-password", "Demo Nick", "active");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword123", "old-encoded-password")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> authService.changePassword(1L, new ChangePasswordRequest("wrongPassword123", "newPassword123")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("INVALID_CURRENT_PASSWORD", exception.getCode());
        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenRepository, never()).findByUser_IdAndRevokedAtIsNull(any());
    }

    @Test
    void logoutSuccess() {
        AuthUser authUser = new AuthUser(1L, "demoUser");
        User user = createUser(1L, "demoUser", "encoded-password", "Demo Nick", "active");
        RefreshToken refreshToken = RefreshToken.issue(user, "refresh-hash", Instant.now().plusSeconds(7200));
        Instant accessExpiresAt = Instant.now().plusSeconds(900);

        when(jwtTokenService.parseAndValidateAccessToken("access-token"))
                .thenReturn(new JwtTokenService.DecodedAccessToken(1L, "demoUser", "access-jti", accessExpiresAt));
        when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(1L)).thenReturn(List.of(refreshToken));

        authService.logout(authUser, "access-token");

        verify(jwtTokenService).blacklistAccessToken("access-jti", accessExpiresAt);
        verify(jwtTokenService).deleteRefreshTokenHash("refresh-hash");
        verify(refreshTokenRepository).saveAll(any());

        Instant revokedAt = (Instant) ReflectionTestUtils.getField(refreshToken, "revokedAt");
        assertNotNull(revokedAt);
    }

    @Test
    void logoutFailsWhenTokenOwnerMismatch() {
        AuthUser authUser = new AuthUser(1L, "demoUser");
        Instant accessExpiresAt = Instant.now().plusSeconds(900);

        when(jwtTokenService.parseAndValidateAccessToken("access-token"))
                .thenReturn(new JwtTokenService.DecodedAccessToken(2L, "otherUser", "access-jti", accessExpiresAt));

        ApiException exception = assertThrows(ApiException.class, () -> authService.logout(authUser, "access-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("INVALID_TOKEN_OWNER", exception.getCode());
        verify(jwtTokenService, never()).blacklistAccessToken(eq("access-jti"), any());
    }

    private User createUser(Long id, String loginId, String passwordHash, String nickname, String status) {
        User user = User.createLocalUser(loginId, passwordHash, nickname);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "status", status);
        return user;
    }
}
