package com.example.dance_community.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithCustomMockUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

    @Override
    public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // ✅ 보내주신 UserDetail 생성자에 맞춰서 객체 생성
        UserDetail userDetail = new UserDetail(
                annotation.userId(),
                annotation.email(),
                annotation.nickname(),
                null, // profileImage (테스트에선 필요 없으면 null)
                "password" // password (임의 값)
        );

        // 인증 토큰 생성 (Principal에 userDetail을 넣는 것이 중요!)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetail,
                "password",
                userDetail.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}