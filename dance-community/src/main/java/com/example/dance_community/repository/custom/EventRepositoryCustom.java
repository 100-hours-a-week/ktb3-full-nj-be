package com.example.dance_community.repository.custom;

import com.example.dance_community.entity.Event;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventRepositoryCustom {
    // 접근 가능한 모든 행사 조회
    List<Event> findAllEvents(List<Long> myClubIds);

    // 다가오는 행사 조회
    List<Event> findUpcomingEvents(List<Long> myClubIds, Pageable pageable);
}
