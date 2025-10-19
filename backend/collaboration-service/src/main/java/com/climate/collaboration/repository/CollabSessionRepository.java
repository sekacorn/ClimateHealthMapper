package com.climate.collaboration.repository;

import com.climate.collaboration.model.CollabSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CollabSession entity
 */
@Repository
public interface CollabSessionRepository extends JpaRepository<CollabSession, Long> {

    Optional<CollabSession> findBySessionId(String sessionId);

    List<CollabSession> findByStatus(CollabSession.SessionStatus status);

    List<CollabSession> findByStatusAndIsPublic(CollabSession.SessionStatus status, Boolean isPublic);

    List<CollabSession> findByCreatorUserId(String creatorUserId);

    List<CollabSession> findByStatusAndLastActivityAtBefore(
        CollabSession.SessionStatus status,
        LocalDateTime cutoffTime
    );

    Long countByStatus(CollabSession.SessionStatus status);
}
