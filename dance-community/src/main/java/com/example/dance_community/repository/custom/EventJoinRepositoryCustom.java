package com.example.dance_community.repository.custom;

import com.example.dance_community.entity.EventJoin;
import com.example.dance_community.enums.EventJoinStatus;

import java.util.List;

public interface EventJoinRepositoryCustom {
    // 행사 참여자 목록 조회 (User 정보 포함)
    List<EventJoin> findParticipantsWithUser(Long eventId, EventJoinStatus status);

    // 내가 참여한 행사 목록 조회
    List<EventJoin> findMyJoinedEvents(Long userId, EventJoinStatus status);
}
