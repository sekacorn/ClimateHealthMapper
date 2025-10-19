package com.climate.collaboration.repository;

import com.climate.collaboration.model.CollabParticipant;
import com.climate.collaboration.model.CollabSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CollabParticipant entity
 */
@Repository
public interface CollabParticipantRepository extends JpaRepository<CollabParticipant, Long> {

    Optional<CollabParticipant> findBySessionAndUserId(CollabSession session, String userId);

    List<CollabParticipant> findBySession(CollabSession session);

    List<CollabParticipant> findBySessionAndStatus(
        CollabSession session,
        CollabParticipant.ParticipantStatus status
    );

    List<CollabParticipant> findByUserId(String userId);

    List<CollabParticipant> findByUserIdAndStatus(
        String userId,
        CollabParticipant.ParticipantStatus status
    );

    Long countBySessionAndStatus(
        CollabSession session,
        CollabParticipant.ParticipantStatus status
    );

    boolean existsBySessionAndUserId(CollabSession session, String userId);
}
