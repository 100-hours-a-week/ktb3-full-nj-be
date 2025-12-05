package com.example.dance_community.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailTest {

    @Test
    @DisplayName("UserDetail 생성 및 필드 매핑 테스트")
    void create_UserDetail_Success() {
        // given
        Long userId = 1L;
        String email = "test@email.com";
        String nickname = "Tester";
        String profileImage = "image.jpg";
        String password = "encodedPassword";

        // when
        UserDetail userDetail = new UserDetail(userId, email, nickname, profileImage, password);

        // then
        assertThat(userDetail.getUserId()).isEqualTo(userId);
        assertThat(userDetail.getUsername()).isEqualTo(email);
        assertThat(userDetail.getNickname()).isEqualTo(nickname);
        assertThat(userDetail.getProfileImage()).isEqualTo(profileImage);
        assertThat(userDetail.getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("권한 목록 조회 테스트 (기본 ROLE_USER 포함)")
    void getAuthorities_Success() {
        // given
        UserDetail userDetail = new UserDetail(1L, "email", "nick", "img", "pw");

        // when
        Collection<? extends GrantedAuthority> authorities = userDetail.getAuthorities();

        // then
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("계정 상태 확인 테스트 (모두 true)")
    void accountStatus_Success() {
        // given
        UserDetail userDetail = new UserDetail(1L, "email", "nick", "img", "pw");

        // then
        assertThat(userDetail.isAccountNonExpired()).isTrue();
        assertThat(userDetail.isAccountNonLocked()).isTrue();
        assertThat(userDetail.isCredentialsNonExpired()).isTrue();
        assertThat(userDetail.isEnabled()).isTrue();
    }
}