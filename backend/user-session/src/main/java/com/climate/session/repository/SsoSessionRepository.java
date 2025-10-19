package com.climate.session.repository;

import com.climate.session.model.SsoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SsoSessionRepository extends JpaRepository<SsoSession, Long> {

    Optional<SsoSession> findByStateToken(String stateToken);

    List<SsoSession> findByUserId(Long userId);

    @Query("SELECT s FROM SsoSession s WHERE s.expiresAt < :now AND s.isCompleted = false")
    List<SsoSession> findExpiredSessions(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SsoSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(LocalDateTime now);
}
