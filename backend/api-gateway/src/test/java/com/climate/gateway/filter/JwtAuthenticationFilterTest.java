package com.climate.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;
    private static final String TEST_SECRET = "TestSecretKey-ForUnitTests-DoNotUseInProduction-2025-MustBeLongEnough";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(filter, "requireMfa", false);

        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testPublicEndpointAllowedWithoutToken() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/session/auth/login")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void testProtectedEndpointWithoutToken() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/integrator/data")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void testProtectedEndpointWithValidToken() {
        // Given
        String token = generateToken("user123", false);
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/integrator/data")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void testProtectedEndpointWithExpiredToken() {
        // Given
        String token = generateExpiredToken("user123");
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/integrator/data")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void testProtectedEndpointWithInvalidToken() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/integrator/data")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void testMfaValidationRequired() {
        // Given
        ReflectionTestUtils.setField(filter, "requireMfa", true);
        String token = generateToken("user123", false); // MFA not validated
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/integrator/data")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void testMfaValidationSuccess() {
        // Given
        ReflectionTestUtils.setField(filter, "requireMfa", true);
        String token = generateToken("user123", true); // MFA validated
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/integrator/data")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    /**
     * Generates a valid JWT token for testing.
     */
    private String generateToken(String userId, boolean mfaValidated) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .subject(userId)
            .claim("email", userId + "@test.com")
            .claim("roles", "USER")
            .claim("permissions", "READ")
            .claim("mfa_validated", mfaValidated)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(key)
            .compact();
    }

    /**
     * Generates an expired JWT token for testing.
     */
    private String generateExpiredToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .subject(userId)
            .claim("email", userId + "@test.com")
            .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
            .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
            .signWith(key)
            .compact();
    }
}
