package com.example.dance_community.repository;

import com.example.dance_community.entity.Post;
import com.example.dance_community.entity.PostLike;
import com.example.dance_community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    boolean existsByPostPostIdAndUserUserId(Long postId, Long userId);
    @Query("SELECT pl.post.postId FROM PostLike pl " +
            "WHERE pl.user.userId = :userId AND pl.post.postId IN :postIds")
    Set<Long> findLikedPostIds(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);
}