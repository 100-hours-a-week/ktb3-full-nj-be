package com.example.dance_community.repository;

import com.example.dance_community.entity.Event;
import com.example.dance_community.repository.custom.EventRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.eventId = :eventId")
    Optional<Event> findWithLockByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE Event e SET e.viewCount = e.viewCount + 1 WHERE e.eventId = :eventId")
    void updateViewCount(@Param("eventId") Long eventId);

    @Modifying()
    @Query("UPDATE Event e SET e.isDeleted = true WHERE e.host.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId);

    @Modifying()
    @Query("UPDATE Event e SET e.isDeleted = true WHERE e.club.clubId = :clubId")
    void softDeleteByClubId(@Param("clubId") Long clubId);
}