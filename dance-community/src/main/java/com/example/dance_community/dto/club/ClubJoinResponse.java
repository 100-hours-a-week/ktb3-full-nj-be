package com.example.dance_community.dto.club;

import com.example.dance_community.entity.ClubJoin;

import java.time.LocalDateTime;

public record ClubJoinResponse (
        Long clubJoinId,
        Long userId,
        String nickname,
        String userEmail,
        String profileImage,
        Long clubId,
        String clubName,
        String role,
        String status,
        LocalDateTime createdAt
){
    public static ClubJoinResponse from(ClubJoin clubJoin) {
        return new ClubJoinResponse(
                clubJoin.getClubJoinId(),
                clubJoin.getUser().getUserId(),
                clubJoin.getUser().getNickname(),
                clubJoin.getUser().getEmail(),
                clubJoin.getUser().getProfileImage(),
                clubJoin.getClub().getClubId(),
                clubJoin.getClub().getClubName(),
                clubJoin.getRole().name(),
                clubJoin.getStatus().name(),
                clubJoin.getCreatedAt()
        );
    }
}