package com.example.dance_community.controller;

import com.example.dance_community.dto.ApiResponse;
import com.example.dance_community.dto.event.EventCreateRequest;
import com.example.dance_community.dto.event.EventResponse;
import com.example.dance_community.dto.event.EventUpdateRequest;
import com.example.dance_community.dto.like.EventlikeResponse;
import com.example.dance_community.enums.ImageType;
import com.example.dance_community.security.UserDetail;
import com.example.dance_community.service.EventLikeService;
import com.example.dance_community.service.EventService;
import com.example.dance_community.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "6_Event", description = "행사 관련 API")
public class EventController {
    private final EventService eventService;
    private final EventLikeService eventLikeService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "행사 생성", description = "행사를 새로 만듭니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @AuthenticationPrincipal UserDetail userDetail,
            @RequestParam("scope") String scope,
            @RequestParam(value = "clubId", required = false) Long clubId,
            @RequestParam("type") String type,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam("locationName") String locationName,
            @RequestParam(value = "locationAddress", required = false) String locationAddress,
            @RequestParam(value = "locationLink", required = false) String locationLink,
            @RequestParam("capacity") Long capacity,
            @RequestParam("startsAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,
            @RequestParam("endsAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endsAt
    ) {
        List<String> imagePaths = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String path = fileStorageService.saveImage(image, ImageType.EVENT);
                imagePaths.add(path);
            }
        }

        EventCreateRequest eventCreateRequest = EventCreateRequest.builder()
                .scope(scope)
                .clubId(clubId)
                .type(type)
                .title(title)
                .content(content)
                .tags(tags)
                .images(imagePaths)
                .locationName(locationName)
                .locationAddress(locationAddress)
                .locationLink(locationLink)
                .capacity(capacity)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        EventResponse eventResponse = eventService.createEvent(userDetail.getUserId(), eventCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("행사 생성 성공", eventResponse));
    }

    @Operation(summary = "행사 조회", description = "행사 id를 통해 정보를 불러옵니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long eventId
    ) {
        EventResponse eventResponse = eventService.getEvent(eventId, userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("행사 조회 성공", eventResponse));
    }

    @Operation(summary = "전체 행사 조회", description = "전체 행사 정보를 불러옵니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEvents(
            @AuthenticationPrincipal UserDetail userDetail
    ) {
        List<EventResponse> eventResponseList = eventService.getEvents(userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("행사 전체 조회 성공", eventResponseList));
    }

    @Operation(summary = "[Upcoming Event]", description = "현재 시간 이후 시작하는 행사를 불러옵니다.")
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents(
            @AuthenticationPrincipal UserDetail userDetail
    ) {
        List<EventResponse> eventResponseList = eventService.getUpcomingEvents(userDetail.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("Upcoming Event 조회 성공", eventResponseList));
    }

    @Operation(summary = "행사 수정", description = "사용자의 행사 정보를 수정합니다.")
    @PatchMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long eventId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "keepImages", required = false) List<String> keepImages,
            @RequestParam(value = "locationName", required = false) String locationName,
            @RequestParam(value = "locationAddress", required = false) String locationAddress,
            @RequestParam(value = "locationLink", required = false) String locationLink,
            @RequestParam(value = "capacity", required = false) Long capacity,
            @RequestParam(value = "startsAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,
            @RequestParam(value = "endsAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endsAt
    ) {
        List<String> newImagePaths = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String path = fileStorageService.saveImage(image, ImageType.EVENT);
                newImagePaths.add(path);
            }
        }

        EventUpdateRequest eventUpdateRequest = EventUpdateRequest.builder()
                .title(title)
                .content(content)
                .tags(tags)
                .keepImages(keepImages)
                .newImagePaths(newImagePaths)
                .locationName(locationName)
                .locationAddress(locationAddress)
                .locationLink(locationLink)
                .capacity(capacity)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        EventResponse eventResponse = eventService.updateEvent(userDetail.getUserId(), eventId, eventUpdateRequest);
        return ResponseEntity.ok(new ApiResponse<>("행사 수정 성공", eventResponse));
    }

    @Operation(summary = "행사 삭제", description = "행사 id를 통해 정보를 삭제합니다.")
    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long eventId
    ) {
        eventService.deleteEvent(userDetail.getUserId(), eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "행사 좋아요", description = "행사 '좋아요' 버튼을 누릅니다.")
    @PostMapping("/{eventId}/like")
    public ResponseEntity<ApiResponse<EventlikeResponse>> toggleLike(
            @AuthenticationPrincipal UserDetail userDetail,
            @PathVariable Long eventId
    ) {
        EventlikeResponse response = eventLikeService.toggleLike(userDetail.getUserId(), eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("행사 좋아요 성공", response));
    }
}
