package com.climate.collaboration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity representing user actions in a collaboration session
 */
@Entity
@Table(name = "user_actions", indexes = {
    @Index(name = "idx_session_timestamp", columnList = "session_id,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CollabSession session;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String actionData; // JSON data specific to the action

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String mbtiType;

    @Column
    private Boolean isBroadcast; // Whether this action should be broadcast to others

    public enum ActionType {
        ZOOM,
        PAN,
        FILTER_APPLY,
        FILTER_REMOVE,
        ANNOTATE,
        SHARE_VIEW,
        CURSOR_MOVE,
        HIGHLIGHT,
        COMMENT,
        LAYER_TOGGLE,
        MARKER_ADD,
        MARKER_REMOVE,
        REGION_SELECT,
        DATA_QUERY,
        EXPORT_REQUEST,
        PERMISSION_CHANGE
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (isBroadcast == null) {
            isBroadcast = true;
        }
    }
}
