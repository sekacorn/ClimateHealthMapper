package com.climate.collaboration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a collaboration session
 */
@Entity
@Table(name = "collab_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private String sessionName;

    @Column(nullable = false)
    private String creatorUserId;

    @Column(nullable = false)
    private String creatorMbtiType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastActivityAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @Column
    private String mapSnapshot; // JSON snapshot of current map state

    @Column
    private String sharedFilters; // JSON of active filters

    @Column
    private Integer maxParticipants;

    @Column
    private Boolean isPublic;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CollabParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAction> actions = new ArrayList<>();

    public enum SessionStatus {
        ACTIVE,
        INACTIVE,
        CLOSED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
        if (isPublic == null) {
            isPublic = false;
        }
        if (maxParticipants == null) {
            maxParticipants = 10;
        }
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public int getParticipantCount() {
        return participants != null ? (int) participants.stream()
                .filter(p -> p.getStatus() == CollabParticipant.ParticipantStatus.ACTIVE)
                .count() : 0;
    }
}
