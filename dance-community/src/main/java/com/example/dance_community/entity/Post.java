package com.example.dance_community.entity;

import com.example.dance_community.enums.Scope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE posts SET is_deleted = true WHERE post_id = ?")
public class Post extends BaseEntity implements ImageHolder{

    // 게시물 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false ,updatable = false)
    private User author;

    // 공개 범위
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Scope scope;

    // 클럽 ID (Scope.CLUB일 때 대상 클럽)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", updatable = false)
    private Club club;

    // 게시물 관련 내용 (제목, 내용, 태그, 이미지)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @ElementCollection
    @CollectionTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "postId")
    )
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "post_images",
            joinColumns = @JoinColumn(name = "postId")
    )
    @Column(name = "image")
    private List<String> images = new ArrayList<>();

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private Long viewCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> likes = new ArrayList<>();

    // CREATE
    @Builder
    private Post(User author, Scope scope, Club club,
                 String title, String content, List<String> tags, List<String> images) {
        validatePost(author, scope, club, title, content);

        this.author = author;
        this.scope = scope;
        this.club = club;
        this.title = title;
        this.content = content;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        this.likeCount = 0L;
        this.viewCount = 0L;
    }

    public Post setAuthor(User author) {
        this.author = author;
        return this;
    }

    // UPDATE
    public Post updatePost(String title, String content, List<String> tags) {
        checkNullOrBlank(title, "제목");
        checkNullOrBlank(content, "내용");

        this.title = title;
        this.content = content;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();

        return this;
    }
    public void updateImages(List<String> images) {
        this.images = images;
    }
    public void incrementLikeCount() {
        this.likeCount++;
    }
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // Check Methods
    private void checkNullOrBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName+" 미입력");
        }
    }
    private void validatePost(User author, Scope scope, Club club, String title, String content) {
        if (author == null) {
            throw new IllegalArgumentException("작성자 미입력");
        }
        if (scope == null) {
            throw new IllegalArgumentException("공개 범위 미입력");
        }
        if (scope == Scope.CLUB && club == null) {
            throw new IllegalArgumentException("공개 범위 : 클럽 -> 클럽 값 필요");
        }
        if (scope == Scope.GLOBAL && club != null) {
            throw new IllegalArgumentException("공개 범위 : 전체 -> 클럽 값 null 이어야 함");
        }
        checkNullOrBlank(title, "제목");
        checkNullOrBlank(content, "내용");
    }
}
