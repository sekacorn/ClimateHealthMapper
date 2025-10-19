package com.climate.collaboration.controller;

import com.climate.collaboration.dto.*;
import com.climate.collaboration.model.CollabSession;
import com.climate.collaboration.service.CollaborationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for collaboration session management
 */
@RestController
@RequestMapping("/api/collab")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class CollaborationController {

    private final CollaborationService collaborationService;

    /**
     * Create a new collaboration session
     */
    @PostMapping("/session/create")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        log.info("Creating new collaboration session for user: {}", request.getUserId());

        SessionResponse session = collaborationService.createSession(
            request.getSessionName(),
            request.getUserId(),
            request.getUserName(),
            request.getMbtiType(),
            request.getMaxParticipants(),
            request.getIsPublic()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Join an existing collaboration session
     */
    @PostMapping("/session/{sessionId}/join")
    public ResponseEntity<JoinSessionResponse> joinSession(
            @PathVariable String sessionId,
            @Valid @RequestBody JoinSessionRequest request) {

        log.info("User {} joining session: {}", request.getUserId(), sessionId);

        JoinSessionResponse response = collaborationService.joinSession(
            sessionId,
            request.getUserId(),
            request.getUserName(),
            request.getMbtiType()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Leave a collaboration session
     */
    @PostMapping("/session/{sessionId}/leave")
    public ResponseEntity<Void> leaveSession(
            @PathVariable String sessionId,
            @RequestParam String userId) {

        log.info("User {} leaving session: {}", userId, sessionId);
        collaborationService.leaveSession(sessionId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get session details
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionDetailsResponse> getSession(@PathVariable String sessionId) {
        log.info("Fetching session details for: {}", sessionId);

        SessionDetailsResponse session = collaborationService.getSessionDetails(sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * Get all active sessions
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<SessionSummary>> getActiveSessions(
            @RequestParam(required = false) Boolean isPublic) {

        log.info("Fetching active sessions (public: {})", isPublic);

        List<SessionSummary> sessions = collaborationService.getActiveSessions(isPublic);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get user's sessions
     */
    @GetMapping("/sessions/user/{userId}")
    public ResponseEntity<List<SessionSummary>> getUserSessions(@PathVariable String userId) {
        log.info("Fetching sessions for user: {}", userId);

        List<SessionSummary> sessions = collaborationService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Update session settings
     */
    @PutMapping("/session/{sessionId}/settings")
    public ResponseEntity<SessionResponse> updateSessionSettings(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateSessionRequest request) {

        log.info("Updating settings for session: {}", sessionId);

        SessionResponse session = collaborationService.updateSessionSettings(sessionId, request);
        return ResponseEntity.ok(session);
    }

    /**
     * Close a session
     */
    @PostMapping("/session/{sessionId}/close")
    public ResponseEntity<Void> closeSession(
            @PathVariable String sessionId,
            @RequestParam String userId) {

        log.info("Closing session: {} by user: {}", sessionId, userId);
        collaborationService.closeSession(sessionId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get session history/actions
     */
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<List<ActionHistoryItem>> getSessionHistory(
            @PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        log.info("Fetching history for session: {} (limit: {})", sessionId, limit);

        List<ActionHistoryItem> history = collaborationService.getSessionHistory(sessionId, limit);
        return ResponseEntity.ok(history);
    }
}
