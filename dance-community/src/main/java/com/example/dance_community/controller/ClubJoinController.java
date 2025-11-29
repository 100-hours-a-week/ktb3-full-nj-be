package com.example.dance_community.controller;

import com.example.dance_community.dto.ApiResponse;
import com.example.dance_community.dto.club.ClubJoinResponse;
import com.example.dance_community.enums.ClubRole;
import com.example.dance_community.security.UserDetail;
import com.example.dance_community.service.ClubJoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
@Tag(name = "4_ClubJoin", description = "클럽 가입 관련 API")
public class ClubJoinController {
    private final ClubJoinService clubJoinService;

    // 일반 사용자용
    @Operation(summary = "클럽 가입 신청", description = "클럽에 가입 신청합니다.")
    @PostMapping("/{clubId}/apply")
    public ResponseEntity<ApiResponse<ClubJoinResponse>> applyToClub(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId
    ) {
        ClubJoinResponse response = clubJoinService.applyToClub(userDetail.getUserId(), clubId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("클럽 가입 신청 성공", response));
    }

    @Operation(summary = "클럽 가입 신청 취소", description = "클럽 가입 신청을 취소합니다.")
    @DeleteMapping("/{clubId}/apply")
    public ResponseEntity<ApiResponse<Void>> cancelApplication(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId
    ) {
        clubJoinService.cancelApplication(userDetail.getUserId(), clubId);
        return ResponseEntity.ok(new ApiResponse<>("클럽 가입 신청 취소 성공", null));
    }

    @Operation(summary = "클럽 탈퇴", description = "클럽에서 탈퇴합니다.")
    @DeleteMapping("/{clubId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveClub(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId
    ) {
        clubJoinService.leaveClub(userDetail.getUserId(), clubId);
        return ResponseEntity.ok(new ApiResponse<>("클럽 탈퇴 성공", null));
    }

    @Operation(summary = "내 가입 상태 조회", description = "내 클럽 가입 상태를 조회합니다.")
    @GetMapping("/{clubId}/my-status")
    public ResponseEntity<ApiResponse<ClubJoinResponse>> getMyJoinStatus(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId
    ) {
        ClubJoinResponse response = clubJoinService.getJoinStatus(userDetail.getUserId(), clubId);
        return ResponseEntity.ok(new ApiResponse<>("내 가입 상태 조회 성공", response));
    }

    @Operation(summary = "내 클럽 목록 조회", description = "내가 가입한 클럽 목록을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClubJoinResponse>>> getMyClubs(
            @AuthenticationPrincipal UserDetail userDetail
    ) {
        List<ClubJoinResponse> myClubs = clubJoinService.getMyClubs(userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("내 클럽 목록 조회 성공", myClubs));
    }

    // 클럽 관리자용
    @Operation(summary = "대기 중인 신청 목록 조회", description = "대기 중인 신청 목록을 조회합니다.")
    @GetMapping("/{clubId}/applications")
    public ResponseEntity<ApiResponse<List<ClubJoinResponse>>> getPendingApplications(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId
    ) {
        List<ClubJoinResponse> responses = clubJoinService.getPendingApplications(userDetail.getUserId(), clubId);
        return ResponseEntity.ok(new ApiResponse<>("대기 중인 신청 목록 조회 성공", responses));
    }

    @Operation(summary = "가입 신청 승인", description = "가입 신청을 승인합니다.")
    @PostMapping("/{clubId}/applications/{applicantId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveApplication(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId,
            @PathVariable Long applicantId
    ) {
        clubJoinService.approveApplication(userDetail.getUserId(), clubId, applicantId);
        return ResponseEntity.ok(new ApiResponse<>("가입 신청 승인 성공", null));
    }

    @Operation(summary = "가입 신청 거절", description = "가입 신청을 거절합니다.")
    @PostMapping("/{clubId}/applications/{applicantId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApplication(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId,
            @PathVariable Long applicantId
    ) {
        clubJoinService.rejectApplication(userDetail.getUserId(), clubId, applicantId);
        return ResponseEntity.ok(new ApiResponse<>("가입 신청 거절 성공", null));
    }

    @Operation(summary = "멤버 역할 변경", description = "멤버의 역할을 변경합니다.")
    @PatchMapping("/{clubId}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<Void>> changeMemberRole(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId,
            @PathVariable Long memberId,
            @RequestParam String newRole
    ) {
        clubJoinService.changeMemberRole(userDetail.getUserId(), clubId, memberId, ClubRole.valueOf(newRole));
        return ResponseEntity.ok(new ApiResponse<>("멤버 역할 변경 성공", null));
    }

    @Operation(summary = "멤버 추방", description = "멤버를 추방합니다.")
    @DeleteMapping("/{clubId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long clubId,
            @PathVariable Long memberId
    ) {
        clubJoinService.kickMember(userDetail.getUserId(), clubId, memberId);
        return ResponseEntity.ok(new ApiResponse<>("멤버 추방 성공", null));
    }

    // 조회용
    @Operation(summary = "활동 중인 멤버 목록 조회", description = "활동 중인 멤버 목록을 조회합니다.")
    @GetMapping("/{clubId}/members")
    public ResponseEntity<ApiResponse<List<ClubJoinResponse>>> getActiveMembers(
            @PathVariable Long clubId
    ) {
        List<ClubJoinResponse> responses = clubJoinService.getActiveMembers(clubId);
        return ResponseEntity.ok(new ApiResponse<>("조회 성공", responses));
    }
}