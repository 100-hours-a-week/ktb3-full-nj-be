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
import com.example.dance_community.repository.PostRepository;
import com.example.dance_community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
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

        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse getPost(Long postId) {
        return PostResponse.from(getActivePost(postId));
    }
    public List<PostResponse> getPosts() {
        return postRepository.findAll().stream().map(PostResponse::from).toList();
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
        Post post = getActivePost(postId);

        checkAuthor(userId, post);

        post.updatePost(request.getTitle(), request.getContent(), request.getTags());
        handleImageUpdate(post, request.getNewImagePaths(), request.getKeepImages());

        return PostResponse.from(post);
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
            throw new AccessDeniedException("수정 권한이 없습니다");
        }
    }
    private void handleImageUpdate(Post post, List<String> newImages, List<String> keepImages) {
        // keepImages가 null이면 이미지 변경 안 함
        if (keepImages == null) {
            System.out.println("📷 이미지 변경 없음 - keepImages가 null");

            // 새 이미지만 추가
            if (newImages != null && !newImages.isEmpty()) {
                List<String> currentImages = new ArrayList<>(post.getImages());
                currentImages.addAll(newImages);
                post.updateImages(currentImages);
                System.out.println("📷 새 이미지만 추가: " + newImages.size() + "개");
            }
            return;
        }

        // keepImages가 빈 리스트면 모든 이미지 삭제
        List<String> currentImages = post.getImages();
        List<String> finalImages = new ArrayList<>();

        if (keepImages.isEmpty()) {
            // 모든 기존 이미지 삭제
            System.out.println("🗑️ 모든 기존 이미지 삭제 요청");
            for (String imagePath : currentImages) {
                fileStorageService.deleteFile(imagePath);
                System.out.println("🗑️ 삭제: " + imagePath);
            }
        } else {
            // 유지할 이미지만 남기고 나머지 삭제
            finalImages.addAll(keepImages);

            List<String> imagesToDelete = currentImages.stream()
                    .filter(img -> !keepImages.contains(img))
                    .collect(Collectors.toList());

            for (String imagePath : imagesToDelete) {
                fileStorageService.deleteFile(imagePath);
                System.out.println("삭제: " + imagePath);
            }

            System.out.println("유지: " + keepImages.size() + "개");
        }

        // 새 이미지 추가
        if (newImages != null && !newImages.isEmpty()) {
            finalImages.addAll(newImages);
            System.out.println("새 이미지 추가: " + newImages.size() + "개");
        }

        // DB 업데이트
        post.updateImages(finalImages);
        System.out.println("최종 이미지: " + finalImages.size() + "개");
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
