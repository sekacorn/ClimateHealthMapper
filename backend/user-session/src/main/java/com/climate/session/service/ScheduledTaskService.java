package com.climate.session.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled tasks for maintenance and cleanup operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final MfaService mfaService;
    private final SsoService ssoService;

    /**
     * Clean up expired MFA sessions every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredMfaSessions() {
        log.info("Starting cleanup of expired MFA sessions");
        try {
            mfaService.cleanupExpiredSessions();
            log.info("Completed cleanup of expired MFA sessions");
        } catch (Exception e) {
            log.error("Error cleaning up expired MFA sessions", e);
        }
    }

    /**
     * Clean up expired SSO sessions every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredSsoSessions() {
        log.info("Starting cleanup of expired SSO sessions");
        try {
            ssoService.cleanupExpiredSessions();
            log.info("Completed cleanup of expired SSO sessions");
        } catch (Exception e) {
            log.error("Error cleaning up expired SSO sessions", e);
        }
    }

    /**
     * Log session statistics every 6 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void logSessionStatistics() {
        log.info("Session statistics logged");
        // Add logic to collect and log statistics
    }
}
