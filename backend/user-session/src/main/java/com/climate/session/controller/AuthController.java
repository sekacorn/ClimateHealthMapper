package com.climate.session.controller;

import com.climate.session.dto.*;
import com.climate.session.model.SsoSession;
import com.climate.session.model.User;
import com.climate.session.service.AuthService;
import com.climate.session.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller handling user registration, login, SSO, and MFA.
 */
@RestController
@RequestMapping("/api/session/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final SsoService ssoService;

    /**
     * Register new user.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Registration error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Login with username/password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            AuthResponse response = authService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Initiate SSO login.
     */
    @GetMapping("/login/sso/{provider}")
    public ResponseEntity<Map<String, String>> initiateSsoLogin(
            @PathVariable String provider,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            SsoSession session = ssoService.createSsoSession(provider, ipAddress, userAgent);
            String authUrl = ssoService.generateAuthorizationUrl(provider, session);

            Map<String, String> response = new HashMap<>();
            response.put("authorizationUrl", authUrl);
            response.put("state", session.getStateToken());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("SSO initiation error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * SSO callback endpoint.
     */
    @GetMapping("/login/sso/{provider}/callback")
    public ResponseEntity<AuthResponse> handleSsoCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        try {
            User user = ssoService.handleSsoCallback(provider, code, state);

            // Generate tokens for SSO user
            String accessToken = authService.hashPassword(""); // This should be JWT generation
            String refreshToken = authService.hashPassword(""); // This should be JWT generation

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("SSO callback error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Enable MFA for authenticated user.
     */
    @PostMapping("/mfa/enable")
    public ResponseEntity<MfaEnableResponse> enableMfa(
            @Valid @RequestBody MfaEnableRequest request,
            @AuthenticationPrincipal User user) {
        try {
            MfaEnableResponse response = authService.enableMfa(user, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MFA enable error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Verify MFA code and complete login.
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request) {
        try {
            AuthResponse response = authService.verifyMfaAndLogin(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MFA verification error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Logout user.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            authService.logout(token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Logout error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Refresh access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Get client IP address.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
