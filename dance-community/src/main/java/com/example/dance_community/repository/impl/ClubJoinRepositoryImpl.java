package com.example.dance_community.repository.impl;

import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.enums.ClubJoinStatus;
import com.example.dance_community.repository.custom.ClubJoinRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.dance_community.entity.QClubJoin.clubJoin;
import static com.example.dance_community.entity.QClub.club;
import static com.example.dance_community.entity.QUser.user;

@RequiredArgsConstructor
public class ClubJoinRepositoryImpl implements ClubJoinRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 내 동아리 목록 (Club 정보 한 방에 가져오기)
    @Override
    public List<ClubJoin> findMyClubJoins(Long userId, List<ClubJoinStatus> statuses) {
        return queryFactory
                .selectFrom(clubJoin)
                .join(clubJoin.club, club).fetchJoin()
                .where(
                        clubJoin.user.userId.eq(userId),
                        clubJoin.status.in(statuses)
                )
                .orderBy(clubJoin.createdAt.desc())
                .fetch();
    }

    // 멤버 목록 (User 정보 한 방에 가져오기)
    @Override
    public List<ClubJoin> findClubMembers(Long clubId, ClubJoinStatus status) {
        return queryFactory
                .selectFrom(clubJoin)
                .join(clubJoin.user, user).fetchJoin()
                .where(
                        clubJoin.club.clubId.eq(clubId),
                        clubJoin.status.eq(status)
                )
                .orderBy(
                        clubJoin.role.asc(),
                        clubJoin.createdAt.asc()
                )
                .fetch();
    }
}