package com.example.dance_community.repository;

import com.example.dance_community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    void updateViewCount(@Param("postId") Long postId);

    @Modifying()
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.author.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId);

    @Modifying()
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.club.clubId = :clubId")
    void softDeleteByClubId(@Param("clubId") Long clubId);
}