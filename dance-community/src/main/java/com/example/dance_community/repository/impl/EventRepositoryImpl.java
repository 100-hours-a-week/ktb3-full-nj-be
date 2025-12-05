package com.example.dance_community.repository.impl;

import com.example.dance_community.entity.Event;
import com.example.dance_community.enums.Scope;
import com.example.dance_community.repository.custom.EventRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.dance_community.entity.QEvent.event;
import static com.example.dance_community.entity.QUser.user;
import static com.example.dance_community.entity.QClub.club;

@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Event> findAllEvents(List<Long> myClubIds) {
        return queryFactory
                .selectFrom(event)
                .join(event.host, user).fetchJoin()
                .leftJoin(event.club, club).fetchJoin()
                .where(
                        event.isDeleted.isFalse(),
                        accessibleEventCondition(myClubIds)
                )
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Event> findUpcomingEvents(List<Long> myClubIds, Pageable pageable) {
        return queryFactory
                .selectFrom(event)
                .join(event.host, user).fetchJoin()
                .leftJoin(event.club, club).fetchJoin()
                .where(
                        event.startsAt.gt(LocalDateTime.now()),
                        event.isDeleted.isFalse(),
                        accessibleEventCondition(myClubIds)
                )
                .orderBy(event.startsAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression accessibleEventCondition(List<Long> myClubIds) {
        BooleanExpression isGlobal = event.scope.eq(Scope.GLOBAL);

        if (myClubIds == null || myClubIds.isEmpty()) {
            return isGlobal;
        }

        BooleanExpression isMyClubEvent = event.scope.eq(Scope.CLUB)
                .and(event.club.clubId.in(myClubIds));

        return isGlobal.or(isMyClubEvent);
    }
}