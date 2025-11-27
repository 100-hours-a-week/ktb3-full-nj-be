package com.example.dance_community.entity;

import com.example.dance_community.enums.EventJoinStatus;
import com.example.dance_community.enums.EventType;
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
@Table(name = "events")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE events SET is_deleted = true WHERE event_id = ?")
public class Event extends BaseEntity{
    // 행사 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false ,updatable = false)
    private User host;

    // 공개 범위
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Scope scope;

    // 클럽 ID (Scope.CLUB일 때 대상 클럽)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", updatable = false)
    private Club club;

    // 행사 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EventType type;

    // 행사 관련 내용 (제목, 내용, 태그, 이미지)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @ElementCollection
    @CollectionTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "eventId")
    )
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "event_images",
            joinColumns = @JoinColumn(name = "eventId")
    )
    @Column(name = "image")
    private List<String> images = new ArrayList<>();

    // 행사 장소 정보 (이름, 주소, 링크)
    private String locationName;
    private String locationAddress;
    private String locationLink;

    // 행사 총 수용 인원
    @Column(nullable = false)
    private Long capacity;

    // 행사 참가자 목록
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventJoin> participants = new ArrayList<>();

    // 행사 일시 (시작, 종료 시간)
    @Column(nullable = false)
    private LocalDateTime startsAt;
    @Column(nullable = false)
    private LocalDateTime endsAt;

    // CREATE
    @Builder
    private Event(User host, Scope scope, Club club, EventType type,
                  String title, String content, List<String> tags, List<String> images,
                  String locationName, String locationAddress, String locationLink, Long capacity,
                  LocalDateTime startsAt, LocalDateTime endsAt) {
        validateEvent(host, scope, club, type, title, content, capacity, startsAt, endsAt);

        this.host = host;
        this.scope = scope;
        this.club = club;
        this.type = type;
        this.title = title;
        this.content = content;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.locationLink = locationLink;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
    public Event setHost(User host) {
        this.host = host;
        return this;
    }

    // UPDATE
    public Event updateEvent(String title, String content, List<String> tags, List<String> images,
                            String locationName, String locationAddress, String locationLink, Long capacity,
                            LocalDateTime startsAt, LocalDateTime endsAt) {
        checkNullOrBlank(title, "제목");
        checkNullOrBlank(content, "내용");


        this.title = title;
        this.content = content;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.locationLink = locationLink;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;

        return this;
    }

    // Convenience Methods for EventJoin
    public void addParticipant(User user, EventJoinStatus status) {
        EventJoin newEventJoin = EventJoin.builder()
                .participant(user)
                .event(this)
                .status(status)
                .build();
        this.participants.add(newEventJoin);
        user.getEventJoins().add(newEventJoin);
    }
    public void removeParticipant(User user) {
        participants.removeIf(m -> m.getParticipant().equals(user));
        user.getEventJoins().removeIf(m -> m.getEvent().equals(this));
    }

    // Check Methods
    private void checkNullOrBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName+" 미입력");
        }
    }
    private void validateEvent(User host, Scope scope, Club club, EventType type,
                               String title, String content, Long capacity,
                               LocalDateTime startsAt, LocalDateTime endsAt) {
        if (host == null) {
            throw new IllegalArgumentException("주최자 미입력");
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
        if (type == null) {
            throw new IllegalArgumentException("행사 유형 미입력");
        }
        checkNullOrBlank(title, "제목");
        checkNullOrBlank(content, "내용");
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("행사 총 수용 인원 미입력 또는 0 이하");
        }
        if (startsAt == null) {
            throw new IllegalArgumentException("행사 시작 일시 미입력");
        }
        if (endsAt == null) {
            throw new IllegalArgumentException("행사 종료 일시 미입력");
        }
    }
}