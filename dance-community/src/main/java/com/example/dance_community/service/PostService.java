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
import com.example.dance_community.exception.NotFoundException;
import com.example.dance_community.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final ClubAuthService clubAuthService;
    private final FileStorageService fileStorageService;

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        Scope scope;
        try {
            scope = Scope.valueOf(request.getScope().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidRequestException("잘못된 공개 범위입니다. (GLOBAL, CLUB 중 선택)");
        }

        Club club = null;
        if (scope == Scope.CLUB) {
            Long clubId = request.getClubId();
            if (clubId == null) {
                throw new InvalidRequestException("공개 범위가 CLUB일 경우 clubId가 필요합니다.");
            }
            club = clubAuthService.findByClubId(clubId);
        }

        Post post = Post.builder()
                .author(author)
                .scope(scope)
                .club(club)
                .title(request.getTitle())
                .content(request.getContent())
                .tags(request.getTags())
                .images(request.getImages())
                .build();

        return PostResponse.from(postRepository.save(post), false);
    }

    @Transactional
    public PostResponse getPost(Long postId, Long userId) {
        Post post = getActivePost(postId);
        postRepository.updateViewCount(postId);

        boolean isLiked = userId != null && postLikeRepository.existsByPostPostIdAndUserUserId(postId, userId);

        return PostResponse.from(post, isLiked);
    }
    public List<PostResponse> getPosts(Long userId) {
        List<Long> myClubIds = clubAuthService.findUserClubIds(userId);
        List<Post> posts = postRepository.findAllPosts(myClubIds);
        return convertToResponses(posts, userId);
    }
    public List<PostResponse> getHotPosts(Long userId) {
        List<Long> myClubIds = clubAuthService.findUserClubIds(userId);
        Pageable pageable = PageRequest.of(0, 10);

        List<Post> posts = postRepository.findHotPosts(myClubIds, pageable);
        return convertToResponses(posts, userId);
    }
    public List<PostResponse> getMyClubPosts(Long userId) {
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = postRepository.findMyClubPosts(userId, pageable);
        return convertToResponses(posts, userId);
    }
    private List<PostResponse> convertToResponses(List<Post> posts, Long userId) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream().map(Post::getPostId).toList();

        Set<Long> likedPostIds = userId != null
                ? postLikeRepository.findLikedPostIds(postIds, userId) : new HashSet<>();

        return posts.stream()
                .map(post -> PostResponse.from(post, likedPostIds.contains(post.getPostId())))
                .toList();
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
        Post post = getActivePost(postId);
        checkAuthor(userId, post);

        post.updatePost(request.getTitle(), request.getContent(), request.getTags());
        fileStorageService.processImageUpdate(post, request.getNewImagePaths(), request.getKeepImages());
        boolean isLiked = postLikeRepository.existsByPostPostIdAndUserUserId(postId, userId);

        return PostResponse.from(post, isLiked);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getActivePost(postId);
        checkAuthor(userId, post);
        post.delete();
    }

    private Post getActivePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시물을 찾을 수 없습니다"));
    }
    private void checkAuthor(Long userId, Post post) {
        if (!post.getAuthor().getUserId().equals(userId)) {
            throw new AccessDeniedException("권한이 없습니다");
        }
    }

    @Transactional
    public void softDeleteByUserId(Long userId) {
        postRepository.softDeleteByUserId(userId);
    }
    @Transactional
    public void softDeleteByClubId(Long clubId) {
        postRepository.softDeleteByClubId(clubId);
    }
}
