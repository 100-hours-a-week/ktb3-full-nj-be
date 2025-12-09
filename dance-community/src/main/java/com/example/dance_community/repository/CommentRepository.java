package com.example.dance_community.repository;

import com.example.dance_community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPost_PostId(Long postId, Pageable pageable);
    Page<Comment> findByEvent_EventId(Long eventId, Pageable pageable);
}