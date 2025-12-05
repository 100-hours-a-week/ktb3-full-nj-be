package com.example.dance_community.service;

import com.example.dance_community.dto.eventJoin.EventJoinResponse;
import com.example.dance_community.entity.Event;
import com.example.dance_community.entity.EventJoin;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.EventJoinStatus;
import com.example.dance_community.exception.ConflictException;
import com.example.dance_community.repository.EventJoinRepository;
import com.example.dance_community.repository.EventRepository;
import com.example.dance_community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventJoinServiceTest {

    @InjectMocks
    private EventJoinService eventJoinService; // 테스트 대상 (가짜들을 주입받음)

    @Mock
    private EventJoinRepository eventJoinRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("행사 신청 성공 - 정원이 남아있을 때")
    void applyEvent_Success() {
        // given (준비)
        Long userId = 1L;
        Long eventId = 100L;

        // 가짜 User, Event 생성
        User user = User.builder().userId(userId).build();
        Event event = Event.builder().eventId(eventId).capacity(50L).build(); // 정원 50명

        // Mocking: Repository가 호출되면 이렇게 행동해라! (Stubbing)
        given(eventJoinRepository.findByParticipant_UserIdAndEvent_EventId(userId, eventId))
                .willReturn(Optional.empty()); // 아직 신청 안 함
        given(eventRepository.findWithLockByEventId(eventId))
                .willReturn(Optional.of(event)); // 행사 존재함
        given(eventJoinRepository.countByEvent_EventIdAndStatus(eventId, EventJoinStatus.CONFIRMED))
                .willReturn(49L); // 현재 49명 (1자리 남음)
        given(userRepository.findById(userId))
                .willReturn(Optional.of(user)); // 유저 존재함

        // save 메서드가 호출되면 가짜 객체를 리턴해라
        EventJoin savedJoin = EventJoin.builder().participant(user).event(event).status(EventJoinStatus.CONFIRMED).build();
        given(eventJoinRepository.save(any(EventJoin.class))).willReturn(savedJoin);

        // when (실행)
        EventJoinResponse response = eventJoinService.applyEvent(userId, eventId);

        // then (검증)
        assertThat(response.status()).isEqualTo(EventJoinStatus.CONFIRMED.name()); // 상태가 CONFIRMED인지?
        verify(eventJoinRepository, times(1)).save(any(EventJoin.class)); // save가 1번 호출되었는지?
    }

    @Test
    @DisplayName("행사 신청 실패 - 정원 초과")
    void applyEvent_Fail_FullCapacity() {
        // given
        Long userId = 1L;
        Long eventId = 100L;
        Event event = Event.builder().eventId(eventId).capacity(50L).build();

        given(eventJoinRepository.findByParticipant_UserIdAndEvent_EventId(userId, eventId))
                .willReturn(Optional.empty());
        given(eventRepository.findWithLockByEventId(eventId))
                .willReturn(Optional.of(event));

        // ★ 핵심: 현재 인원이 50명이라고 설정
        given(eventJoinRepository.countByEvent_EventIdAndStatus(eventId, EventJoinStatus.CONFIRMED))
                .willReturn(50L);

        // when & then (예외 발생 검증)
        assertThrows(ConflictException.class, () -> {
            eventJoinService.applyEvent(userId, eventId);
        });
    }
}