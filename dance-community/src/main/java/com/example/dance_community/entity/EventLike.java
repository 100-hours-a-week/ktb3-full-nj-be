package com.example.dance_community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "event_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "user_id"})
        }
)
public class EventLike extends BaseLike {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder
    public EventLike(Event event, User user) {
        super(user);
        this.event = event;
    }
}
