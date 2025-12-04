package com.example.dance_community.repository;

import com.example.dance_community.entity.Post;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostRepositoryCustom {
    // 접근 가능한 모든 게시글 조회
    List<Post> findAllPosts(List<Long> myClubIds);

    // [메인 페이지] 인기글 조회
    List<Post> findHotPosts(List<Long> myClubIds, Pageable pageable);

    // [메인 페이지] 내 동아리 소식 조회
    List<Post> findMyClubPosts(Long userId, Pageable pageable);
}
