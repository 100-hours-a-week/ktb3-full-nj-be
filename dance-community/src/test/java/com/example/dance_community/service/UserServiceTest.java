package com.example.dance_community.service;

import com.example.dance_community.dto.user.PasswordUpdateRequest;
import com.example.dance_community.dto.user.UserResponse;
import com.example.dance_community.dto.user.UserUpdateRequest;
import com.example.dance_community.entity.User;
import com.example.dance_community.exception.ConflictException;
import com.example.dance_community.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PostService postService;
    @Mock
    private EventService eventService;
    @Mock
    private ClubJoinService clubJoinService;
    @Mock
    private EventJoinService eventJoinService;
    @Mock
    private EntityManager em;

    // --- 1. 회원가입 (createUser) ---

    @Test
    @DisplayName("회원가입 성공")
    void createUser_Success() {
        // given
        String email = "test@example.com";
        String password = "password";
        String nickname = "Dancer";
        String profileImage = "image.jpg";

        given(userRepository.existsByEmail(email)).willReturn(false);
        given(userRepository.existsByNickname(nickname)).willReturn(false);
        given(passwordEncoder.encode(password)).willReturn("encodedPassword");

        User savedUser = User.builder()
                .userId(1L)
                .email(email)
                .password("encodedPassword")
                .nickname(nickname)
                .profileImage(profileImage)
                .build();

        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = userService.createUser(email, password, nickname, profileImage);

        // then
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.nickname()).isEqualTo(nickname);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void createUser_Fail_DuplicateEmail() {
        // given
        given(userRepository.existsByEmail("test@example.com")).willReturn(true);

        // when & then
        assertThrows(ConflictException.class, () ->
                userService.createUser("test@example.com", "pw", "nick", null)
        );
    }

    // --- 2. 회원 정보 수정 (updateUser) ---

    @Test
    @DisplayName("회원 정보 수정 성공 - 닉네임 및 이미지 변경")
    void updateUser_Success() {
        // given
        Long userId = 1L;
        // 기존 유저 (이미지 있음)
        User user = spy(User.builder()
                .userId(userId)
                .nickname("OldNick")
                .profileImage("old.jpg")
                .build());

        UserUpdateRequest request = new UserUpdateRequest("NewNick", "new.jpg");

        // 닉네임 중복 체크 통과
        given(userRepository.existsByNicknameAndUserIdNot("NewNick", userId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        UserResponse response = userService.updateUser(userId, request);

        // then
        // 1. 기존 이미지 삭제 호출 확인
        verify(fileStorageService).deleteFile("old.jpg");
        // 2. 유저 정보 업데이트 확인
        assertThat(user.getNickname()).isEqualTo("NewNick");
        assertThat(user.getProfileImage()).isEqualTo("new.jpg");
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 중복된 닉네임")
    void updateUser_Fail_DuplicateNickname() {
        // given
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest("DuplicateNick", null);

        // 닉네임 중복 발생 (다른 유저가 쓰고 있음)
        given(userRepository.existsByNicknameAndUserIdNot("DuplicateNick", userId)).willReturn(true);

        // when & then
        assertThrows(ConflictException.class, () -> userService.updateUser(userId, request));
    }

    // --- 3. 비밀번호 변경 (updatePassword) ---

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_Success() {
        // given
        Long userId = 1L;
        User user = spy(User.builder().userId(userId).password("oldHash").build());
        PasswordUpdateRequest request = new PasswordUpdateRequest("newPassword");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPassword")).willReturn("newHash");
        given(userRepository.save(user)).willReturn(user);

        // when
        userService.updatePassword(userId, request);

        // then
        assertThat(user.getPassword()).isEqualTo("newHash");
    }

    // --- 4. 회원 탈퇴 (deleteUser) ---

    @Test
    @DisplayName("회원 탈퇴 성공 - 연관 데이터 삭제 호출 확인")
    void deleteUser_Success() {
        // given
        Long userId = 1L;
        // 프로필 이미지가 있는 유저
        User user = spy(User.builder().userId(userId).profileImage("profile.jpg").build());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.deleteUser(userId);

        // then
        // 1. 프로필 이미지 파일 삭제 확인
        verify(fileStorageService).deleteFile("profile.jpg");

        // 2. 다른 서비스들의 softDelete 메서드 호출 확인 (Facade 역할 검증)
        verify(postService).softDeleteByUserId(userId);
        verify(eventService).softDeleteByUserId(userId);
        verify(clubJoinService).softDeleteByUserId(userId);
        verify(eventJoinService).softDeleteByUserId(userId);

        // 3. 유저 본인 삭제(Soft Delete) 및 Flush 확인
        verify(user).delete();
        verify(em).flush();
        verify(em).clear();
    }
}