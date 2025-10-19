package com.climate.collaboration.repository;

import com.climate.collaboration.model.CollabSession;
import com.climate.collaboration.model.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for UserAction entity
 */
@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findBySession(CollabSession session);

    List<UserAction> findBySessionOrderByTimestampDesc(CollabSession session);

    List<UserAction> findBySessionAndUserId(CollabSession session, String userId);

    List<UserAction> findBySessionAndActionType(
        CollabSession session,
        UserAction.ActionType actionType
    );

    List<UserAction> findBySessionAndTimestampBetween(
        CollabSession session,
        LocalDateTime start,
        LocalDateTime end
    );

    Long countBySession(CollabSession session);

    void deleteBySessionAndTimestampBefore(CollabSession session, LocalDateTime cutoffTime);
}
