package com.example.dance_community.service;

import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.enums.ClubRole;
import com.example.dance_community.exception.AuthException;
import com.example.dance_community.repository.ClubJoinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClubAuthServiceTest {

    @InjectMocks
    private ClubAuthService clubAuthService;

    @Mock
    private ClubJoinRepository clubJoinRepository;

    @Test
    @DisplayName("관리자 권한 확인 성공 - 매니저")
    void validateClubAuthority_Success_Manager() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin join = ClubJoin.builder().role(ClubRole.MANAGER).build(); // hasManagementPermission == true

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.of(join));

        // when & then
        assertDoesNotThrow(() -> clubAuthService.validateClubAuthority(userId, clubId));
    }

    @Test
    @DisplayName("관리자 권한 확인 실패 - 일반 멤버")
    void validateClubAuthority_Fail_Member() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin join = ClubJoin.builder().role(ClubRole.MEMBER).build(); // hasManagementPermission == false

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.of(join));

        // when & then
        assertThrows(AuthException.class, () -> clubAuthService.validateClubAuthority(userId, clubId));
    }

    @Test
    @DisplayName("리더 권한 확인 실패 - 매니저라도 안됨")
    void validateLeaderAuthority_Fail_Manager() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin join = ClubJoin.builder().role(ClubRole.MANAGER).build(); // 리더 아님

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.of(join));

        // when & then
        assertThrows(AuthException.class, () -> clubAuthService.validateLeaderAuthority(userId, clubId));
    }
}