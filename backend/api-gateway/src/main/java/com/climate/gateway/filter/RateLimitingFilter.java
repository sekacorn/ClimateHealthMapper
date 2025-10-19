package com.climate.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Rate Limiting Filter using Redis for distributed rate limiting.
 *
 * <p>This filter implements the Token Bucket algorithm using Redis to enforce
 * rate limits on API requests. It prevents abuse and ensures fair resource
 * allocation across all users of the ClimateHealthMapper platform.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-user and per-IP rate limiting</li>
 *   <li>Distributed rate limiting using Redis</li>
 *   <li>Configurable rate limits and time windows</li>
 *   <li>Rate limit headers in responses</li>
 *   <li>Graceful degradation on Redis failure</li>
 * </ul>
 *
 * <p>Rate Limit Strategy:
 * <ul>
 *   <li>Authenticated users: 1000 requests per hour</li>
 *   <li>Unauthenticated users: 100 requests per hour</li>
 *   <li>Premium users: 5000 requests per hour</li>
 * </ul>
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements GatewayFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // Rate limit constants
    private static final int DEFAULT_RATE_LIMIT = 100; // requests per hour
    private static final int AUTHENTICATED_RATE_LIMIT = 1000; // requests per hour
    private static final int PREMIUM_RATE_LIMIT = 5000; // requests per hour
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    // Redis key prefixes
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * Filters incoming requests and enforces rate limits.
     *
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return a Mono signaling completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Extract rate limit key (user ID or IP address)
        String rateLimitKey = extractRateLimitKey(exchange);
        int rateLimit = determineRateLimit(exchange);

        log.debug("Checking rate limit for key: {}, limit: {}", rateLimitKey, rateLimit);

        return checkRateLimit(rateLimitKey, rateLimit)
            .flatMap(allowed -> {
                if (allowed) {
                    // Add rate limit headers
                    return getRemainingRequests(rateLimitKey, rateLimit)
                        .flatMap(remaining -> {
                            addRateLimitHeaders(exchange, rateLimit, remaining);
                            return chain.filter(exchange);
                        });
                } else {
                    log.warn("Rate limit exceeded for key: {}", rateLimitKey);
                    return onRateLimitExceeded(exchange, rateLimit);
                }
            })
            .onErrorResume(e -> {
                // Fail open: allow request if Redis is unavailable
                log.error("Rate limiting error, allowing request: {}", e.getMessage());
                return chain.filter(exchange);
            });
    }

    /**
     * Extracts the rate limit key from the request.
     * Uses user ID if authenticated, otherwise uses IP address.
     *
     * @param exchange the server exchange
     * @return the rate limit key
     */
    private String extractRateLimitKey(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // Try to get user ID from header (set by JWT filter)
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // Fall back to IP address
        String ipAddress = extractIpAddress(request);
        return "ip:" + ipAddress;
    }

    /**
     * Extracts the client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractIpAddress(ServerHttpRequest request) {
        // Check for proxy headers first
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Determines the rate limit based on user type.
     *
     * @param exchange the server exchange
     * @return the rate limit
     */
    private int determineRateLimit(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // Check if user is authenticated
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            return DEFAULT_RATE_LIMIT;
        }

        // Check for premium user (from roles/permissions)
        String roles = request.getHeaders().getFirst("X-User-Roles");
        if (roles != null && roles.contains("PREMIUM")) {
            return PREMIUM_RATE_LIMIT;
        }

        return AUTHENTICATED_RATE_LIMIT;
    }

    /**
     * Checks if the request is within the rate limit.
     *
     * @param key the rate limit key
     * @param limit the rate limit
     * @return a Mono emitting true if allowed, false otherwise
     */
    private Mono<Boolean> checkRateLimit(String key, int limit) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        long now = Instant.now().getEpochSecond();

        return redisTemplate.opsForValue().get(redisKey)
            .flatMap(value -> {
                int currentCount = Integer.parseInt(value);
                if (currentCount < limit) {
                    // Increment counter
                    return redisTemplate.opsForValue().increment(redisKey)
                        .map(newCount -> true);
                } else {
                    return Mono.just(false);
                }
            })
            .switchIfEmpty(
                // First request, initialize counter
                redisTemplate.opsForValue()
                    .set(redisKey, "1", RATE_LIMIT_WINDOW)
                    .thenReturn(true)
            );
    }

    /**
     * Gets the remaining requests for the rate limit key.
     *
     * @param key the rate limit key
     * @param limit the rate limit
     * @return a Mono emitting the remaining requests
     */
    private Mono<Integer> getRemainingRequests(String key, int limit) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;

        return redisTemplate.opsForValue().get(redisKey)
            .map(value -> {
                int currentCount = Integer.parseInt(value);
                return Math.max(0, limit - currentCount);
            })
            .defaultIfEmpty(limit);
    }

    /**
     * Adds rate limit headers to the response.
     *
     * @param exchange the server exchange
     * @param limit the rate limit
     * @param remaining the remaining requests
     */
    private void addRateLimitHeaders(ServerWebExchange exchange, int limit, int remaining) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
        response.getHeaders().add("X-RateLimit-Reset",
            String.valueOf(Instant.now().plus(RATE_LIMIT_WINDOW).getEpochSecond()));
    }

    /**
     * Handles rate limit exceeded scenario.
     *
     * @param exchange the server exchange
     * @param limit the rate limit
     * @return a Mono signaling completion
     */
    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, int limit) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset",
            String.valueOf(Instant.now().plus(RATE_LIMIT_WINDOW).getEpochSecond()));

        String errorResponse = String.format(
            "{\"error\":\"Rate Limit Exceeded\",\"message\":\"You have exceeded the rate limit of %d requests per hour. Please try again later.\",\"status\":429}",
            limit
        );

        return response.writeWith(Mono.just(
            response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8))
        ));
    }

    @Override
    public int getOrder() {
        return -50; // Execute after authentication but before routing
    }
}
