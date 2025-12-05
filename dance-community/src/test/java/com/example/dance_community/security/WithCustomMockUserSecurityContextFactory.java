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

        UserDetail userDetail = new UserDetail(
                annotation.userId(),
                annotation.email(),
                annotation.nickname(),
                null,
                "password"
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetail,
                "password",
                userDetail.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}