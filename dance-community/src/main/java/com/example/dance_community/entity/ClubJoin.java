package com.example.dance_community.entity;

import com.example.dance_community.enums.ClubJoinStatus;
import com.example.dance_community.enums.ClubRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "club_joins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_club",
                        columnNames = {"user_id", "club_id"}
                )
        }
)
public class ClubJoin extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clubJoinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClubJoinStatus status;

    // CREATE
    @Builder
    private ClubJoin(User user, Club club, ClubRole role, ClubJoinStatus status) {
        validateClubJoin(user, club, role, status);
        this.user = user;
        this.club = club;
        this.role = role;
        this.status = status;
    }

    // READ
    public boolean hasManagementPermission() {
        return this.role == ClubRole.LEADER || this.role == ClubRole.MANAGER;
    }

    // UPDATE
    public void changeRole(ClubRole newRole) {
        if (newRole == null) throw new IllegalArgumentException("클럽 가입 - 역할 미입력");
        this.role = newRole;
    }
    public void changeStatus(ClubJoinStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("클럽 가입 - 상태 미입력");
        this.status = newStatus;
    }

    // Check Methods
    private void validateClubJoin(User user, Club club, ClubRole role, ClubJoinStatus status) {
        if (user == null) {
            throw new IllegalArgumentException("클럽 가입 - 사용자 미입력");
        }
        if (club == null) {
            throw new IllegalArgumentException("클럽 가입 - 클럽 미입력");
        }
        if (role == null) {
            throw new IllegalArgumentException("클럽 가입 - 역할 미입력");
        }
        if (status == null) {
            throw new IllegalArgumentException("클럽 가입 - 상태 미입력");
        }
    }
}