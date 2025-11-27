package com.example.dance_community.service;

import com.example.dance_community.entity.Club;
import com.example.dance_community.entity.ClubJoin;
import com.example.dance_community.enums.ClubRole;
import com.example.dance_community.exception.AuthException;
import com.example.dance_community.exception.NotFoundException;
import com.example.dance_community.repository.ClubJoinRepository;
import com.example.dance_community.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAuthService {
    private final ClubRepository clubRepository;
    private final ClubJoinRepository clubJoinRepository;

    public void validateClubAuthority(Long userId, Long clubId) {
        ClubJoin clubJoin = clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId);

        if (clubJoin == null) {
            throw new AuthException("클럽 멤버가 아닙니다");
        }

        if (!clubJoin.hasManagementPermission()) {
            throw new AuthException("클럽 권한이 없습니다");
        }
    }

    public void validateLeaderAuthority(Long userId, Long clubId) {
        ClubJoin clubJoin = clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId);

        if (clubJoin == null || clubJoin.getRole() != ClubRole.LEADER) {
            throw new AuthException("클럽 리더만 가능합니다");
        }
    }

    public Club findByClubId(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("클럽을 찾을 수 없습니다"));
    }

    public ClubJoin findClubJoin(Long userId, Long clubId) {
        ClubJoin clubJoin = clubJoinRepository.findByUser_UserIdAndClub_ClubId(userId, clubId);
        if (clubJoin == null) {
            throw new NotFoundException("클럽 가입 정보를 찾을 수 없습니다");
        }
        return clubJoin;
    }
}