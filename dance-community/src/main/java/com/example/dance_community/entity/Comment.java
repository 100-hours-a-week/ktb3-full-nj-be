package com.example.dance_community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)

@Table(name = "comments")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE comments SET is_deleted = true WHERE comment_id = ?")
public class Comment extends BaseEntity {

    // 댓글 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    // 행사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // 댓글 내용
    @Column(nullable = false, length = 500)
    private String content;

    @PrePersist
    @PreUpdate
    public void validateComment() {
        if (post == null && event == null) {
            throw new IllegalStateException("댓글은 게시글 또는 행사에 반드시 속해야 합니다.");
        }
        if (post != null && event != null) {
            throw new IllegalStateException("댓글은 게시글과 행사에 동시에 속할 수 없습니다.");
        }
    }

    public void updateContent(String content) {
        this.content = content;
    }
}