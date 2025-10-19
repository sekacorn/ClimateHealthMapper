package com.climate.session.controller;

import com.climate.session.model.User;
import com.climate.session.repository.UserRepository;
import com.climate.session.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Session Controller for managing user sessions and profiles.
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Get current user session info.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");

            if (!jwtService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            Long userId = jwtService.extractUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("roles", user.getRoles());
            response.put("mfaEnabled", user.getMfaEnabled());
            response.put("isActive", user.getIsActive());
            response.put("emailVerified", user.getEmailVerified());
            response.put("lastLogin", user.getLastLogin());
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Get current user error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Validate token.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            boolean isValid = jwtService.validateToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            if (isValid) {
                response.put("username", jwtService.extractUsername(token));
                response.put("userId", jwtService.extractUserId(token));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token validation error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "user-session");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
