package com.example.dance_community.controller;

import com.example.dance_community.dto.event.EventCreateRequest;
import com.example.dance_community.dto.event.EventResponse;
import com.example.dance_community.dto.event.EventUpdateRequest;
import com.example.dance_community.dto.like.EventlikeResponse;
import com.example.dance_community.enums.ImageType;
import com.example.dance_community.security.JwtFilter;
import com.example.dance_community.security.JwtUtil;
import com.example.dance_community.security.WithCustomMockUser;
import com.example.dance_community.service.EventLikeService;
import com.example.dance_community.service.EventService;
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
        controllers = EventController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventLikeService eventLikeService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private EventResponse createMockResponse() {
        return new EventResponse(
                1L, 1L, "Host", null, "GLOBAL", null, null,
                "WORKSHOP", "Title", "Content", List.of("tag"), List.of("img.jpg"),
                "Loc", "Addr", "Link", 50L, 0L,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                0L, 0L, false, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("행사 생성 성공")
    @WithCustomMockUser
    void createEvent_Success() throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg", "data".getBytes());
        given(fileStorageService.saveImage(any(), eq(ImageType.EVENT))).willReturn("path/img.jpg");
        given(eventService.createEvent(any(), any(EventCreateRequest.class))).willReturn(createMockResponse());

        mockMvc.perform(multipart("/events")
                        .file(image)
                        .param("scope", "GLOBAL")
                        .param("type", "WORKSHOP")
                        .param("title", "Title")
                        .param("content", "Content")
                        .param("locationName", "Seoul")
                        .param("capacity", "50")
                        .param("startsAt", LocalDateTime.now().toString())
                        .param("endsAt", LocalDateTime.now().plusHours(2).toString())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("행사 생성 성공"));
    }

    @Test
    @DisplayName("행사 조회 성공")
    @WithCustomMockUser
    void getEvent_Success() throws Exception {
        given(eventService.getEvent(eq(1L), any())).willReturn(createMockResponse());

        mockMvc.perform(get("/events/{eventId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(1L));
    }

    @Test
    @DisplayName("전체 행사 조회 성공")
    @WithCustomMockUser
    void getEvents_Success() throws Exception {
        given(eventService.getEvents(any())).willReturn(List.of(createMockResponse()));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventId").value(1L));
    }

    @Test
    @DisplayName("Upcoming Event 조회 성공")
    @WithCustomMockUser
    void getUpcomingEvents_Success() throws Exception {
        given(eventService.getUpcomingEvents(any())).willReturn(List.of(createMockResponse()));

        mockMvc.perform(get("/events/upcoming"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("행사 수정 성공")
    @WithCustomMockUser
    void updateEvent_Success() throws Exception {
        given(eventService.updateEvent(eq(1L), any(), any(EventUpdateRequest.class))).willReturn(createMockResponse());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/events/{eventId}", 1L)
                        .param("title", "Updated Title")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("행사 수정 성공"));
    }

    @Test
    @DisplayName("행사 삭제 성공")
    @WithCustomMockUser
    void deleteEvent_Success() throws Exception {
        mockMvc.perform(delete("/events/{eventId}", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(any(), eq(1L));
    }

    @Test
    @DisplayName("행사 좋아요 토글 성공")
    @WithCustomMockUser
    void toggleLike_Success() throws Exception {
        EventlikeResponse response = new EventlikeResponse(true, 10L);
        given(eventLikeService.toggleLike(any(), eq(1L))).willReturn(response);

        mockMvc.perform(post("/events/{eventId}/like", 1L)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }
}