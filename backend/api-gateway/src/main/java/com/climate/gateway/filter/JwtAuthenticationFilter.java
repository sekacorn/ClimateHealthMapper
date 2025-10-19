package com.climate.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

/**
 * JWT Authentication Filter for validating JWT tokens in incoming requests.
 *
 * <p>This filter intercepts all incoming requests (except whitelisted endpoints)
 * and validates the JWT token present in the Authorization header. It extracts
 * user information from the token and adds it to the request headers for
 * downstream services.
 *
 * <p>Security Features:
 * <ul>
 *   <li>JWT signature validation</li>
 *   <li>Token expiration checking</li>
 *   <li>MFA status validation</li>
 *   <li>User role and permission extraction</li>
 *   <li>Request context enrichment</li>
 * </ul>
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.require-mfa:true}")
    private boolean requireMfa;

    /**
     * List of endpoints that do not require authentication.
     */
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/session/auth/login",
        "/api/session/auth/register",
        "/api/session/auth/refresh",
        "/api/session/auth/sso",
        "/api/session/health",
        "/actuator/health",
        "/actuator/prometheus"
    );

    /**
     * Filters incoming requests and validates JWT tokens.
     *
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return a Mono signaling completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(request.getPath().value())) {
            log.debug("Public endpoint accessed: {}", request.getPath());
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String token = extractToken(request);

        if (token == null) {
            log.warn("Missing authentication token for endpoint: {}", request.getPath());
            return onError(exchange, "Missing authentication token", HttpStatus.UNAUTHORIZED);
        }

        try {
            // Validate and parse JWT token
            Claims claims = validateToken(token);

            // Validate MFA if required
            if (requireMfa && !isMfaValidated(claims)) {
                log.warn("MFA validation required but not present for user: {}", claims.getSubject());
                return onError(exchange, "MFA validation required", HttpStatus.FORBIDDEN);
            }

            // Enrich request with user context
            ServerHttpRequest enrichedRequest = enrichRequest(request, claims);

            log.debug("Successfully authenticated user: {} for endpoint: {}",
                claims.getSubject(), request.getPath());

            return chain.filter(exchange.mutate().request(enrichedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return onError(exchange, "Invalid token format", HttpStatus.UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return onError(exchange, "Malformed token", HttpStatus.UNAUTHORIZED);
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return onError(exchange, "Invalid token signature", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage(), e);
            return onError(exchange, "Authentication failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Checks if the given path is a public endpoint.
     *
     * @param path the request path
     * @return true if the path is public, false otherwise
     */
    private boolean isPublicEndpoint(String path) {
        Predicate<String> pathMatcher = publicPath ->
            path.equals(publicPath) || path.startsWith(publicPath + "/");
        return PUBLIC_ENDPOINTS.stream().anyMatch(pathMatcher);
    }

    /**
     * Extracts JWT token from the Authorization header.
     *
     * @param request the HTTP request
     * @return the JWT token or null if not present
     */
    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get("Authorization");

        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.get(0);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /**
     * Validates the JWT token and extracts claims.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     * @throws Exception if token validation fails
     */
    private Claims validateToken(String token) throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Checks if MFA has been validated for the user.
     *
     * @param claims the JWT claims
     * @return true if MFA is validated, false otherwise
     */
    private boolean isMfaValidated(Claims claims) {
        Boolean mfaValidated = claims.get("mfa_validated", Boolean.class);
        return mfaValidated != null && mfaValidated;
    }

    /**
     * Enriches the request with user context from JWT claims.
     *
     * @param request the original request
     * @param claims the JWT claims
     * @return the enriched request
     */
    private ServerHttpRequest enrichRequest(ServerHttpRequest request, Claims claims) {
        return request.mutate()
            .header("X-User-Id", claims.getSubject())
            .header("X-User-Email", claims.get("email", String.class))
            .header("X-User-Roles", claims.get("roles", String.class))
            .header("X-User-Permissions", claims.get("permissions", String.class))
            .header("X-MFA-Validated", String.valueOf(isMfaValidated(claims)))
            .build();
    }

    /**
     * Handles authentication errors.
     *
     * @param exchange the server exchange
     * @param message the error message
     * @param httpStatus the HTTP status code
     * @return a Mono signaling completion
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format(
            "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
            httpStatus.getReasonPhrase(),
            message,
            httpStatus.value()
        );

        return response.writeWith(Mono.just(
            response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8))
        ));
    }

    @Override
    public int getOrder() {
        return -100; // Execute before other filters
    }
}
