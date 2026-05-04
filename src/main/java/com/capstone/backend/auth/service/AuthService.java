package com.capstone.backend.auth.service;

import com.capstone.backend.auth.dto.LoginRequest;
import com.capstone.backend.auth.dto.LoginResponse;
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

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       ConditionLogRepository conditionLogRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.conditionLogRepository = conditionLogRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public UserProfileResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ApiException(HttpStatus.CONFLICT, "LOGIN_ID_ALREADY_EXISTS", "Login ID already exists");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ApiException(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS", "Nickname already exists");
        }

        User user = User.createLocalUser(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );

        User savedUser = userRepository.save(user);
        return UserProfileResponse.from(savedUser, false, 0);
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
}
