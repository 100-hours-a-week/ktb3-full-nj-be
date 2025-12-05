package com.example.dance_community.service;

import com.example.dance_community.dto.event.EventCreateRequest;
import com.example.dance_community.dto.event.EventResponse;
import com.example.dance_community.dto.event.EventUpdateRequest;
import com.example.dance_community.entity.Event;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.EventType;
import com.example.dance_community.enums.Scope;
import com.example.dance_community.exception.InvalidRequestException;
import com.example.dance_community.repository.EventJoinRepository;
import com.example.dance_community.repository.EventLikeRepository;
import com.example.dance_community.repository.EventRepository;
import com.example.dance_community.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventLikeRepository eventLikeRepository;
    @Mock
    private ClubAuthService clubAuthService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private EventJoinRepository eventJoinRepository;
    @Mock
    private EntityManager entityManager;

    // --- 1. 행사 생성 (createEvent) ---

    @Test
    @DisplayName("행사 생성 성공 - GLOBAL 범위")
    void createEvent_Success_Global() {
        // given
        Long userId = 1L;
        User host = User.builder().userId(userId).build();

        EventCreateRequest request = new EventCreateRequest(
                "GLOBAL", null, "WORKSHOP", "Title", "Content",
                List.of("tag"), null, "Loc", "Addr", "Link",
                50L, LocalDateTime.now(), LocalDateTime.now().plusHours(2)
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(host));

        // 저장될 객체 Mocking
        Event savedEvent = Event.builder()
                .host(host)
                .scope(Scope.GLOBAL)
                .type(EventType.WORKSHOP)
                .title("Title")
                .build();
        given(eventRepository.save(any(Event.class))).willReturn(savedEvent);

        // when
        EventResponse response = eventService.createEvent(userId, request);

        // then
        assertThat(response.scope()).isEqualTo("GLOBAL");
        assertThat(response.type()).isEqualTo("WORKSHOP");
        verify(clubAuthService, never()).findByClubId(any());
    }

    @Test
    @DisplayName("행사 생성 실패 - 잘못된 Scope")
    void createEvent_Fail_InvalidScope() {
        // given
        Long userId = 1L;
        User host = User.builder().userId(userId).build();
        EventCreateRequest request = new EventCreateRequest(
                "INVALID", null, "WORKSHOP", "Title", "Content", null, null, null, null, null, 10L, null, null
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(host));

        // when & then
        assertThrows(InvalidRequestException.class, () -> eventService.createEvent(userId, request));
    }

    @Test
    @DisplayName("행사 생성 실패 - CLUB인데 clubId 누락")
    void createEvent_Fail_NoClubId() {
        // given
        Long userId = 1L;
        User host = User.builder().userId(userId).build();
        EventCreateRequest request = new EventCreateRequest(
                "CLUB", null, "WORKSHOP", "Title", "Content", null, null, null, null, null, 10L, null, null
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(host));

        // when & then
        assertThrows(InvalidRequestException.class, () -> eventService.createEvent(userId, request));
    }

    // --- 2. 행사 조회 (getEvent, getUpcomingEvents) ---

    @Test
    @DisplayName("행사 상세 조회 성공 - 조회수 증가 확인")
    void getEvent_Success() {
        // given
        Long eventId = 100L;
        Long userId = 1L;
        User host = User.builder().userId(userId).build();
        Event event = Event.builder().eventId(eventId).scope(Scope.GLOBAL).type(EventType.BATTLE).host(host).build();

        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventLikeRepository.existsByEventEventIdAndUserUserId(eventId, userId)).willReturn(true);

        // when
        EventResponse response = eventService.getEvent(eventId, userId);

        // then
        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.isLiked()).isTrue();
        // updateViewCount 호출 확인 (엔티티 메서드 or 레포지토리 메서드)
        // 현재 코드에서는 eventRepository.updateViewCount(eventId)를 호출함
        verify(eventRepository, times(1)).updateViewCount(eventId);
    }

    @Test
    @DisplayName("다가오는 행사 목록 조회 성공 - 좋아요 매핑 확인")
    void getUpcomingEvents_Success() {
        // given
        Long userId = 1L;
        Long eventId = 100L;

        // Event 객체 준비 (Host 필수)
        User host = User.builder().userId(2L).build();
        Event event = Event.builder().eventId(eventId).scope(Scope.GLOBAL).type(EventType.BATTLE).host(host).build();

        List<Long> myClubIds = List.of(10L);
        given(clubAuthService.findUserClubIds(userId)).willReturn(myClubIds);

        // QueryDSL 메서드 Mocking
        given(eventRepository.findUpcomingEvents(eq(myClubIds), any()))
                .willReturn(List.of(event));

        // 좋아요 누른 상태
        given(eventLikeRepository.findLikedEventIds(any(), eq(userId)))
                .willReturn(Set.of(eventId));

        // when
        List<EventResponse> responses = eventService.getUpcomingEvents(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().isLiked()).isTrue();
    }

    // --- 3. 행사 수정 (updateEvent) ---

    @Test
    @DisplayName("행사 수정 성공")
    void updateEvent_Success() {
        // given
        Long userId = 1L;
        Long eventId = 100L;
        User host = User.builder().userId(userId).build();

        // Spy 객체 사용 (내부 메서드 호출 확인용) 또는 Mock
        // 여기서는 단순 Mocking으로 진행
        Event realEvent = Event.builder()
                .eventId(eventId)
                .host(host)
                .scope(Scope.GLOBAL)       // 필수
                .type(EventType.WORKSHOP)  // 필수
                .title("Old Title")
                .content("Old Content")
                .build();

        Event event = spy(realEvent);

        // Host 정보 접근을 위해 Stubbing
        given(event.getHost()).willReturn(host);
        given(event.getEventId()).willReturn(eventId);

        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

        EventUpdateRequest request = new EventUpdateRequest(
                "New Title", "New Content", null, null, null, null, null, null, 100L, null, null
        );

        // when
        eventService.updateEvent(userId, eventId, request);

        // then
        // updateEvent 메서드가 호출되었는지 검증
        verify(event).updateEvent(
                eq("New Title"), eq("New Content"), any(), any(), any(), any(), eq(100L), any(), any()
        );
        // 이미지 처리 위임 확인
        verify(fileStorageService).processImageUpdate(eq(event), any(), any());
    }

    // --- 4. 행사 삭제 (deleteEvent) ---

    @Test
    @DisplayName("행사 삭제 성공 - Soft Delete")
    void deleteEvent_Success() {
        // given
        Long userId = 1L;
        Long eventId = 100L;
        User host = User.builder().userId(userId).build();
        // Event spy
        Event event = spy(Event.builder().eventId(eventId).host(host).build());

        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

        // when
        eventService.deleteEvent(userId, eventId);

        // then
        verify(event).delete(); // 엔티티의 delete() 메서드 호출 확인 (isDeleted = true)
        // EntityManager flush/clear는 단위 테스트에서 Mocking하기 까다로우므로 생략 가능
    }
}