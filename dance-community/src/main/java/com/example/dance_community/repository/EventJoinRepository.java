package com.example.dance_community.repository;

import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.entity.Event;
import com.example.dance_community.entity.EventJoin;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.ClubJoinStatus;
import com.example.dance_community.enums.EventJoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventJoinRepository extends JpaRepository<EventJoin, Long> , EventJoinRepositoryCustom {
    Optional<EventJoin> findByParticipant_UserIdAndEvent_EventId(Long userId, Long eventId);

    long countByEvent_EventIdAndStatus(Long eventId, EventJoinStatus status);

    @Modifying()
    @Query("UPDATE EventJoin cj SET cj.status = :status WHERE cj.participant.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId, @Param("status") EventJoinStatus status);

    @Modifying
    @Query("update EventJoin ej set ej.status = :status where ej.event.club.clubId = :clubId")
    void softDeleteByClubId(@Param("clubId") Long clubId, @Param("status") EventJoinStatus status);

    @Modifying
    @Query("update EventJoin ej set ej.status = :status where ej.event.eventId = :eventId")
    void softDeleteByEventId(@Param("eventId") Long eventId, @Param("status") EventJoinStatus status);
}