package com.example.dance_community.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    private final CookieUtil cookieUtil = new CookieUtil();

    @Test
    @DisplayName("RefreshToken 쿠키 설정 테스트")
    void setRefreshTokenCookie_Success() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "refresh-token-value";

        // when
        cookieUtil.setRefreshTokenCookie(response, token);

        // then
        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(token);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isGreaterThan(0);
    }

    @Test
    @DisplayName("RefreshToken 쿠키 삭제 테스트 (MaxAge=0)")
    void deleteRefreshTokenCookie_Success() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieUtil.deleteRefreshTokenCookie(response);

        // then
        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNull();
        assertThat(cookie.getMaxAge()).isEqualTo(0);
    }
}