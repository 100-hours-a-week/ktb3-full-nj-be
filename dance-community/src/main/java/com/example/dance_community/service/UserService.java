package com.example.dance_community.service;

import com.example.dance_community.dto.user.PasswordUpdateRequest;
import com.example.dance_community.dto.user.UserResponse;
import com.example.dance_community.dto.user.UserUpdateRequest;
import com.example.dance_community.entity.User;
import com.example.dance_community.exception.ConflictException;
import com.example.dance_community.exception.NotFoundException;
import com.example.dance_community.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final PostService postService;
    private final ClubJoinService clubJoinService;
    private final EntityManager em;

    @Transactional
    public UserResponse createUser(String email, String password, String nickname, String profileImage) {
        if(this.existsByEmail(email)) {
            throw new ConflictException("이미 사용 중인 이메일입니다");
        }
        if(this.existsByNickname(nickname)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .profileImage(profileImage)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUser(Long userId) {
        return UserResponse.from(findByUserId(userId));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        validateNickname(request.getNickname(), userId);
        User user = this.findByUserId(userId);
        String currentProfileImage = user.getProfileImage();

        if (request.getProfileImage() != null) {
            if (currentProfileImage != null) {
                fileStorageService.deleteFile(currentProfileImage);
            }
        }

        user.updateUser(
                request.getNickname(),
                request.getProfileImage() == null ? user.getProfileImage() : request.getProfileImage()
        );

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = this.findByUserId(userId);

        user.updatePassword(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse deleteProfileImage(Long userId) {
        User user = this.findByUserId(userId);

        if (user.getProfileImage() != null) {
            fileStorageService.deleteFile(user.getProfileImage());
            user.deleteImage();
        }

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findByUserId(userId);

        if (user.getProfileImage() != null) {
            fileStorageService.deleteFile(user.getProfileImage());
        }

        postService.softDeleteByUserId(userId);
        clubJoinService.softDeleteByUserId(userId);
        user.delete();

        em.flush();
        em.clear();
    }

    public boolean matchesPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
    public User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("등록되지 않은 사용자"));
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("등록되지 않은 사용자"));
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
    public void validateNickname(String nickname, Long userId) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return;
        }

        if (userRepository.existsByNicknameAndUserIdNot(nickname, userId)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다");
        }
    }
}