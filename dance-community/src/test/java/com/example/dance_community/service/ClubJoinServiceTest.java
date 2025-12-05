package com.example.dance_community.service;

import com.example.dance_community.dto.club.ClubJoinResponse;
import com.example.dance_community.entity.Club;
import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.ClubJoinStatus;
import com.example.dance_community.enums.ClubRole;
import com.example.dance_community.exception.ConflictException;
import com.example.dance_community.exception.InvalidRequestException;
import com.example.dance_community.repository.ClubJoinRepository;
import com.example.dance_community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubJoinServiceTest {

    @InjectMocks
    private ClubJoinService clubJoinService;

    @Mock
    private ClubJoinRepository clubJoinRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClubAuthService clubAuthService;

    // --- 1. 일반 사용자 기능 (신청/취소/탈퇴) ---

    @Test
    @DisplayName("클럽 가입 신청 성공 - 신규 신청")
    void applyToClub_Success_New() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        User user = User.builder().userId(userId).build();
        Club club = Club.builder().clubId(clubId).build();

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.empty()); // 기존 내역 없음
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(clubAuthService.findByClubId(clubId)).willReturn(club);

        // save 호출 시 PENDING 상태의 객체 반환
        ClubJoin savedJoin = ClubJoin.builder().user(user).club(club).status(ClubJoinStatus.PENDING).role(ClubRole.MEMBER).build();
        given(clubJoinRepository.save(any(ClubJoin.class))).willReturn(savedJoin);

        // when
        ClubJoinResponse response = clubJoinService.applyToClub(userId, clubId);

        // then
        assertThat(response.status()).isEqualTo(ClubJoinStatus.PENDING.name());
        verify(clubJoinRepository, times(1)).save(any(ClubJoin.class));
    }

    @Test
    @DisplayName("클럽 가입 신청 실패 - 이미 활동 중")
    void applyToClub_Fail_AlreadyActive() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin existingJoin = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).build();

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.of(existingJoin));

        // when & then
        assertThrows(ConflictException.class, () -> clubJoinService.applyToClub(userId, clubId));
    }

    @Test
    @DisplayName("클럽 가입 신청 성공 - 탈퇴 후 재가입")
    void applyToClub_Success_Rejoin() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        // 이전에 탈퇴(LEFT)한 기록 있음
        User user = User.builder().userId(userId).build();
        Club club = Club.builder().clubId(clubId).build();

        ClubJoin existingJoin = ClubJoin.builder().status(ClubJoinStatus.LEFT).role(ClubRole.MEMBER).user(user).club(club).build();

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.of(existingJoin));

        // when
        ClubJoinResponse response = clubJoinService.applyToClub(userId, clubId);

        // then
        assertThat(response.status()).isEqualTo(ClubJoinStatus.PENDING.name());
        assertThat(existingJoin.getStatus()).isEqualTo(ClubJoinStatus.PENDING); // 상태 변경 확인
        verify(clubJoinRepository, never()).save(any()); // save 호출 안 함
    }

    @Test
    @DisplayName("클럽 가입 신청 실패 - 이미 신청 대기 중(PENDING)")
    void applyToClub_Fail_AlreadyPending() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin existingJoin = ClubJoin.builder().status(ClubJoinStatus.PENDING).build();

        given(clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId))
                .willReturn(Optional.of(existingJoin));

        // when & then
        assertThrows(ConflictException.class, () -> clubJoinService.applyToClub(userId, clubId));
    }

    @Test
    @DisplayName("신청 취소 성공")
    void cancelApplication_Success() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin join = ClubJoin.builder().status(ClubJoinStatus.PENDING).build();

        given(clubAuthService.findClubJoin(userId, clubId)).willReturn(join);

        // when
        clubJoinService.cancelApplication(userId, clubId);

        // then
        assertThat(join.getStatus()).isEqualTo(ClubJoinStatus.CANCELED);
    }

    @Test
    @DisplayName("클럽 탈퇴 성공")
    void leaveClub_Success() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        // 일반 멤버는 탈퇴 가능
        ClubJoin join = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).role(ClubRole.MEMBER).build();

        given(clubAuthService.findClubJoin(userId, clubId)).willReturn(join);

        // when
        clubJoinService.leaveClub(userId, clubId);

        // then
        assertThat(join.getStatus()).isEqualTo(ClubJoinStatus.LEFT);
    }

    @Test
    @DisplayName("클럽 탈퇴 실패 - 리더는 탈퇴 불가")
    void leaveClub_Fail_Leader() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        ClubJoin join = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).role(ClubRole.LEADER).build();

        given(clubAuthService.findClubJoin(userId, clubId)).willReturn(join);

        // when & then
        assertThrows(InvalidRequestException.class, () -> clubJoinService.leaveClub(userId, clubId));
    }

    // --- 2. 관리자 기능 (승인/거절/추방/권한변경) ---

    @Test
    @DisplayName("가입 승인 성공")
    void approveApplication_Success() {
        // given
        Long managerId = 1L;
        Long clubId = 10L;
        Long applicantId = 2L;
        ClubJoin applicantJoin = ClubJoin.builder().status(ClubJoinStatus.PENDING).build();

        // 관리자 권한 체크는 통과했다고 가정 (Mock)
        doNothing().when(clubAuthService).validateClubAuthority(managerId, clubId);
        given(clubAuthService.findClubJoin(applicantId, clubId)).willReturn(applicantJoin);

        // when
        clubJoinService.approveApplication(managerId, clubId, applicantId);

        // then
        assertThat(applicantJoin.getStatus()).isEqualTo(ClubJoinStatus.ACTIVE);
    }

    @Test
    @DisplayName("가입 거절 성공")
    void rejectApplication_Success() {
        // given
        Long managerId = 1L;
        Long clubId = 10L;
        Long applicantId = 2L;
        ClubJoin applicantJoin = ClubJoin.builder().status(ClubJoinStatus.PENDING).build();

        doNothing().when(clubAuthService).validateClubAuthority(managerId, clubId);
        given(clubAuthService.findClubJoin(applicantId, clubId)).willReturn(applicantJoin);

        // when
        clubJoinService.rejectApplication(managerId, clubId, applicantId);

        // then
        assertThat(applicantJoin.getStatus()).isEqualTo(ClubJoinStatus.REJECTED);
    }

    @Test
    @DisplayName("가입 거절 실패 - 대기 상태가 아님")
    void rejectApplication_Fail_NotPending() {
        // given
        Long managerId = 1L;
        Long clubId = 10L;
        Long applicantId = 2L;
        // 이미 활동 중인 멤버를 거절하려고 함
        ClubJoin applicantJoin = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).build();

        doNothing().when(clubAuthService).validateClubAuthority(managerId, clubId);
        given(clubAuthService.findClubJoin(applicantId, clubId)).willReturn(applicantJoin);

        // when & then
        assertThrows(InvalidRequestException.class, () ->
                clubJoinService.rejectApplication(managerId, clubId, applicantId));
    }

    @Test
    @DisplayName("멤버 추방 성공")
    void kickMember_Success() {
        // given
        Long managerId = 1L;
        Long clubId = 10L;
        Long targetId = 2L;
        ClubJoin targetJoin = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).role(ClubRole.MEMBER).build();

        doNothing().when(clubAuthService).validateClubAuthority(managerId, clubId);
        given(clubAuthService.findClubJoin(targetId, clubId)).willReturn(targetJoin);

        // when
        clubJoinService.kickMember(managerId, clubId, targetId);

        // then
        assertThat(targetJoin.getStatus()).isEqualTo(ClubJoinStatus.LEFT);
    }

    @Test
    @DisplayName("멤버 추방 실패 - 자기 자신 추방 불가")
    void kickMember_Fail_Self() {
        // given
        Long managerId = 1L;
        Long clubId = 10L;
        Long targetId = 1L; // 본인

        doNothing().when(clubAuthService).validateClubAuthority(managerId, clubId);
        // findClubJoin 호출 전에 ID 검사에서 걸려야 함 (Service 로직 순서 확인)
        // 현재 로직상 validate -> find -> ID체크 순서이므로 find도 Mocking 필요
        ClubJoin selfJoin = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).role(ClubRole.MANAGER).build();
        given(clubAuthService.findClubJoin(targetId, clubId)).willReturn(selfJoin);

        // when & then
        assertThrows(InvalidRequestException.class, () ->
                clubJoinService.kickMember(managerId, clubId, targetId));
    }

    @Test
    @DisplayName("권한 변경 성공 - 리더만 가능")
    void changeMemberRole_Success() {
        // given
        Long leaderId = 1L;
        Long clubId = 10L;
        Long targetId = 2L;
        ClubJoin targetJoin = ClubJoin.builder().status(ClubJoinStatus.ACTIVE).role(ClubRole.MEMBER).build();

        doNothing().when(clubAuthService).validateLeaderAuthority(leaderId, clubId);
        given(clubAuthService.findClubJoin(targetId, clubId)).willReturn(targetJoin);

        // when
        clubJoinService.changeMemberRole(leaderId, clubId, targetId, ClubRole.MANAGER);

        // then
        assertThat(targetJoin.getRole()).isEqualTo(ClubRole.MANAGER);
    }

    @Test
    @DisplayName("내 클럽 목록 조회 성공")
    void getMyClubs_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        // QueryDSL Mocking: Club 정보가 채워진 Join 객체 리스트 반환
        Club club = Club.builder().clubId(10L).clubName("Dance Team").build();
        ClubJoin join = ClubJoin.builder().user(user).club(club).role(ClubRole.LEADER).status(ClubJoinStatus.ACTIVE).build();

        given(clubJoinRepository.findMyClubJoins(userId, List.of(ClubJoinStatus.ACTIVE)))
                .willReturn(List.of(join));

        // when
        var result = clubJoinService.getMyClubs(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().clubName()).isEqualTo("Dance Team");
    }
}