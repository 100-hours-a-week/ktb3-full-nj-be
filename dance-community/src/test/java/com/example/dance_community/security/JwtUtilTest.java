package com.example.dance_community.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "thisIsTestSecretKeyForJwtValidation1234567890";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        jwtUtil.init();
    }

    @Test
    @DisplayName("토큰 생성 및 검증 성공 - Access Token")
    void generateAndValidateAccessToken() {
        // given
        Long userId = 1L;

        // when
        String token = jwtUtil.generateAccessToken(userId);

        // then
        assertThat(token).isNotNull();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.isAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("토큰 생성 및 검증 성공 - Refresh Token")
    void generateAndValidateRefreshToken() {
        // given
        Long userId = 1L;

        // when
        String token = jwtUtil.generateRefreshToken(userId);

        // then
        assertThat(token).isNotNull();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_Expired() {
        // given
        Date past = new Date(System.currentTimeMillis() - 1000); // 1초 전
        String expiredToken = Jwts.builder()
                .setSubject("1")
                .setExpiration(past)
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        // when
        boolean isValid = jwtUtil.validateToken(expiredToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("위조된 토큰 검증 실패")
    void validateToken_Forged() {
        // given
        String fakeSecret = "fakeSecretKeyMustBeLongEnough1234567890";
        String forgedToken = Jwts.builder()
                .setSubject("1")
                .signWith(Keys.hmacShaKeyFor(fakeSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        // when
        boolean isValid = jwtUtil.validateToken(forgedToken);

        // then
        assertThat(isValid).isFalse();
    }
}