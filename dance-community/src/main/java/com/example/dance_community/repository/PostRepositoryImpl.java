package com.example.dance_community.repository;

import com.example.dance_community.entity.Post;
import com.example.dance_community.enums.ClubJoinStatus;
import com.example.dance_community.enums.Scope;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.dance_community.entity.QPost.post;
import static com.example.dance_community.entity.QClub.club;
import static com.example.dance_community.entity.QUser.user;
import static com.example.dance_community.entity.QClubJoin.clubJoin;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Post> findHotPosts(List<Long> myClubIds, Pageable pageable) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(14);
        return queryFactory
                .selectFrom(post)
                .join(post.author, user).fetchJoin()
                .leftJoin(post.club, club).fetchJoin()
                .where(
                        post.createdAt.gt(oneWeekAgo),
                        post.isDeleted.isFalse(),
                        accessiblePostCondition(myClubIds)
                )
                .orderBy(post.likeCount.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Post> findMyClubPosts(Long userId, Pageable pageable) {
        return queryFactory
                .selectFrom(post)
                .join(post.club, club).fetchJoin()
                .join(post.author, user).fetchJoin()
                .join(club.members, clubJoin)
                .where(
                        clubJoin.user.userId.eq(userId),
                        clubJoin.status.eq(ClubJoinStatus.ACTIVE),
                        post.isDeleted.isFalse()
                )
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression accessiblePostCondition(List<Long> myClubIds) {
        BooleanExpression isGlobal = post.scope.eq(Scope.GLOBAL);

        if (myClubIds == null || myClubIds.isEmpty()) {
            return isGlobal;
        }

        BooleanExpression isMyClubPost = post.scope.eq(Scope.CLUB)
                .and(post.club.clubId.in(myClubIds));

        return isGlobal.or(isMyClubPost);
    }
}