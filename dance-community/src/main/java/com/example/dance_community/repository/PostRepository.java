package com.example.dance_community.repository;

import com.example.dance_community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    void updateViewCount(@Param("postId") Long postId);

    @Modifying()
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.author.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId);

    @Modifying()
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.club.clubId = :clubId")
    void softDeleteByClubId(@Param("clubId") Long clubId);

    // [Hot Groove] 최근 1주일간 좋아요 많은 순 (Top N)
    @Query("SELECT p FROM Post p " +
            "WHERE p.createdAt > :oneWeekAgo " +
            "AND p.isDeleted = false " +
            "ORDER BY p.likeCount DESC")
    List<Post> findHotPosts(@Param("oneWeekAgo") LocalDateTime oneWeekAgo, Pageable pageable);

    // [My Club News] 내 동아리 최신글 (Top N)
    @Query("SELECT p FROM Post p " +
            "JOIN p.club c " +
            "JOIN c.members m " +
            "WHERE m.user.userId = :userId " +
            "AND m.status = 'ACTIVE' " +
            "AND p.isDeleted = false " +
            "ORDER BY p.createdAt DESC")
    List<Post> findMyClubPosts(@Param("userId") Long userId, Pageable pageable);
}