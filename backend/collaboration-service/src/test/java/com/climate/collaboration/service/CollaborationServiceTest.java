package com.climate.collaboration.service;

import com.climate.collaboration.dto.*;
import com.climate.collaboration.model.CollabParticipant;
import com.climate.collaboration.model.CollabSession;
import com.climate.collaboration.model.UserAction;
import com.climate.collaboration.repository.CollabParticipantRepository;
import com.climate.collaboration.repository.CollabSessionRepository;
import com.climate.collaboration.repository.UserActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CollaborationService
 */
@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private CollabSessionRepository sessionRepository;

    @Mock
    private CollabParticipantRepository participantRepository;

    @Mock
    private UserActionRepository actionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CollaborationService collaborationService;

    private CollabSession testSession;
    private CollabParticipant testParticipant;

    @BeforeEach
    void setUp() {
        // Set up test session
        testSession = CollabSession.builder()
            .id(1L)
            .sessionId("test-session-123")
            .sessionName("Test Climate Session")
            .creatorUserId("user-1")
            .creatorMbtiType("ENTJ")
            .status(CollabSession.SessionStatus.ACTIVE)
            .maxParticipants(10)
            .isPublic(true)
            .createdAt(LocalDateTime.now())
            .lastActivityAt(LocalDateTime.now())
            .build();

        // Set up test participant
        testParticipant = CollabParticipant.builder()
            .id(1L)
            .session(testSession)
            .userId("user-1")
            .userName("John Doe")
            .mbtiType("ENTJ")
            .role(CollabParticipant.ParticipantRole.OWNER)
            .status(CollabParticipant.ParticipantStatus.ACTIVE)
            .color("#FF6B6B")
            .joinedAt(LocalDateTime.now())
            .lastActiveAt(LocalDateTime.now())
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testCreateSession_Success() {
        // Arrange
        when(sessionRepository.save(any(CollabSession.class))).thenReturn(testSession);
        when(participantRepository.save(any(CollabParticipant.class))).thenReturn(testParticipant);

        // Act
        SessionResponse response = collaborationService.createSession(
            "Test Climate Session",
            "user-1",
            "John Doe",
            "ENTJ",
            10,
            true
        );

        // Assert
        assertNotNull(response);
        assertEquals("Test Climate Session", response.getSessionName());
        assertEquals("user-1", response.getCreatorUserId());
        assertTrue(response.getIsPublic());
        assertEquals(10, response.getMaxParticipants());

        verify(sessionRepository, times(1)).save(any(CollabSession.class));
        verify(participantRepository, times(1)).save(any(CollabParticipant.class));
    }

    @Test
    void testJoinSession_NewParticipant_Success() {
        // Arrange
        CollabParticipant newParticipant = CollabParticipant.builder()
            .id(2L)
            .session(testSession)
            .userId("user-2")
            .userName("Jane Smith")
            .mbtiType("INFJ")
            .role(CollabParticipant.ParticipantRole.PARTICIPANT)
            .status(CollabParticipant.ParticipantStatus.ACTIVE)
            .color("#4ECDC4")
            .build();

        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(participantRepository.findBySessionAndUserId(testSession, "user-2"))
            .thenReturn(Optional.empty());
        when(participantRepository.save(any(CollabParticipant.class)))
            .thenReturn(newParticipant);
        when(sessionRepository.save(any(CollabSession.class)))
            .thenReturn(testSession);

        // Act
        JoinSessionResponse response = collaborationService.joinSession(
            "test-session-123",
            "user-2",
            "Jane Smith",
            "INFJ"
        );

        // Assert
        assertNotNull(response);
        assertEquals("test-session-123", response.getSessionId());
        assertEquals(2L, response.getParticipantId());
        assertEquals("#4ECDC4", response.getAssignedColor());

        verify(participantRepository, times(1)).save(any(CollabParticipant.class));
        verify(sessionRepository, times(1)).save(any(CollabSession.class));
    }

    @Test
    void testJoinSession_ExistingParticipant_Success() {
        // Arrange
        testParticipant.setStatus(CollabParticipant.ParticipantStatus.DISCONNECTED);

        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(participantRepository.findBySessionAndUserId(testSession, "user-1"))
            .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(CollabParticipant.class)))
            .thenReturn(testParticipant);
        when(sessionRepository.save(any(CollabSession.class)))
            .thenReturn(testSession);

        // Act
        JoinSessionResponse response = collaborationService.joinSession(
            "test-session-123",
            "user-1",
            "John Doe",
            "ENTJ"
        );

        // Assert
        assertNotNull(response);
        assertEquals(CollabParticipant.ParticipantStatus.ACTIVE, testParticipant.getStatus());
    }

    @Test
    void testJoinSession_SessionNotFound_ThrowsException() {
        // Arrange
        when(sessionRepository.findBySessionId("invalid-session"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            collaborationService.joinSession(
                "invalid-session",
                "user-2",
                "Jane Smith",
                "INFJ"
            )
        );
    }

    @Test
    void testLeaveSession_Success() {
        // Arrange
        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(participantRepository.findBySessionAndUserId(testSession, "user-1"))
            .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(CollabParticipant.class)))
            .thenReturn(testParticipant);
        when(sessionRepository.save(any(CollabSession.class)))
            .thenReturn(testSession);

        // Act
        collaborationService.leaveSession("test-session-123", "user-1");

        // Assert
        assertEquals(CollabParticipant.ParticipantStatus.DISCONNECTED, testParticipant.getStatus());
        assertNotNull(testParticipant.getLeftAt());
        verify(participantRepository, times(1)).save(testParticipant);
    }

    @Test
    void testGetSessionDetails_Success() {
        // Arrange
        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(participantRepository.findBySessionAndStatus(
            testSession,
            CollabParticipant.ParticipantStatus.ACTIVE
        )).thenReturn(Arrays.asList(testParticipant));

        // Act
        SessionDetailsResponse response = collaborationService.getSessionDetails("test-session-123");

        // Assert
        assertNotNull(response);
        assertEquals("test-session-123", response.getSessionId());
        assertEquals("Test Climate Session", response.getSessionName());
        assertEquals(1, response.getParticipants().size());
        assertEquals("John Doe", response.getParticipants().get(0).getUserName());
    }

    @Test
    void testGetActiveSessions_Success() {
        // Arrange
        List<CollabSession> sessions = Arrays.asList(testSession);
        when(sessionRepository.findByStatusAndIsPublic(
            CollabSession.SessionStatus.ACTIVE,
            true
        )).thenReturn(sessions);

        // Act
        List<SessionSummary> result = collaborationService.getActiveSessions(true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Climate Session", result.get(0).getSessionName());
    }

    @Test
    void testCloseSession_Success() {
        // Arrange
        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(CollabSession.class)))
            .thenReturn(testSession);

        // Act
        collaborationService.closeSession("test-session-123", "user-1");

        // Assert
        assertEquals(CollabSession.SessionStatus.CLOSED, testSession.getStatus());
        verify(sessionRepository, times(1)).save(testSession);
    }

    @Test
    void testCloseSession_NotOwner_ThrowsException() {
        // Arrange
        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            collaborationService.closeSession("test-session-123", "user-2")
        );
    }

    @Test
    void testRecordAction_Success() {
        // Arrange
        WebSocketMessage message = WebSocketMessage.builder()
            .action("ZOOM")
            .userId("user-1")
            .userName("John Doe")
            .mbtiType("ENTJ")
            .data(Map.of("level", 10))
            .build();

        UserAction action = UserAction.builder()
            .id(1L)
            .session(testSession)
            .userId("user-1")
            .userName("John Doe")
            .actionType(UserAction.ActionType.ZOOM)
            .mbtiType("ENTJ")
            .build();

        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(actionRepository.save(any(UserAction.class)))
            .thenReturn(action);
        when(sessionRepository.save(any(CollabSession.class)))
            .thenReturn(testSession);

        // Act
        collaborationService.recordAction("test-session-123", message);

        // Assert
        verify(actionRepository, times(1)).save(any(UserAction.class));
        verify(sessionRepository, times(1)).save(testSession);
    }

    @Test
    void testUpdateSessionSettings_Success() {
        // Arrange
        UpdateSessionRequest request = UpdateSessionRequest.builder()
            .sessionName("Updated Session Name")
            .maxParticipants(20)
            .isPublic(false)
            .build();

        when(sessionRepository.findBySessionId("test-session-123"))
            .thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(CollabSession.class)))
            .thenReturn(testSession);

        // Act
        SessionResponse response = collaborationService.updateSessionSettings(
            "test-session-123",
            request
        );

        // Assert
        assertEquals("Updated Session Name", testSession.getSessionName());
        assertEquals(20, testSession.getMaxParticipants());
        assertFalse(testSession.getIsPublic());
        verify(sessionRepository, times(1)).save(testSession);
    }

    @Test
    void testUpdateCursorPosition_Success() {
        // Arrange
        Object position = Map.of("x", 100, "y", 200);

        // Act
        collaborationService.updateCursorPosition("test-session-123", "user-1", position);

        // Assert
        verify(valueOperations, times(1)).set(
            anyString(),
            eq(position),
            anyLong(),
            any()
        );
    }

    @Test
    void testGetUserSessions_Success() {
        // Arrange
        when(participantRepository.findByUserId("user-1"))
            .thenReturn(Arrays.asList(testParticipant));

        // Act
        List<SessionSummary> result = collaborationService.getUserSessions("user-1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Climate Session", result.get(0).getSessionName());
    }
}
