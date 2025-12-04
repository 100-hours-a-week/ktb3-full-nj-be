package com.example.dance_community.repository;

import com.example.dance_community.entity.EventJoin;
import com.example.dance_community.enums.EventJoinStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.dance_community.entity.QEventJoin.eventJoin;
import static com.example.dance_community.entity.QUser.user;
import static com.example.dance_community.entity.QEvent.event;

@RequiredArgsConstructor
public class EventJoinRepositoryImpl implements EventJoinRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventJoin> findParticipantsWithUser(Long eventId, EventJoinStatus status) {
        return queryFactory
                .selectFrom(eventJoin)
                .join(eventJoin.participant, user).fetchJoin()
                .where(
                        eventJoin.event.eventId.eq(eventId),
                        eventJoin.status.eq(status)
                )
                .orderBy(eventJoin.createdAt.asc())
                .fetch();
    }

    @Override
    public List<EventJoin> findMyJoinedEvents(Long userId, EventJoinStatus status) {
        return queryFactory
                .selectFrom(eventJoin)
                .join(eventJoin.event, event).fetchJoin()
                .join(event.host, user).fetchJoin()
                .where(
                        eventJoin.participant.userId.eq(userId),
                        eventJoin.status.eq(status)
                )
                .orderBy(eventJoin.createdAt.desc())
                .fetch();
    }
}