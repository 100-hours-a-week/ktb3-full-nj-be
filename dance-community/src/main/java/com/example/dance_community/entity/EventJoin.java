package com.example.dance_community.entity;

import com.example.dance_community.enums.EventJoinStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)

@Table(
        name = "event_joins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_event",
                        columnNames = {"user_id", "event_id"}
                )
        }
)
public class EventJoin extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventJoinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventJoinStatus status;

    // CREATE
    private EventJoin(User participant, Event event, EventJoinStatus status) {
        validateEventJoin(participant, event, status);
        this.participant = participant;
        this.event = event;
        this.status = status;
    }

    // UPDATE
    public void changeStatus(EventJoinStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("이벤트 신청 - 상태 미입력");
        this.status = newStatus;
    }

    // Check Methods
    private void validateEventJoin(User user, Event event, EventJoinStatus status) {
        if (user == null) {
            throw new IllegalArgumentException("이벤트 신청 - 사용자 미입력");
        }
        if (event == null) {
            throw new IllegalArgumentException("이벤트 신청 - 이벤트 미입력");
        }
        if (status == null) {
            throw new IllegalArgumentException("이벤트 신청 - 상태 미입력");
        }
    }
}