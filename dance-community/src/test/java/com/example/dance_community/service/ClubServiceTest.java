package com.example.dance_community.service;

import com.example.dance_community.dto.club.ClubCreateRequest;
import com.example.dance_community.dto.club.ClubResponse;
import com.example.dance_community.dto.club.ClubUpdateRequest;
import com.example.dance_community.entity.Club;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.ClubType;
import com.example.dance_community.exception.AuthException;
import com.example.dance_community.repository.ClubRepository;
import com.example.dance_community.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
class ClubServiceTest {

    @InjectMocks
    private ClubService clubService;

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClubAuthService clubAuthService;
    @Mock
    private ClubJoinService clubJoinService;
    @Mock
    private EventJoinService eventJoinService;
    @Mock
    private PostService postService;
    @Mock
    private EventService eventService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private EntityManager em;

    // --- 1. 동아리 생성 (createClub) ---

    @Test
    @DisplayName("동아리 생성 성공 - 생성자가 리더로 등록됨")
    void createClub_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        ClubCreateRequest request = new ClubCreateRequest(
                "Dance Crew", "Intro", "Desc", "Seoul", ClubType.CREW, "img.jpg", List.of("hiphop")
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // 저장될 Club 객체 Mocking
        Club savedClub = Club.builder()
                .clubId(10L)
                .clubName("Dance Crew")
                .clubType(ClubType.CREW)
                .build();

        // save 호출 시 savedClub 반환
        given(clubRepository.save(any(Club.class))).willReturn(savedClub);

        // when
        ClubResponse response = clubService.createClub(userId, request);

        // then
        assertThat(response.clubName()).isEqualTo("Dance Crew");
        // Club.addMember()가 내부적으로 호출되었는지 확인은 어렵지만(Entity 로직),
        // save()가 호출되었는지는 확인 가능
        verify(clubRepository).save(any(Club.class));
    }

    // --- 2. 동아리 수정 (updateClub) ---

    @Test
    @DisplayName("동아리 수정 성공 - 권한 있음")
    void updateClub_Success() {
        // given
        Long userId = 1L;
        Long clubId = 10L;

        // 기존 Club (이미지 있음)
        Club club = spy(Club.builder()
                .clubId(clubId)
                .clubName("Old Name")
                .clubImage("old.jpg")
                .build());

        ClubUpdateRequest request = new ClubUpdateRequest(
                "New Name", "New Intro", "Desc", "Loc", ClubType.CLUB, "new.jpg", List.of("tag")
        );

        // 권한 체크 통과 가정
        doNothing().when(clubAuthService).validateClubAuthority(userId, clubId);
        given(clubAuthService.findByClubId(clubId)).willReturn(club);
        given(clubRepository.save(any(Club.class))).willReturn(club);

        // when
        ClubResponse response = clubService.updateClub(userId, clubId, request);

        // then
        // 1. 기존 이미지 삭제 호출 확인
        verify(fileStorageService).deleteFile("old.jpg");
        // 2. 엔티티 업데이트 메서드 호출 확인
        verify(club).updateClub(
                eq("New Name"), any(), any(), any(), any(), eq("new.jpg"), any()
        );
    }

    @Test
    @DisplayName("동아리 수정 실패 - 권한 없음")
    void updateClub_Fail_NoAuth() {
        // given
        Long userId = 99L; // 다른 유저
        Long clubId = 10L;
        ClubUpdateRequest request = new ClubUpdateRequest(
                "Name", "Intro", "Desc", "Loc", ClubType.CLUB, null, null
        );

        // 권한 체크에서 예외 발생 설정
        doThrow(new AuthException("권한 없음")).when(clubAuthService).validateClubAuthority(userId, clubId);

        // when & then
        assertThrows(AuthException.class, () ->
                clubService.updateClub(userId, clubId, request)
        );
    }

    // --- 3. 동아리 대표 이미지 삭제 (deleteClubImage) ---

    @Test
    @DisplayName("동아리 이미지 삭제 성공")
    void deleteClubImage_Success() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        Club club = spy(Club.builder().clubId(clubId).clubImage("image.jpg").build());

        doNothing().when(clubAuthService).validateClubAuthority(userId, clubId);
        given(clubAuthService.findByClubId(clubId)).willReturn(club);
        given(clubRepository.save(club)).willReturn(club);

        // when
        clubService.deleteClubImage(userId, clubId);

        // then
        verify(fileStorageService).deleteFile("image.jpg"); // 실제 파일 삭제 요청
        verify(club).deleteImage(); // 엔티티 필드 null 처리
    }

    // --- 4. 동아리 삭제 (deleteClub) ---

    @Test
    @DisplayName("동아리 삭제 성공 - 연관 데이터 모두 삭제됨")
    void deleteClub_Success() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        Club club = spy(Club.builder().clubId(clubId).clubImage("image.jpg").build());

        // 리더 권한 체크 통과
        doNothing().when(clubAuthService).validateLeaderAuthority(userId, clubId);
        given(clubAuthService.findByClubId(clubId)).willReturn(club);

        // when
        clubService.deleteClub(userId, clubId);

        // then
        // 1. 이미지 파일 삭제
        verify(fileStorageService).deleteFile("image.jpg");

        // 2. 연관 데이터 Soft Delete 호출 확인 (Facade 역할 검증)
        verify(postService).softDeleteByClubId(clubId);
        verify(eventService).softDeleteByClubId(clubId);
        verify(clubJoinService).softDeleteByClubId(clubId);
        verify(eventJoinService).softDeleteByClubId(clubId);

        // 3. 클럽 자체 삭제 및 Flush
        verify(club).delete();
        verify(em).flush();
    }
}