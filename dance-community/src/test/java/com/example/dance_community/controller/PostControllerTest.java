package com.example.dance_community.controller;

import com.example.dance_community.dto.like.PostLikeResponse;
import com.example.dance_community.dto.post.PostCreateRequest;
import com.example.dance_community.dto.post.PostResponse;
import com.example.dance_community.dto.post.PostUpdateRequest;
import com.example.dance_community.enums.ImageType;
import com.example.dance_community.security.JwtFilter;
import com.example.dance_community.security.JwtUtil;
import com.example.dance_community.security.WithCustomMockUser;
import com.example.dance_community.service.FileStorageService;
import com.example.dance_community.service.PostLikeService;
import com.example.dance_community.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PostController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostLikeService postLikeService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // 테스트용 더미 응답 생성
    private PostResponse createMockResponse() {
        return new PostResponse(
                1L, 1L, "User", null, "GLOBAL", null, null,
                "Title", "Content", List.of("tag"), List.of("img.jpg"),
                0L, 0L, false, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("게시글 생성 성공")
    @WithCustomMockUser
    void createPost_Success() throws Exception {
        // given
        MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg", "data".getBytes());
        given(fileStorageService.saveImage(any(), eq(ImageType.POST))).willReturn("path/img.jpg");
        given(postService.createPost(any(), any(PostCreateRequest.class))).willReturn(createMockResponse());

        // when & then
        mockMvc.perform(multipart("/posts")
                        .file(image)
                        .param("scope", "GLOBAL")
                        .param("title", "Title")
                        .param("content", "Content")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("게시물 생성 성공"));
    }

    @Test
    @DisplayName("게시글 조회 성공")
    @WithCustomMockUser
    void getPost_Success() throws Exception {
        given(postService.getPost(eq(1L), any())).willReturn(createMockResponse());

        mockMvc.perform(get("/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(1L));
    }

    @Test
    @DisplayName("전체 게시글 조회 성공")
    @WithCustomMockUser
    void getPosts_Success() throws Exception {
        given(postService.getPosts(any())).willReturn(List.of(createMockResponse()));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(1L));
    }

    @Test
    @DisplayName("Hot Groove 조회 성공")
    @WithCustomMockUser
    void getHotPosts_Success() throws Exception {
        given(postService.getHotPosts(any())).willReturn(List.of(createMockResponse()));

        mockMvc.perform(get("/posts/hot"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 게시글 수정 성공")
    @WithCustomMockUser
    void updatePost_Success() throws Exception {
        // given
        MockMultipartFile image = new MockMultipartFile("images", "new.jpg", "image/jpeg", "data".getBytes());
        given(fileStorageService.saveImage(any(), eq(ImageType.POST))).willReturn("path/new.jpg");
        given(postService.updatePost(eq(1L), any(), any(PostUpdateRequest.class))).willReturn(createMockResponse());

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/posts/{postId}", 1L)
                        .file(image)
                        .param("title", "New Title")
                        .param("keepImages", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시물 수정 성공"));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    @WithCustomMockUser
    void deletePost_Success() throws Exception {
        mockMvc.perform(delete("/posts/{postId}", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(postService).deletePost(any(), eq(1L));
    }

    @Test
    @DisplayName("게시글 좋아요 토글 성공")
    @WithCustomMockUser
    void toggleLike_Success() throws Exception {
        PostLikeResponse response = new PostLikeResponse(true, 10L);
        given(postLikeService.toggleLike(any(), eq(1L))).willReturn(response);

        mockMvc.perform(post("/posts/{postId}/like", 1L)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }
}