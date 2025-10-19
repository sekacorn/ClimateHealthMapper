package com.climate.session.service;

import com.climate.session.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Audit Service for logging security events and user actions.
 */
@Service
@Slf4j
public class AuditService {

    public void logUserRegistration(User user) {
        log.info("AUDIT: User registered - Username: {}, Email: {}, ID: {}",
                user.getUsername(), user.getEmail(), user.getId());
    }

    public void logSuccessfulLogin(User user, String ipAddress) {
        log.info("AUDIT: Successful login - Username: {}, IP: {}, Timestamp: {}",
                user.getUsername(), ipAddress, java.time.LocalDateTime.now());
    }

    public void logFailedLogin(User user, String ipAddress) {
        log.warn("AUDIT: Failed login attempt - Username: {}, IP: {}, Attempts: {}, Timestamp: {}",
                user.getUsername(), ipAddress, user.getFailedLoginAttempts(), java.time.LocalDateTime.now());
    }

    public void logMfaEnabled(User user) {
        log.info("AUDIT: MFA enabled - Username: {}, ID: {}", user.getUsername(), user.getId());
    }

    public void logMfaDisabled(User user) {
        log.info("AUDIT: MFA disabled - Username: {}, ID: {}", user.getUsername(), user.getId());
    }

    public void logPasswordChange(User user) {
        log.info("AUDIT: Password changed - Username: {}, ID: {}", user.getUsername(), user.getId());
    }

    public void logAccountLocked(User user) {
        log.warn("AUDIT: Account locked - Username: {}, ID: {}, Locked until: {}",
                user.getUsername(), user.getId(), user.getAccountLockedUntil());
    }

    public void logAccountUnlocked(User user) {
        log.info("AUDIT: Account unlocked - Username: {}, ID: {}", user.getUsername(), user.getId());
    }

    public void logRoleChanged(User user, String oldRoles, String newRoles) {
        log.info("AUDIT: Role changed - Username: {}, ID: {}, Old: {}, New: {}",
                user.getUsername(), user.getId(), oldRoles, newRoles);
    }

    public void logSsoLogin(User user, String provider, String ipAddress) {
        log.info("AUDIT: SSO login - Username: {}, Provider: {}, IP: {}, Timestamp: {}",
                user.getUsername(), provider, ipAddress, java.time.LocalDateTime.now());
    }
}
