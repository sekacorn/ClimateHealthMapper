package com.climate.collaboration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity representing participants in a collaboration session
 */
@Entity
@Table(name = "collab_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CollabSession session;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String userName;

    @Column
    private String mbtiType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime leftAt;

    @Column
    private LocalDateTime lastActiveAt;

    @Column
    private String cursorPosition; // JSON {x, y, zoom}

    @Column
    private String currentView; // JSON of current map view

    @Column
    private String color; // Assigned color for this user's cursor/annotations

    public enum ParticipantRole {
        OWNER,
        MODERATOR,
        PARTICIPANT,
        VIEWER
    }

    public enum ParticipantStatus {
        ACTIVE,
        IDLE,
        DISCONNECTED
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (lastActiveAt == null) {
            lastActiveAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ParticipantStatus.ACTIVE;
        }
        if (role == null) {
            role = ParticipantRole.PARTICIPANT;
        }
    }

    public void updateActivity() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public void leave() {
        this.status = ParticipantStatus.DISCONNECTED;
        this.leftAt = LocalDateTime.now();
    }
}
