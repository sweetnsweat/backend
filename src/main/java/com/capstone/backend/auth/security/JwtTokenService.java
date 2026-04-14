package com.capstone.backend.auth.security;

import com.capstone.backend.auth.config.JwtProperties;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties, StringRedisTemplate redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;

        if (jwtProperties.secret() == null || jwtProperties.secret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair issueTokenPair(User user) {
        String accessToken = createToken(user, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenSeconds());
        String refreshToken = createToken(user, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenSeconds());
        return new TokenPair(accessToken, refreshToken);
    }

    public DecodedAccessToken parseAndValidateAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);

        String jti = claims.getId();
        if (isAccessTokenBlacklisted(jti)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", "Access token was revoked");
        }

        Long userId = Long.parseLong(claims.getSubject());
        String loginId = claims.get("loginId", String.class);
        Instant expiresAt = claims.getExpiration().toInstant();

        return new DecodedAccessToken(userId, loginId, jti, expiresAt);
    }

    public DecodedRefreshToken parseAndValidateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);

        Long userId = Long.parseLong(claims.getSubject());
        String jti = claims.getId();
        Instant expiresAt = claims.getExpiration().toInstant();

        return new DecodedRefreshToken(userId, jti, expiresAt);
    }

    public void blacklistAccessToken(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(accessBlacklistKey(jti), "1", ttl);
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        Boolean exists = redisTemplate.hasKey(accessBlacklistKey(jti));
        return Boolean.TRUE.equals(exists);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public void storeRefreshTokenHash(String tokenHash, Long userId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(refreshTokenKey(tokenHash), String.valueOf(userId), ttl);
    }

    public void deleteRefreshTokenHash(String tokenHash) {
        redisTemplate.delete(refreshTokenKey(tokenHash));
    }

    private String createToken(User user, String tokenType, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("typ", tokenType)
                .claim("loginId", user.getLoginId())
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token");
        }
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String tokenType = claims.get("typ", String.class);
        if (!expectedType.equals(tokenType)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_TYPE", "Token type mismatch");
        }
    }

    private String accessBlacklistKey(String jti) {
        return "auth:blacklist:access:" + jti;
    }

    private String refreshTokenKey(String tokenHash) {
        return "auth:refresh:hash:" + tokenHash;
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    public record DecodedAccessToken(Long userId, String loginId, String jti, Instant expiresAt) {
    }

    public record DecodedRefreshToken(Long userId, String jti, Instant expiresAt) {
    }
}
