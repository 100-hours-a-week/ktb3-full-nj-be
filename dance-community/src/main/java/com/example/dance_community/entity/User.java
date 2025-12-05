package com.example.dance_community.entity;

import com.example.dance_community.enums.EventJoinStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)

@Table(name = "users")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE user_id = ?")
public class User extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 100, updatable = false)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 255, columnDefinition = "TEXT")
    private String profileImage;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<ClubJoin> clubs = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "host")
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "participant")
    @Builder.Default
    private List<EventJoin> eventJoins = new ArrayList<>();

    // CREATE
    public User(String email, String password, String nickname, String profileImage) {
        checkNullOrBlank(email, "이메일");
        checkNullOrBlank(password, "비밀번호");
        checkNullOrBlank(nickname, "사용자 이름");

        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    // UPDATE
    public User updateUser(String nickname, String profileImage) {
        if (nickname == null && nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이름 null 값 입력 불가");
        }

        this.nickname = nickname;
        this.profileImage = profileImage;

        return this;
    }
    public User updatePassword(String password) {
        checkNullOrBlank(password, "비밀번호");
        this.password = password;

        return this;
    }

    // DELETE IMAGE
    public User deleteImage() {
        this.profileImage = null;
        return this;
    }

    // Convenience Methods for Post
    public void addPost(Post post) {
        this.posts.add(post);
        post.setAuthor(this);
    }
    public void removePost(Post post) {
        this.posts.remove(post);
        post.setAuthor(null);
    }

    // Convenience Methods for Event
    public void addEvent(Event event) {
        this.events.add(event);
        event.setHost(this);
    }
    public void removeEvent(Event event) {
        this.events.remove(event);
        event.setHost(null);
    }

    // Convenience Methods for EventJoin
    public void addEventJoin(Event event, EventJoinStatus status) {
        EventJoin newEventJoin = EventJoin.builder()
                .participant(this)
                .event(event)
                .status(status)
                .build();
        this.eventJoins.add(newEventJoin);
        event.getParticipants().add(newEventJoin);
    }
    public void removeEventJoin(Event event) {
        eventJoins.removeIf(ej -> ej.getEvent().equals(event));
        event.getParticipants().removeIf(ej -> ej.getParticipant().equals(this));
    }

    // Check Methods
    private void checkNullOrBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName+" 미입력");
        }
    }
}