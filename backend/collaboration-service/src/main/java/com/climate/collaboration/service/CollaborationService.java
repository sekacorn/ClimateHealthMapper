package com.climate.collaboration.service;

import com.climate.collaboration.dto.*;
import com.climate.collaboration.model.CollabParticipant;
import com.climate.collaboration.model.CollabSession;
import com.climate.collaboration.model.UserAction;
import com.climate.collaboration.repository.CollabParticipantRepository;
import com.climate.collaboration.repository.CollabSessionRepository;
import com.climate.collaboration.repository.UserActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing collaboration sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

    private final CollabSessionRepository sessionRepository;
    private final CollabParticipantRepository participantRepository;
    private final UserActionRepository actionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String CURSOR_CACHE_PREFIX = "cursor:";
    private static final String PRESENCE_CACHE_PREFIX = "presence:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * Create a new collaboration session
     */
    @Transactional
    public SessionResponse createSession(String sessionName, String userId, String userName,
                                        String mbtiType, Integer maxParticipants, Boolean isPublic) {

        String sessionId = UUID.randomUUID().toString();

        CollabSession session = CollabSession.builder()
            .sessionId(sessionId)
            .sessionName(sessionName)
            .creatorUserId(userId)
            .creatorMbtiType(mbtiType)
            .maxParticipants(maxParticipants != null ? maxParticipants : 10)
            .isPublic(isPublic != null ? isPublic : false)
            .status(CollabSession.SessionStatus.ACTIVE)
            .build();

        session = sessionRepository.save(session);

        // Add creator as owner
        CollabParticipant owner = CollabParticipant.builder()
            .session(session)
            .userId(userId)
            .userName(userName)
            .mbtiType(mbtiType)
            .role(CollabParticipant.ParticipantRole.OWNER)
            .status(CollabParticipant.ParticipantStatus.ACTIVE)
            .color(generateUserColor(0))
            .build();

        participantRepository.save(owner);

        // Cache session
        cacheSession(session);

        log.info("Created collaboration session: {} by user: {}", sessionId, userId);

        return mapToSessionResponse(session);
    }

    /**
     * Join an existing session
     */
    @Transactional
    public JoinSessionResponse joinSession(String sessionId, String userId, String userName, String mbtiType) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        // Check if already a participant
        Optional<CollabParticipant> existingParticipant = participantRepository
            .findBySessionAndUserId(session, userId);

        CollabParticipant participant;

        if (existingParticipant.isPresent()) {
            participant = existingParticipant.get();
            participant.setStatus(CollabParticipant.ParticipantStatus.ACTIVE);
            participant.updateActivity();
        } else {
            // Check max participants
            if (session.getParticipantCount() >= session.getMaxParticipants()) {
                throw new RuntimeException("Session is full");
            }

            participant = CollabParticipant.builder()
                .session(session)
                .userId(userId)
                .userName(userName)
                .mbtiType(mbtiType)
                .role(CollabParticipant.ParticipantRole.PARTICIPANT)
                .status(CollabParticipant.ParticipantStatus.ACTIVE)
                .color(generateUserColor(session.getParticipantCount()))
                .build();
        }

        participant = participantRepository.save(participant);
        session.updateActivity();
        sessionRepository.save(session);

        // Update cache
        cacheSession(session);

        log.info("User {} joined session: {}", userId, sessionId);

        return JoinSessionResponse.builder()
            .sessionId(sessionId)
            .participantId(participant.getId())
            .assignedColor(participant.getColor())
            .role(participant.getRole().name())
            .currentParticipants(session.getParticipantCount())
            .sessionSnapshot(session.getMapSnapshot())
            .sharedFilters(session.getSharedFilters())
            .build();
    }

    /**
     * Leave a session
     */
    @Transactional
    public void leaveSession(String sessionId, String userId) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        CollabParticipant participant = participantRepository
            .findBySessionAndUserId(session, userId)
            .orElseThrow(() -> new RuntimeException("Participant not found"));

        participant.leave();
        participantRepository.save(participant);

        session.updateActivity();
        sessionRepository.save(session);

        // Clear cache for this user
        redisTemplate.delete(CURSOR_CACHE_PREFIX + sessionId + ":" + userId);
        redisTemplate.delete(PRESENCE_CACHE_PREFIX + sessionId + ":" + userId);

        log.info("User {} left session: {}", userId, sessionId);
    }

    /**
     * Get session details
     */
    @Transactional(readOnly = true)
    public SessionDetailsResponse getSessionDetails(String sessionId) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        List<ParticipantInfo> participants = participantRepository
            .findBySessionAndStatus(session, CollabParticipant.ParticipantStatus.ACTIVE)
            .stream()
            .map(this::mapToParticipantInfo)
            .collect(Collectors.toList());

        return SessionDetailsResponse.builder()
            .sessionId(session.getSessionId())
            .sessionName(session.getSessionName())
            .creatorUserId(session.getCreatorUserId())
            .createdAt(session.getCreatedAt())
            .status(session.getStatus().name())
            .participantCount(session.getParticipantCount())
            .maxParticipants(session.getMaxParticipants())
            .isPublic(session.getIsPublic())
            .participants(participants)
            .mapSnapshot(session.getMapSnapshot())
            .sharedFilters(session.getSharedFilters())
            .build();
    }

    /**
     * Get active sessions
     */
    @Transactional(readOnly = true)
    public List<SessionSummary> getActiveSessions(Boolean isPublic) {
        List<CollabSession> sessions;

        if (isPublic != null) {
            sessions = sessionRepository.findByStatusAndIsPublic(
                CollabSession.SessionStatus.ACTIVE, isPublic);
        } else {
            sessions = sessionRepository.findByStatus(CollabSession.SessionStatus.ACTIVE);
        }

        return sessions.stream()
            .map(this::mapToSessionSummary)
            .collect(Collectors.toList());
    }

    /**
     * Get user's sessions
     */
    @Transactional(readOnly = true)
    public List<SessionSummary> getUserSessions(String userId) {
        List<CollabParticipant> participants = participantRepository.findByUserId(userId);

        return participants.stream()
            .map(CollabParticipant::getSession)
            .filter(session -> session.getStatus() == CollabSession.SessionStatus.ACTIVE)
            .distinct()
            .map(this::mapToSessionSummary)
            .collect(Collectors.toList());
    }

    /**
     * Update session settings
     */
    @Transactional
    public SessionResponse updateSessionSettings(String sessionId, UpdateSessionRequest request) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        if (request.getSessionName() != null) {
            session.setSessionName(request.getSessionName());
        }
        if (request.getMaxParticipants() != null) {
            session.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getIsPublic() != null) {
            session.setIsPublic(request.getIsPublic());
        }

        session = sessionRepository.save(session);
        cacheSession(session);

        return mapToSessionResponse(session);
    }

    /**
     * Close a session
     */
    @Transactional
    public void closeSession(String sessionId, String userId) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        // Verify user is owner
        if (!session.getCreatorUserId().equals(userId)) {
            throw new RuntimeException("Only session owner can close the session");
        }

        session.setStatus(CollabSession.SessionStatus.CLOSED);
        sessionRepository.save(session);

        // Clear cache
        redisTemplate.delete(SESSION_CACHE_PREFIX + sessionId);

        log.info("Session {} closed by user: {}", sessionId, userId);
    }

    /**
     * Record user action
     */
    @Transactional
    public void recordAction(String sessionId, WebSocketMessage message) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        UserAction action = UserAction.builder()
            .session(session)
            .userId(message.getUserId())
            .userName(message.getUserName())
            .actionType(mapActionType(message.getAction()))
            .actionData(serializeData(message.getData()))
            .mbtiType(message.getMbtiType())
            .isBroadcast(true)
            .build();

        actionRepository.save(action);

        session.updateActivity();
        sessionRepository.save(session);
    }

    /**
     * Get session history
     */
    @Transactional(readOnly = true)
    public List<ActionHistoryItem> getSessionHistory(String sessionId, int limit) {
        CollabSession session = getSessionByIdOrThrow(sessionId);

        List<UserAction> actions = actionRepository.findBySessionOrderByTimestampDesc(session);

        return actions.stream()
            .limit(limit)
            .map(this::mapToActionHistoryItem)
            .collect(Collectors.toList());
    }

    /**
     * Update session filters
     */
    @Transactional
    public void updateSessionFilters(String sessionId, Object filtersData) {
        CollabSession session = getSessionByIdOrThrow(sessionId);
        session.setSharedFilters(serializeData(filtersData));
        sessionRepository.save(session);
    }

    /**
     * Update map snapshot
     */
    @Transactional
    public void updateMapSnapshot(String sessionId, Object snapshotData) {
        CollabSession session = getSessionByIdOrThrow(sessionId);
        session.setMapSnapshot(serializeData(snapshotData));
        sessionRepository.save(session);
    }

    /**
     * Update cursor position (cached only, not persisted)
     */
    public void updateCursorPosition(String sessionId, String userId, Object position) {
        String key = CURSOR_CACHE_PREFIX + sessionId + ":" + userId;
        redisTemplate.opsForValue().set(key, position, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Update user presence
     */
    public void updateUserPresence(String sessionId, String userId, Object presenceData) {
        String key = PRESENCE_CACHE_PREFIX + sessionId + ":" + userId;
        redisTemplate.opsForValue().set(key, presenceData, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    // Helper methods

    private CollabSession getSessionByIdOrThrow(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    private void cacheSession(CollabSession session) {
        String key = SESSION_CACHE_PREFIX + session.getSessionId();
        redisTemplate.opsForValue().set(key, session, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private String generateUserColor(int index) {
        String[] colors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#98D8C8",
            "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B88B", "#AAB7B8"
        };
        return colors[index % colors.length];
    }

    private UserAction.ActionType mapActionType(String action) {
        try {
            return UserAction.ActionType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserAction.ActionType.SHARE_VIEW;
        }
    }

    private String serializeData(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error serializing data", e);
            return "{}";
        }
    }

    private SessionResponse mapToSessionResponse(CollabSession session) {
        return SessionResponse.builder()
            .sessionId(session.getSessionId())
            .sessionName(session.getSessionName())
            .creatorUserId(session.getCreatorUserId())
            .createdAt(session.getCreatedAt())
            .status(session.getStatus().name())
            .participantCount(session.getParticipantCount())
            .maxParticipants(session.getMaxParticipants())
            .isPublic(session.getIsPublic())
            .build();
    }

    private SessionSummary mapToSessionSummary(CollabSession session) {
        return SessionSummary.builder()
            .sessionId(session.getSessionId())
            .sessionName(session.getSessionName())
            .participantCount(session.getParticipantCount())
            .maxParticipants(session.getMaxParticipants())
            .createdAt(session.getCreatedAt())
            .lastActivityAt(session.getLastActivityAt())
            .isPublic(session.getIsPublic())
            .build();
    }

    private ParticipantInfo mapToParticipantInfo(CollabParticipant participant) {
        return ParticipantInfo.builder()
            .userId(participant.getUserId())
            .userName(participant.getUserName())
            .mbtiType(participant.getMbtiType())
            .role(participant.getRole().name())
            .color(participant.getColor())
            .joinedAt(participant.getJoinedAt())
            .lastActiveAt(participant.getLastActiveAt())
            .build();
    }

    private ActionHistoryItem mapToActionHistoryItem(UserAction action) {
        return ActionHistoryItem.builder()
            .userId(action.getUserId())
            .userName(action.getUserName())
            .actionType(action.getActionType().name())
            .timestamp(action.getTimestamp())
            .mbtiType(action.getMbtiType())
            .build();
    }
}
