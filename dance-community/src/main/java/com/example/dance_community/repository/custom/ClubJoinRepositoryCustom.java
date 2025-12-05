package com.example.dance_community.repository.custom;

import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.enums.ClubJoinStatus;
import java.util.List;

public interface ClubJoinRepositoryCustom {
    // 내 동아리 목록 조회
    List<ClubJoin> findMyClubJoins(Long userId, List<ClubJoinStatus> statuses);

    // 클럽 멤버/신청자 목록 조회
    List<ClubJoin> findClubMembers(Long clubId, ClubJoinStatus status);
}