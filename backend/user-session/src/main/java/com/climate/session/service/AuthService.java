package com.climate.session.service;

import com.climate.session.dto.*;
import com.climate.session.model.MfaSession;
import com.climate.session.model.Organization;
import com.climate.session.model.User;
import com.climate.session.repository.OrganizationRepository;
import com.climate.session.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Authentication Service handling user registration, login, and password management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final AuditService auditService;

    /**
     * Register new user.
     */
    @Transactional
    public User register(RegisterRequest request) {
        // Validate username and email uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Hash password
        String passwordHash = hashPassword(request.getPassword());

        // Build user
        User.UserBuilder userBuilder = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .isVerified(false)
                .emailVerified(false)
                .mfaEnabled(false)
                .passwordChangedAt(LocalDateTime.now())
                .roles(new HashSet<>(Collections.singletonList(User.Role.USER)));

        // Associate with organization if provided
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));

            if (!organization.canAddUser()) {
                throw new RuntimeException("Organization has reached maximum user limit");
            }

            userBuilder.organization(organization);

            // Set ENTERPRISE role for organization users
            userBuilder.roles(new HashSet<>(List.of(User.Role.USER, User.Role.ENTERPRISE)));
        }

        User user = userBuilder.build();
        user = userRepository.save(user);

        auditService.logUserRegistration(user);
        log.info("User registered: {}", user.getUsername());

        return user;
    }

    /**
     * Authenticate user with username/password.
     */
    @Transactional
    public AuthResponse login(AuthRequest request, String ipAddress, String userAgent) {
        // Find user
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new RuntimeException("Account is locked due to multiple failed login attempts");
        }

        // Verify password
        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            auditService.logFailedLogin(user, ipAddress);
            throw new RuntimeException("Invalid credentials");
        }

        // Reset failed attempts on successful password verification
        user.resetFailedLoginAttempts();

        // Check if MFA is required
        if (user.getMfaEnabled()) {
            MfaSession mfaSession = mfaService.createMfaSession(user, ipAddress, userAgent);
            userRepository.save(user);

            return AuthResponse.builder()
                    .requiresMfa(true)
                    .mfaSessionToken(mfaSession.getSessionToken())
                    .build();
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.logSuccessfulLogin(user, ipAddress);
        log.info("User logged in: {}", user.getUsername());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    /**
     * Verify MFA and complete login.
     */
    @Transactional
    public AuthResponse verifyMfaAndLogin(MfaVerifyRequest request) {
        boolean verified = mfaService.verifyMfaSession(request.getSessionToken(), request.getCode());

        if (!verified) {
            throw new RuntimeException("Invalid MFA code");
        }

        // Get user from session
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.logSuccessfulLogin(user, "");
        log.info("User logged in with MFA: {}", user.getUsername());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    /**
     * Enable MFA for user.
     */
    @Transactional
    public MfaEnableResponse enableMfa(User user, MfaEnableRequest request) {
        // Verify password
        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate MFA secret and backup codes
        String secret = mfaService.generateSecret();
        List<String> backupCodes = mfaService.generateBackupCodes();

        // Generate QR code
        String qrCodeUrl = mfaService.generateQrCodeUrl(user.getUsername(), secret);
        String qrCodeImage = mfaService.generateQrCodeImage(qrCodeUrl);

        // Enable MFA
        mfaService.enableMfa(user, secret, backupCodes);

        auditService.logMfaEnabled(user);
        log.info("MFA enabled for user: {}", user.getUsername());

        return MfaEnableResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeImage)
                .backupCodes(backupCodes)
                .message("MFA enabled successfully. Please save your backup codes in a secure location.")
                .build();
    }

    /**
     * Hash password using BCrypt.
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Verify password against hash.
     */
    public boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    /**
     * Build authentication response.
     */
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .requiresMfa(false)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .roles(user.getRoles())
                        .mfaEnabled(user.getMfaEnabled())
                        .lastLogin(user.getLastLogin())
                        .build())
                .build();
    }

    /**
     * Refresh access token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (!"refresh".equals(jwtService.getTokenType(refreshToken))) {
            throw new RuntimeException("Token is not a refresh token");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(newAccessToken, newRefreshToken, user);
    }

    /**
     * Logout user (invalidate tokens - requires Redis for blacklisting).
     */
    public void logout(String token) {
        // In a production system, you would add the token to a blacklist in Redis
        // For now, we just log the logout
        String username = jwtService.extractUsername(token);
        log.info("User logged out: {}", username);
    }
}
