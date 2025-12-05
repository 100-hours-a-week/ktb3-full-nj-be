package com.example.dance_community.service;

import com.example.dance_community.dto.post.PostCreateRequest;
import com.example.dance_community.dto.post.PostResponse;
import com.example.dance_community.dto.post.PostUpdateRequest;
import com.example.dance_community.entity.Club;
import com.example.dance_community.entity.Post;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.Scope;
import com.example.dance_community.exception.AccessDeniedException;
import com.example.dance_community.exception.InvalidRequestException;
import com.example.dance_community.repository.ClubJoinRepository;
import com.example.dance_community.repository.PostLikeRepository;
import com.example.dance_community.repository.PostRepository;
import com.example.dance_community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private ClubAuthService clubAuthService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ClubJoinRepository clubJoinRepository; // getMyClubPosts 등에서 사용될 수 있음

    // --- 1. 게시글 생성 (createPost) ---

    @Test
    @DisplayName("게시글 생성 성공 - GLOBAL 범위")
    void createPost_Success_Global() {
        // given
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        PostCreateRequest request = new PostCreateRequest(
                "GLOBAL", null, "Title", "Content", List.of("tag1"), List.of("img1")
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // 저장될 객체 Mocking
        Post savedPost = Post.builder()
                .author(user).scope(Scope.GLOBAL).title("Title").content("Content").build();
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponse response = postService.createPost(userId, request);

        // then
        assertThat(response.scope()).isEqualTo("GLOBAL");
        verify(clubAuthService, never()).findByClubId(any()); // 클럽 조회 안 함
    }

    @Test
    @DisplayName("게시글 생성 성공 - CLUB 범위")
    void createPost_Success_Club() {
        // given
        Long userId = 1L;
        Long clubId = 10L;
        User user = User.builder().userId(userId).build();
        Club club = Club.builder().clubId(clubId).build();

        PostCreateRequest request = new PostCreateRequest(
                "CLUB", clubId, "Title", "Content", null, null
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(clubAuthService.findByClubId(clubId)).willReturn(club);

        Post savedPost = Post.builder()
                .author(user).scope(Scope.CLUB).club(club).title("Title").build();
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponse response = postService.createPost(userId, request);

        // then
        assertThat(response.scope()).isEqualTo("CLUB");
        assertThat(response.clubId()).isEqualTo(clubId);
    }

    @Test
    @DisplayName("게시글 생성 실패 - CLUB인데 clubId 누락")
    void createPost_Fail_NoClubId() {
        // given
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        PostCreateRequest request = new PostCreateRequest(
                "CLUB", null, "Title", "Content", null, null // clubId null
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThrows(InvalidRequestException.class, () -> postService.createPost(userId, request));
    }

    // --- 2. 게시글 조회 (getHotPosts, getMyClubPosts) ---

    @Test
    @DisplayName("Hot Groove 조회 성공 - 좋아요 여부 포함")
    void getHotPosts_Success() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        User user = User.builder().userId(userId).build();

        // Post 객체 (엔티티에 필요한 필드 채우기)
        Post post = Post.builder()
                .postId(postId) // ID 필수
                .title("Hot Post")
                .author(user)   // 작성자 필수 (DTO 변환 시 사용)
                .scope(Scope.GLOBAL)
                .likeCount(50L)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        given(postRepository.findHotPosts(pageable)).willReturn(List.of(post));

        // 좋아요 누른 상태라고 가정
        given(postLikeRepository.findLikedPostIds(List.of(postId), userId)).willReturn(Set.of(postId));

        // when
        List<PostResponse> responses = postService.getHotPosts(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().postId()).isEqualTo(postId);
        assertThat(responses.getFirst().isLiked()).isTrue(); // 좋아요 체크 확인
    }

    // --- 3. 게시글 수정 (updatePost) ---

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        User user = User.builder().userId(userId).build();

        // 기존 게시글 (작성자가 본인)
        Post post = Post.builder().postId(postId).scope(Scope.GLOBAL).author(user).title("Old").content("Old").build();

        PostUpdateRequest request = new PostUpdateRequest("New", "New", null, null, null);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        PostResponse response = postService.updatePost(postId, userId, request);

        // then
        assertThat(response.title()).isEqualTo("New");
        // 이미지 처리 메서드 호출 확인
        verify(fileStorageService, times(1)).processImageUpdate(any(Post.class), any(), any());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자가 아님")
    void updatePost_Fail_NotAuthor() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        User otherUser = User.builder().userId(otherUserId).build();

        Post post = Post.builder().postId(100L).author(otherUser).build(); // 작성자 다름

        given(postRepository.findById(100L)).willReturn(Optional.of(post));

        // when & then
        assertThrows(AccessDeniedException.class, () ->
                postService.updatePost(100L, userId, new PostUpdateRequest("T", "C", null, null, null))
        );
    }

    // --- 4. 게시글 삭제 (deletePost) ---

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().userId(userId).build();
        // Post가 Mock 객체여야 delete() 호출 여부 검증 가능 (또는 Spy 사용)
        // 여기서는 실제 객체의 delete 필드 변경을 확인하거나, void 메서드라 호출만 확인
        Post post = spy(Post.builder().postId(100L).author(user).build());

        given(postRepository.findById(100L)).willReturn(Optional.of(post));

        // when
        postService.deletePost(userId, 100L);

        // then
        verify(post, times(1)).delete(); // Soft delete 메서드 호출 확인
    }
}