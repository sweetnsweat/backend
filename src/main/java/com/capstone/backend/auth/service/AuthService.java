package com.capstone.backend.auth.service;

import com.capstone.backend.auth.dto.ChangePasswordRequest;
import com.capstone.backend.auth.dto.FindLoginIdRequest;
import com.capstone.backend.auth.dto.LoginRequest;
import com.capstone.backend.auth.dto.LoginResponse;
import com.capstone.backend.auth.dto.NicknameAvailabilityResponse;
import com.capstone.backend.auth.dto.PasswordResetRequest;
import com.capstone.backend.auth.dto.SignupRequest;
import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.auth.entity.RefreshToken;
import com.capstone.backend.auth.repository.RefreshTokenRepository;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.condition.repository.ConditionLogRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.reward.repository.WalletRepository;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthMailService authMailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final char[] TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final int TEMP_PASSWORD_LENGTH = 10;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       ConditionLogRepository conditionLogRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       AuthMailService authMailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.conditionLogRepository = conditionLogRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authMailService = authMailService;
    }

    @Transactional
    public UserProfileResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ApiException(HttpStatus.CONFLICT, "LOGIN_ID_ALREADY_EXISTS", "Login ID already exists");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ApiException(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS", "Nickname already exists");
        }
        String email = normalize(request.email());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 등록된 이메일입니다.");
        }

        User user = User.createLocalUser(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                email,
                null
        );

        User savedUser = userRepository.save(user);
        return UserProfileResponse.from(savedUser, false, 0);
    }

    @Transactional(readOnly = true)
    public NicknameAvailabilityResponse checkNickname(String nickname) {
        String normalizedNickname = normalize(nickname);
        if (normalizedNickname == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NICKNAME_REQUIRED", "닉네임을 입력해 주세요.");
        }
        if (normalizedNickname.length() < 2 || normalizedNickname.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임은 2~50자여야 합니다.");
        }

        return NicknameAvailabilityResponse.of(normalizedNickname, userRepository.existsByNickname(normalizedNickname));
    }

    @Transactional(readOnly = true)
    public void sendLoginIdEmail(FindLoginIdRequest request) {
        String email = normalize(request.email());
        if (email == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "이메일을 입력해 주세요.");
        }

        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "일치하는 계정을 찾을 수 없습니다."));

        authMailService.sendLoginId(user.getEmail(), user.getNickname(), user.getLoginId());
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = normalize(request.email());
        if (email == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "이메일을 입력해 주세요.");
        }

        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "일치하는 계정을 찾을 수 없습니다."));

        String temporaryPassword = issueTemporaryPassword();
        user.changePassword(passwordEncoder.encode(temporaryPassword));
        revokeAllRefreshTokens(user.getId());
        authMailService.sendTemporaryPassword(user.getEmail(), user.getNickname(), temporaryPassword);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "일치하는 계정을 찾을 수 없습니다."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "현재 비밀번호가 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        revokeAllRefreshTokens(user.getId());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid login credentials"));

        if (!"active".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "Inactive user status");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid login credentials");
        }

        revokeAllRefreshTokens(user.getId());

        JwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokenPair(user);
        JwtTokenService.DecodedRefreshToken decodedRefreshToken = jwtTokenService.parseAndValidateRefreshToken(tokenPair.refreshToken());
        String refreshTokenHash = jwtTokenService.hashToken(tokenPair.refreshToken());

        RefreshToken refreshTokenEntity = RefreshToken.issue(user, refreshTokenHash, decodedRefreshToken.expiresAt());
        refreshTokenRepository.save(refreshTokenEntity);
        jwtTokenService.storeRefreshTokenHash(refreshTokenHash, user.getId(), decodedRefreshToken.expiresAt());

        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer",
                UserProfileResponse.from(user, hasTodayCondition(user.getId()), balanceCurrency(user.getId()))
        );
    }

    @Transactional
    public void logout(AuthUser authUser, String accessToken) {
        JwtTokenService.DecodedAccessToken decodedAccessToken = jwtTokenService.parseAndValidateAccessToken(accessToken);

        if (!decodedAccessToken.userId().equals(authUser.userId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_OWNER", "Token owner mismatch");
        }

        jwtTokenService.blacklistAccessToken(decodedAccessToken.jti(), decodedAccessToken.expiresAt());
        revokeAllRefreshTokens(authUser.userId());
    }

    private void revokeAllRefreshTokens(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(userId);
        if (activeTokens.isEmpty()) {
            return;
        }

        Instant now = KoreanTime.nowInstant();
        for (RefreshToken refreshToken : activeTokens) {
            refreshToken.revoke(now);
            jwtTokenService.deleteRefreshTokenHash(refreshToken.getTokenHash());
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private boolean hasTodayCondition(Long userId) {
        return conditionLogRepository.findByUser_IdAndLogDate(userId, KoreanTime.today()).isPresent();
    }

    private int balanceCurrency(Long userId) {
        return walletRepository.findById(userId)
                .map(wallet -> wallet.getBalanceCurrency())
                .orElse(0);
    }

    private String issueTemporaryPassword() {
        StringBuilder temporaryPassword = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            temporaryPassword.append(TEMP_PASSWORD_CHARS[secureRandom.nextInt(TEMP_PASSWORD_CHARS.length)]);
        }
        return temporaryPassword.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
