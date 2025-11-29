package com.example.dance_community.repository;

import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.enums.ClubJoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubJoinRepository extends JpaRepository<ClubJoin, Long> {
    List<ClubJoin> findByClub_ClubIdAndStatus(Long clubId, ClubJoinStatus status);


    List<ClubJoin> findByUser_UserIdAndStatusIn(Long userId, List<ClubJoinStatus> statuses);

    Optional<ClubJoin> findByUser_UserIdAndClub_ClubId(Long userId, Long clubId);
    boolean existsByUser_UserIdAndClub_ClubId(Long userId, Long clubId);

    @Modifying()
    @Query("UPDATE ClubJoin cj SET cj.status = :status WHERE cj.user.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId, @Param("status") ClubJoinStatus status);

    @Modifying()
    @Query("UPDATE ClubJoin cj SET cj.status = :status WHERE cj.club.clubId = :clubId")
    void softDeleteByClubId(@Param("clubId") Long clubId, @Param("status") ClubJoinStatus status);
}