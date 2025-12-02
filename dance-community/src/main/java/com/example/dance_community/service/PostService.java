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
import com.example.dance_community.repository.PostLikeRepository;
import com.example.dance_community.repository.PostRepository;
import com.example.dance_community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        Club club = null;
        if (Scope.CLUB.toString().equals(request.getScope())) {
            Long clubId = request.getClubId();
            if (clubId == null) throw new InvalidRequestException("공개 범위가 CLUB일 경우 clubId가 필요");
            club = clubAuthService.findByClubId(clubId);
        }

        Post post = Post.builder()
                .author(author)
                .scope(Scope.valueOf(request.getScope().toUpperCase()))
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
        return postRepository.findAll().stream()
                .map(post -> {
                    // TODO: 게시글이 많으면 N+1 문제 발생 가능 (추후 QueryDSL 등으로 최적화 권장)
                    boolean isLiked = userId != null
                            && postLikeRepository.existsByPostPostIdAndUserUserId(post.getPostId(), userId);
                    return PostResponse.from(post, isLiked);
                })
                .toList();
    }
    public List<PostResponse> getHotPosts(Long userId) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        Pageable limitTen = PageRequest.of(0, 10);

        return postRepository.findHotPosts(oneWeekAgo, limitTen).stream()
                .map(post -> {
                    boolean isLiked = userId != null
                            && postLikeRepository.existsByPostPostIdAndUserUserId(post.getPostId(), userId);
                    return PostResponse.from(post, isLiked);
                })
                .toList();
    }
    public List<PostResponse> getMyClubPosts(Long userId) {
        Pageable limitThree = PageRequest.of(0, 10);

        return postRepository.findMyClubPosts(userId, limitThree).stream()
                .map(post -> {
                    boolean isLiked = userId != null
                            && postLikeRepository.existsByPostPostIdAndUserUserId(post.getPostId(), userId);
                    return PostResponse.from(post, isLiked);
                })
                .toList();
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
        Post post = getActivePost(postId);

        checkAuthor(userId, post);

        post.updatePost(request.getTitle(), request.getContent(), request.getTags());
        handleImageUpdate(post, request.getNewImagePaths(), request.getKeepImages());
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
    private void handleImageUpdate(Post post, List<String> newImages, List<String> keepImages) {
        if (keepImages == null) {
            if (newImages != null && !newImages.isEmpty()) {
                List<String> currentImages = new ArrayList<>(post.getImages());
                currentImages.addAll(newImages);
                post.updateImages(currentImages);
            }
            return;
        }

        List<String> currentImages = post.getImages();
        List<String> finalImages = new ArrayList<>();

        if (keepImages.isEmpty()) {
            for (String imagePath : currentImages) {
                fileStorageService.deleteFile(imagePath);
            }
        } else {
            finalImages.addAll(keepImages);

            List<String> imagesToDelete = currentImages.stream()
                    .filter(img -> !keepImages.contains(img))
                    .collect(Collectors.toList());

            for (String imagePath : imagesToDelete) {
                fileStorageService.deleteFile(imagePath);
            }
        }

        if (newImages != null && !newImages.isEmpty()) {
            finalImages.addAll(newImages);
        }
        post.updateImages(finalImages);
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
