package com.example.dance_community.controller;

import com.example.dance_community.dto.club.ClubCreateRequest;
import com.example.dance_community.dto.club.ClubResponse;
import com.example.dance_community.dto.club.ClubUpdateRequest;
import com.example.dance_community.enums.ClubType;
import com.example.dance_community.enums.ImageType;
import com.example.dance_community.security.JwtFilter;
import com.example.dance_community.security.JwtUtil;
import com.example.dance_community.security.WithCustomMockUser;
import com.example.dance_community.service.ClubService;
import com.example.dance_community.service.FileStorageService;
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
        controllers = ClubController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ClubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubService clubService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private ClubResponse createMockResponse() {
        return new ClubResponse(
                1L, "Club Name", "Intro", "Desc", "Seoul",
                ClubType.CLUB, "img.jpg", List.of("tag"),
                10L, LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("클럽 생성 성공")
    @WithCustomMockUser
    void createClub_Success() throws Exception {
        MockMultipartFile image = new MockMultipartFile("clubImage", "test.jpg", "image/jpeg", "data".getBytes());
        given(fileStorageService.saveImage(any(), eq(ImageType.CLUB))).willReturn("path/img.jpg");
        given(clubService.createClub(any(), any(ClubCreateRequest.class))).willReturn(createMockResponse());

        mockMvc.perform(multipart("/clubs")
                        .file(image)
                        .param("clubName", "Club Name")
                        .param("intro", "Intro")
                        .param("locationName", "Seoul")
                        .param("description", "Desc")
                        .param("clubType", "CLUB")
                        .param("tags", "tag1", "tag2")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("클럽 생성 성공"));
    }

    @Test
    @DisplayName("클럽 조회 성공")
    @WithCustomMockUser
    void getClub_Success() throws Exception {
        given(clubService.getClub(eq(1L))).willReturn(createMockResponse());

        mockMvc.perform(get("/clubs/{clubId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clubId").value(1L));
    }

    @Test
    @DisplayName("클럽 수정 성공")
    @WithCustomMockUser
    void updateClub_Success() throws Exception {
        MockMultipartFile image = new MockMultipartFile("clubImage", "new.jpg", "image/jpeg", "data".getBytes());
        given(clubService.updateClub(any(), eq(1L), any(ClubUpdateRequest.class))).willReturn(createMockResponse());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/clubs/{clubId}", 1L)
                        .file(image)
                        .param("clubName", "Updated Name")
                        .param("intro", "Updated Intro")
                        .param("locationName", "Busan")
                        .param("description", "Updated Desc")
                        .param("clubType", ClubType.CLUB.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("클럽 수정 성공"));
    }

    @Test
    @DisplayName("클럽 이미지 삭제 성공")
    @WithCustomMockUser
    void deleteClubImage_Success() throws Exception {
        given(clubService.deleteClubImage(any(), eq(1L))).willReturn(createMockResponse());

        mockMvc.perform(delete("/clubs/{clubId}/club-image", 1L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("클럽 이미지 삭제 성공"));
    }

    @Test
    @DisplayName("클럽 삭제 성공")
    @WithCustomMockUser
    void deleteClub_Success() throws Exception {
        mockMvc.perform(delete("/clubs/{clubId}", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(clubService).deleteClub(any(), eq(1L));
    }
}