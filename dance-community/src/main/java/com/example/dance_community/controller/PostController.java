package com.example.dance_community.controller;

import com.example.dance_community.dto.ApiResponse;
import com.example.dance_community.dto.like.PostLikeResponse;
import com.example.dance_community.dto.post.PostCreateRequest;
import com.example.dance_community.dto.post.PostResponse;
import com.example.dance_community.dto.post.PostUpdateRequest;
import com.example.dance_community.enums.ImageType;
import com.example.dance_community.security.UserDetail;
import com.example.dance_community.service.FileStorageService;
import com.example.dance_community.service.PostLikeService;
import com.example.dance_community.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "5_Post", description = "게시물 관련 API")
public class PostController {
    private final PostService postService;
    private final PostLikeService postLikeService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "게시물 생성", description = "게시물을 새로 작성합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserDetail userDetail,
            @RequestParam("scope") String scope,
            @RequestParam(value = "clubId", required = false) Long clubId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        List<String> imagePaths = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String path = fileStorageService.saveImage(image, ImageType.POST);
                imagePaths.add(path);
            }
        }

        PostCreateRequest postCreateRequest = new PostCreateRequest(
                scope, clubId, title, content, tags, imagePaths
        );

        PostResponse postResponse = postService.createPost(userDetail.getUserId(), postCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("게시물 생성 성공", postResponse));
    }

    @Operation(summary = "게시물 조회", description = "게시물 id를 통해 정보를 불러옵니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long postId
    ) {
        PostResponse postResponse = postService.getPost(postId, userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("게시물 조회 성공", postResponse));
    }

    @Operation(summary = "전체 게시물 조회", description = "전체 게시물의 정보를 불러옵니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getPosts(
            @AuthenticationPrincipal UserDetail userDetail
    ) {
        List<PostResponse> postResponses = postService.getPosts(userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("게시글 전체 조회 성공", postResponses));
    }

    @Operation(summary = "[Hot Groove] ", description = "최근 7일간 작성된 글 중 좋아요 순 상위 10개")
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getHotPosts(
            @AuthenticationPrincipal UserDetail userDetail
    ) {
        List<PostResponse> postResponses = postService.getHotPosts(userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("Hot Groove 조회 성공", postResponses));
    }

    @Operation(summary = "[My Club News]", description = "내가 가입한 클럽의 최신글 조회")
    @GetMapping("/my-club")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getMyClubPosts(
            @AuthenticationPrincipal UserDetail userDetail
    ) {
        List<PostResponse> postResponses = postService.getMyClubPosts(userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("My Club News 조회 성공", postResponses));
    }

    @Operation(summary = "내 게시물 수정", description = "사용자의 게시물을 수정합니다.")
    @PatchMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long postId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "keepImages", required = false) List<String> keepImages
    ) {
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String path = fileStorageService.saveImage(image, ImageType.POST);
                keepImages.add(path);
            }
        }

        PostUpdateRequest request = new PostUpdateRequest(
                title, content, tags, keepImages, keepImages
        );

        PostResponse postResponse = postService.updatePost(postId, userDetail.getUserId(), request);
        return ResponseEntity.ok(new ApiResponse<>("게시물 수정 성공", postResponse));
    }

    @Operation(summary = "게시물 삭제", description = "게시물 id를 통해 정보를 삭제합니다.")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long postId
    ) {
        postService.deletePost(userDetail.getUserId(), postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시물 좋아요", description = "게시물 '좋아요' 버튼을 누릅니다.")
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> toggleLike(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long postId
    ) {
        PostLikeResponse response = postLikeService.toggleLike(userDetail.getUserId(), postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("게시물 좋아요 성공", response));
    }
}
