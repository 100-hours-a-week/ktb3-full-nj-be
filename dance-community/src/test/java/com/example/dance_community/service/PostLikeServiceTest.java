package com.example.dance_community.service;

import com.example.dance_community.dto.like.PostLikeResponse;
import com.example.dance_community.entity.Post;
import com.example.dance_community.entity.PostLike;
import com.example.dance_community.entity.User;
import com.example.dance_community.repository.PostLikeRepository;
import com.example.dance_community.repository.PostRepository;
import com.example.dance_community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;

    @Test
    @DisplayName("좋아요 추가 성공 (Toggle On)")
    void toggleLike_Add() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        User user = User.builder().userId(userId).build();
        // Post에 likeCount 초기값 설정 필요 (Increment 로직 검증용이지만, Entity 로직이라 여기선 값 변화 확인 어려울 수 있음)
        Post post = Post.builder().postId(postId).likeCount(0L).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // 기존 좋아요 없음
        given(postLikeRepository.findByPostAndUser(post, user)).willReturn(Optional.empty());

        // when
        PostLikeResponse response = postLikeService.toggleLike(userId, postId);

        // then
        assertThat(response.isLiked()).isTrue(); // true여야 함
        verify(postLikeRepository, times(1)).save(any(PostLike.class)); // 저장 호출됨
        // Post 엔티티 내부 로직인 incrementLikeCount()는 여기서 검증하기 어려움 (통합 테스트 영역)
        // 하지만 response.likeCount() 값은 증가된 값이어야 함
        assertThat(response.likeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 취소 성공 (Toggle Off)")
    void toggleLike_Remove() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        User user = User.builder().userId(userId).build();
        Post post = Post.builder().postId(postId).likeCount(1L).build(); // 이미 1개

        // 기존 좋아요 있음
        PostLike existingLike = PostLike.builder().post(post).user(user).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostAndUser(post, user)).willReturn(Optional.of(existingLike));

        // when
        PostLikeResponse response = postLikeService.toggleLike(userId, postId);

        // then
        assertThat(response.isLiked()).isFalse(); // false여야 함
        verify(postLikeRepository, times(1)).delete(existingLike); // 삭제 호출됨
        assertThat(response.likeCount()).isEqualTo(0L); // 감소됨
    }
}