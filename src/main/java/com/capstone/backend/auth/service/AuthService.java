package com.capstone.backend.auth.service;

import com.capstone.backend.auth.dto.FindLoginIdRequest;
import com.capstone.backend.auth.dto.LoginRequest;
import com.capstone.backend.auth.dto.LoginResponse;
import com.capstone.backend.auth.dto.NicknameAvailabilityResponse;
import com.capstone.backend.auth.dto.PasswordResetConfirmRequest;
import com.capstone.backend.auth.dto.PasswordResetRequest;
import com.capstone.backend.auth.dto.SignupRequest;
import com.capstone.backend.auth.dto.UserProfileResponse;
import com.capstone.backend.auth.entity.PasswordResetToken;
import com.capstone.backend.auth.entity.RefreshToken;
import com.capstone.backend.auth.repository.PasswordResetTokenRepository;
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
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthMailService authMailService;
    private final long passwordResetTokenMinutes;
    private final String passwordResetLinkTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       ConditionLogRepository conditionLogRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       AuthMailService authMailService,
                       @Value("${app.auth.password-reset-token-minutes:30}") long passwordResetTokenMinutes,
                       @Value("${app.auth.password-reset-link-template:sweetnsweat://password-reset?token=%s}") String passwordResetLinkTemplate) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.conditionLogRepository = conditionLogRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authMailService = authMailService;
        this.passwordResetTokenMinutes = passwordResetTokenMinutes;
        this.passwordResetLinkTemplate = passwordResetLinkTemplate;
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
        String token = issuePasswordResetToken(user);
        String resetLink = passwordResetLink(token);
        authMailService.sendPasswordReset(user.getEmail(), user.getNickname(), token, resetLink, passwordResetTokenMinutes);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String tokenHash = jwtTokenService.hashToken(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_RESET_TOKEN", "비밀번호 재설정 토큰이 올바르지 않습니다."));
        Instant now = KoreanTime.nowInstant();
        if (resetToken.isUsed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_USED", "이미 사용된 비밀번호 재설정 토큰입니다.");
        }
        if (resetToken.isExpired(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_EXPIRED", "비밀번호 재설정 토큰이 만료되었습니다.");
        }

        User user = resetToken.getUser();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed(now);
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

    private String issuePasswordResetToken(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = jwtTokenService.hashToken(token);
        Instant expiresAt = KoreanTime.nowInstant().plusSeconds(passwordResetTokenMinutes * 60);
        passwordResetTokenRepository.save(PasswordResetToken.issue(user, tokenHash, expiresAt));
        return token;
    }

    private String passwordResetLink(String token) {
        if (passwordResetLinkTemplate == null || passwordResetLinkTemplate.isBlank()) {
            return token;
        }
        return passwordResetLinkTemplate.formatted(token);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
