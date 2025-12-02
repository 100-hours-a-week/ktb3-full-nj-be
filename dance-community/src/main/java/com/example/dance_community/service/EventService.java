package com.example.dance_community.service;

import com.example.dance_community.dto.event.EventCreateRequest;
import com.example.dance_community.dto.event.EventResponse;
import com.example.dance_community.dto.event.EventUpdateRequest;
import com.example.dance_community.entity.Club;
import com.example.dance_community.entity.Event;
import com.example.dance_community.entity.User;
import com.example.dance_community.enums.EventJoinStatus;
import com.example.dance_community.enums.EventType;
import com.example.dance_community.enums.Scope;
import com.example.dance_community.exception.InvalidRequestException;
import com.example.dance_community.exception.NotFoundException;
import com.example.dance_community.repository.EventJoinRepository;
import com.example.dance_community.repository.EventLikeRepository;
import com.example.dance_community.repository.EventRepository;
import com.example.dance_community.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventJoinRepository eventJoinRepository;
    private final EventLikeRepository eventLikeRepository;
    private final ClubAuthService clubAuthService;
    private final FileStorageService fileStorageService;
    private final EntityManager em;

    @Transactional
    public EventResponse createEvent(Long userId, EventCreateRequest request) {
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        Club club = null;
        if (Scope.CLUB.toString().equals(request.getScope())) {
            if (request.getClubId() == null) throw new InvalidRequestException("클럽 ID 필요");
            club = clubAuthService.findByClubId(request.getClubId());
        }

        Event event = Event.builder()
                .host(host)
                .scope(Scope.valueOf(request.getScope().toUpperCase()))
                .club(club)
                .type(EventType.valueOf(request.getType().toUpperCase()))
                .title(request.getTitle())
                .content(request.getContent())
                .tags(request.getTags())
                .images(request.getImages())
                .locationName(request.getLocationName())
                .locationAddress(request.getLocationAddress())
                .locationLink(request.getLocationLink())
                .capacity(request.getCapacity())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();

        return EventResponse.from(eventRepository.save(event), false);
    }

    @Transactional
    public EventResponse getEvent(Long eventId, Long userId) {
        Event event = getActiveEvent(eventId);

        eventRepository.updateViewCount(eventId);
        boolean isLiked = userId != null && eventLikeRepository.existsByEventEventIdAndUserUserId(eventId, userId);
        return EventResponse.from(getActiveEvent(eventId), isLiked);
    }
    public List<EventResponse> getEvents(Long userId) {
        return eventRepository.findAll().stream()
                .map(event -> {
                            boolean isLiked = userId != null
                                    && eventLikeRepository.existsByEventEventIdAndUserUserId(event.getEventId(), userId);
                            return EventResponse.from(event, isLiked);
                        }).toList();
    }
    public List<EventResponse> getUpcomingEvents(Long userId) {
        Pageable limitEight = PageRequest.of(0, 10);

        return eventRepository.findUpcomingEvents(limitEight).stream()
                .map(event -> {
                    boolean isLiked = userId != null
                            && eventLikeRepository.existsByEventEventIdAndUserUserId(event.getEventId(), userId);
                    return EventResponse.from(event, isLiked);
                }).toList();
    }

    @Transactional
    public EventResponse updateEvent(Long userId, Long eventId, EventUpdateRequest request) {
        Event event = getActiveEvent(eventId);
        checkHost(userId, event);

        event.updateEvent(
                request.getTitle(), request.getContent(), request.getTags(),
                request.getLocationName(), request.getLocationAddress(),
                request.getLocationLink(), request.getCapacity(),
                request.getStartsAt(), request.getEndsAt()
        );

        handleImageUpdate(event, request.getNewImagePaths(), request.getKeepImages());
        boolean isLiked = eventLikeRepository.existsByEventEventIdAndUserUserId(eventId, userId);

        return EventResponse.from(event, isLiked);
    }

    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        Event event = getActiveEvent(eventId);
        checkHost(userId, event);

        if (event.getImages() != null && !event.getImages().isEmpty()) {
            for (String imagePath : event.getImages()) {
                fileStorageService.deleteFile(imagePath);
            }
        }

        eventJoinRepository.softDeleteByEventId(eventId, EventJoinStatus.CANCELED);
        event.delete();

        em.flush();
        em.clear();
    }

    public Event getActiveEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("행사 조회 실패"));
    }
    private void checkHost(Long userId, Event event) {
        if (!event.getHost().getUserId().equals(userId)) {
            throw new InvalidRequestException("권한이 없습니다");
        }
    }
    private void handleImageUpdate(Event event, List<String> newImages, List<String> keepImages) {
        if (keepImages == null) {
            if (newImages != null && !newImages.isEmpty()) {
                List<String> currentImages = new ArrayList<>(event.getImages());
                currentImages.addAll(newImages);
                event.updateImages(currentImages);
            }
            return;
        }

        List<String> currentImages = event.getImages();
        List<String> finalImages = new ArrayList<>();

        if (keepImages.isEmpty()) {
            for (String imagePath : currentImages) {
                fileStorageService.deleteFile(imagePath);
            }
        } else {
            finalImages.addAll(keepImages);

            List<String> imagesToDelete = currentImages.stream()
                    .filter(img -> !keepImages.contains(img))
                    .collect(Collectors.toList());

            for (String imagePath : imagesToDelete) {
                fileStorageService.deleteFile(imagePath);
            }
        }

        if (newImages != null && !newImages.isEmpty()) {
            finalImages.addAll(newImages);
        }

        event.updateImages(finalImages);
    }

    public void softDeleteByUserId(Long userId) {
        eventRepository.softDeleteByUserId(userId);
    }
    public void softDeleteByClubId(Long clubId) { eventRepository.softDeleteByClubId(clubId);}
}
