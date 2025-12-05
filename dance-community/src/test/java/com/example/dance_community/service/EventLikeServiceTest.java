package com.example.dance_community.service;

import com.example.dance_community.dto.like.EventlikeResponse;
import com.example.dance_community.entity.Event;
import com.example.dance_community.entity.EventLike;
import com.example.dance_community.entity.User;
import com.example.dance_community.repository.EventLikeRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventLikeServiceTest {

    @InjectMocks
    private EventLikeService eventLikeService;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventLikeRepository eventLikeRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("좋아요 추가 성공")
    void toggleLike_Add() {
        // given
        Long userId = 1L;
        Long eventId = 100L;
        User user = User.builder().userId(userId).build();
        Event event = Event.builder().eventId(eventId).likeCount(0L).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventLikeRepository.findByEventAndUser(event, user)).willReturn(Optional.empty());

        // when
        EventlikeResponse response = eventLikeService.toggleLike(userId, eventId);

        // then
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(eventLikeRepository, times(1)).save(any(EventLike.class));
    }

    @Test
    @DisplayName("좋아요 취소 성공")
    void toggleLike_Remove() {
        // given
        Long userId = 1L;
        Long eventId = 100L;
        User user = User.builder().userId(userId).build();
        Event event = Event.builder().eventId(eventId).likeCount(1L).build();
        EventLike existingLike = EventLike.builder().event(event).user(user).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        given(eventLikeRepository.findByEventAndUser(event, user)).willReturn(Optional.of(existingLike));

        // when
        EventlikeResponse response = eventLikeService.toggleLike(userId, eventId);

        // then
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(0L);
        verify(eventLikeRepository, times(1)).delete(existingLike);
    }
}