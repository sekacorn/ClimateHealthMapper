package com.climate.collaboration.controller;

import com.climate.collaboration.dto.WebSocketMessage;
import com.climate.collaboration.service.CollaborationService;
import com.climate.collaboration.service.MbtiCollaborationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket Controller for real-time collaboration
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CollaborationService collaborationService;
    private final MbtiCollaborationService mbtiCollaborationService;
    private final ObjectMapper objectMapper;

    /**
     * Handle zoom actions
     */
    @MessageMapping("/collab/{sessionId}/zoom")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleZoom(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        log.info("Zoom action in session: {} by user: {}", sessionId, message.getUserId());

        // Record action
        collaborationService.recordAction(sessionId, message);

        // Apply MBTI-specific handling
        message = mbtiCollaborationService.enhanceMessage(message);

        // Broadcast to all participants
        return message;
    }

    /**
     * Handle pan actions
     */
    @MessageMapping("/collab/{sessionId}/pan")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handlePan(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Pan action in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle filter actions
     */
    @MessageMapping("/collab/{sessionId}/filter")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleFilter(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Filter action in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        collaborationService.updateSessionFilters(sessionId, message.getData());
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle annotation actions
     */
    @MessageMapping("/collab/{sessionId}/annotate")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleAnnotate(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Annotate action in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle share view actions
     */
    @MessageMapping("/collab/{sessionId}/share")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleShare(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Share view action in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        collaborationService.updateMapSnapshot(sessionId, message.getData());
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle cursor movement
     */
    @MessageMapping("/collab/{sessionId}/cursor")
    public void handleCursor(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        // Update cursor position without persisting (high frequency)
        collaborationService.updateCursorPosition(sessionId, message.getUserId(), message.getData());

        // Broadcast to others (not sender)
        messagingTemplate.convertAndSend(
            "/topic/collab/" + sessionId + "/cursors",
            message
        );
    }

    /**
     * Handle comments
     */
    @MessageMapping("/collab/{sessionId}/comment")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleComment(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Comment in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle highlight actions
     */
    @MessageMapping("/collab/{sessionId}/highlight")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleHighlight(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Highlight action in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle layer toggle
     */
    @MessageMapping("/collab/{sessionId}/layer")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleLayer(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Layer toggle in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle marker add/remove
     */
    @MessageMapping("/collab/{sessionId}/marker")
    @SendTo("/topic/collab/{sessionId}")
    public WebSocketMessage handleMarker(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        log.info("Marker action in session: {} by user: {}", sessionId, message.getUserId());

        collaborationService.recordAction(sessionId, message);
        message = mbtiCollaborationService.enhanceMessage(message);

        return message;
    }

    /**
     * Handle user presence updates
     */
    @MessageMapping("/collab/{sessionId}/presence")
    public void handlePresence(
            @DestinationVariable String sessionId,
            @Payload WebSocketMessage message) {

        collaborationService.updateUserPresence(sessionId, message.getUserId(), message.getData());

        // Broadcast presence update
        messagingTemplate.convertAndSend(
            "/topic/collab/" + sessionId + "/presence",
            message
        );
    }

    /**
     * Send MBTI-specific notifications
     */
    public void sendMbtiNotification(String sessionId, String userId, Map<String, Object> notification) {
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/collab/" + sessionId + "/mbti",
            notification
        );
    }

    /**
     * Broadcast system message to all participants
     */
    public void broadcastSystemMessage(String sessionId, String messageType, Object data) {
        WebSocketMessage message = WebSocketMessage.builder()
            .action("SYSTEM")
            .messageType(messageType)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();

        messagingTemplate.convertAndSend("/topic/collab/" + sessionId, message);
    }
}
