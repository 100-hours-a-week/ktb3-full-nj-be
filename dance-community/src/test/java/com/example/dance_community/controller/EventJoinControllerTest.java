package com.example.dance_community.controller;

import com.example.dance_community.dto.eventJoin.EventJoinResponse;
import com.example.dance_community.security.JwtFilter;
import com.example.dance_community.security.WithCustomMockUser;
import com.example.dance_community.service.EventJoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EventJoinController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JwtFilter.class
                )
        }
)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class EventJoinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventJoinService eventJoinService;

    private EventJoinResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new EventJoinResponse(
                1L, 1L, "Dancer", "test@email.com", "img.jpg",
                100L, "CONFIRMED", LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("행사 신청 API 성공")
    @WithCustomMockUser(userId = 1L)
    void applyEvent_Success() throws Exception {
        Long eventId = 100L;
        given(eventJoinService.applyEvent(any(), eq(eventId))).willReturn(mockResponse);

        mockMvc.perform(post("/events/{eventId}/apply", eventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventJoinId").value(1L))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("행사 신청 취소 API 성공")
    @WithCustomMockUser(userId = 1L)
    void cancelEventJoin_Success() throws Exception {
        Long eventId = 100L;

        mockMvc.perform(delete("/events/{eventId}/apply", eventId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("행사 신청 취소 성공"));
    }

    @Test
    @DisplayName("내 신청 상태 조회 API 성공")
    @WithCustomMockUser(userId = 1L)
    void getMyJoinStatus_Success() throws Exception {
        Long eventId = 100L;
        given(eventJoinService.getJoinStatus(any(), eq(eventId))).willReturn(mockResponse);

        mockMvc.perform(get("/events/{eventId}/my-status", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("내 행사 신청 목록 조회 API 성공")
    @WithCustomMockUser(userId = 1L)
    void getMyEvents_Success() throws Exception {
        given(eventJoinService.getUserEvents(any())).willReturn(List.of(mockResponse));

        mockMvc.perform(get("/events/my-joins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventJoinId").value(1L));
    }

    @Test
    @DisplayName("행사 참여자 목록 조회 API 성공")
    @WithCustomMockUser(userId = 1L)
    void getEventParticipants_Success() throws Exception {
        Long eventId = 100L;
        given(eventJoinService.getEventUsers(eventId)).willReturn(List.of(mockResponse));

        mockMvc.perform(get("/events/{eventId}/participants", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nickname").value("Dancer"));
    }

    @Test
    @DisplayName("행사 신청 거절 API 성공")
    @WithCustomMockUser(userId = 1L)
    void rejectParticipation_Success() throws Exception {
        Long eventId = 100L;
        Long participantId = 2L;

        mockMvc.perform(post("/events/{eventId}/participation/{participantId}/reject", eventId, participantId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("행사 신청 거절 성공"));
    }
}