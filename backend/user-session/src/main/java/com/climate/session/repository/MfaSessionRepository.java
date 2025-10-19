package com.climate.session.repository;

import com.climate.session.model.MfaSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MfaSessionRepository extends JpaRepository<MfaSession, Long> {

    Optional<MfaSession> findBySessionToken(String sessionToken);

    List<MfaSession> findByUserId(Long userId);

    @Query("SELECT m FROM MfaSession m WHERE m.expiresAt < :now AND m.isVerified = false")
    List<MfaSession> findExpiredSessions(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM MfaSession m WHERE m.expiresAt < :now")
    void deleteExpiredSessions(LocalDateTime now);
}
