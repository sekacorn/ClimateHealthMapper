package com.climate.gateway.config;

import com.climate.gateway.filter.RateLimitingFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Gateway routing configuration for ClimateHealthMapper microservices.
 *
 * <p>This configuration defines the routing rules for the API Gateway, directing
 * incoming requests to the appropriate backend microservices. It implements
 * advanced patterns for resilience, load balancing, and fault tolerance.
 *
 * <p>Microservices Architecture:
 * <ul>
 *   <li>Climate Integrator Service (8081): Data integration and external APIs</li>
 *   <li>Climate Visualizer Service (8082): Data visualization and analytics</li>
 *   <li>User Session Service (8083): Authentication, authorization, and sessions</li>
 *   <li>LLM Service (8084): AI/ML features and natural language processing</li>
 *   <li>Collaboration Service (8085): Real-time collaboration features</li>
 * </ul>
 *
 * <p>Resilience Patterns:
 * <ul>
 *   <li>Circuit Breaker: Prevents cascading failures</li>
 *   <li>Retry Logic: Automatic retry for transient failures</li>
 *   <li>Timeout Management: Prevents indefinite waiting</li>
 *   <li>Fallback Mechanisms: Graceful degradation</li>
 * </ul>
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final RateLimitingFilter rateLimitingFilter;

    @Value("${services.integrator.url:http://climate-integrator:8081}")
    private String integratorServiceUrl;

    @Value("${services.visualizer.url:http://climate-visualizer:8082}")
    private String visualizerServiceUrl;

    @Value("${services.session.url:http://user-session:8083}")
    private String sessionServiceUrl;

    @Value("${services.llm.url:http://llm-service:8084}")
    private String llmServiceUrl;

    @Value("${services.collaboration.url:http://collaboration-service:8085}")
    private String collaborationServiceUrl;

    /**
     * Configures the gateway routes for all microservices.
     *
     * <p>Each route includes:
     * - Path predicate for request matching
     * - URI for the target service
     * - Circuit breaker for resilience
     * - Rate limiting filter
     * - Path rewriting if needed
     *
     * @param builder the route locator builder
     * @return the configured route locator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Configuring gateway routes for ClimateHealthMapper microservices");

        return builder.routes()
            // Climate Integrator Service Routes
            .route("integrator-service", r -> r
                .path("/api/integrator/**")
                .filters(f -> f
                    .stripPrefix(1) // Remove /api prefix
                    .filter(rateLimitingFilter)
                    .circuitBreaker(config -> config
                        .setName("integratorCircuitBreaker")
                        .setFallbackUri("forward:/fallback/integrator")
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                    )
                )
                .uri(integratorServiceUrl)
            )

            // Climate Visualizer Service Routes
            .route("visualizer-service", r -> r
                .path("/api/visualizer/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .filter(rateLimitingFilter)
                    .circuitBreaker(config -> config
                        .setName("visualizerCircuitBreaker")
                        .setFallbackUri("forward:/fallback/visualizer")
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                    )
                )
                .uri(visualizerServiceUrl)
            )

            // User Session Service Routes
            .route("session-service", r -> r
                .path("/api/session/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .filter(rateLimitingFilter)
                    .circuitBreaker(config -> config
                        .setName("sessionCircuitBreaker")
                        .setFallbackUri("forward:/fallback/session")
                    )
                    // No retry for session operations (to prevent duplicate logins)
                )
                .uri(sessionServiceUrl)
            )

            // LLM Service Routes
            .route("llm-service", r -> r
                .path("/api/llm/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .filter(rateLimitingFilter)
                    .circuitBreaker(config -> config
                        .setName("llmCircuitBreaker")
                        .setFallbackUri("forward:/fallback/llm")
                    )
                    .retry(config -> config
                        .setRetries(2)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(200), Duration.ofMillis(2000), 2, true)
                    )
                )
                .uri(llmServiceUrl)
            )

            // Collaboration Service Routes
            .route("collaboration-service", r -> r
                .path("/api/collab/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .filter(rateLimitingFilter)
                    .circuitBreaker(config -> config
                        .setName("collaborationCircuitBreaker")
                        .setFallbackUri("forward:/fallback/collaboration")
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                    )
                )
                .uri(collaborationServiceUrl)
            )

            .build();
    }

    /**
     * Configures the circuit breaker with custom settings.
     *
     * <p>Circuit Breaker Configuration:
     * - Failure Rate Threshold: 50% (open circuit if >50% requests fail)
     * - Slow Call Rate Threshold: 50% (open if >50% calls are slow)
     * - Slow Call Duration: 2 seconds
     * - Wait Duration in Open State: 10 seconds
     * - Sliding Window Size: 10 calls
     * - Minimum Number of Calls: 5
     * - Permitted Calls in Half-Open State: 3
     *
     * @return the circuit breaker customizer
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        log.info("Configuring default circuit breaker settings");

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                // Open circuit if failure rate exceeds 50%
                .failureRateThreshold(50)
                // Open circuit if slow call rate exceeds 50%
                .slowCallRateThreshold(50)
                // Consider calls slower than 2 seconds as slow
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // Wait 10 seconds before transitioning from open to half-open
                .waitDurationInOpenState(Duration.ofSeconds(10))
                // Allow 3 calls in half-open state to test recovery
                .permittedNumberOfCallsInHalfOpenState(3)
                // Use a sliding window of 10 calls
                .slidingWindowSize(10)
                // Minimum 5 calls before calculating failure rate
                .minimumNumberOfCalls(5)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                // Overall timeout for requests
                .timeoutDuration(Duration.ofSeconds(5))
                .build())
            .build());
    }

    /**
     * Configures the key resolver for rate limiting.
     *
     * <p>This resolver determines the key used for rate limiting. It uses the
     * user ID if available (for authenticated users), otherwise falls back to
     * the IP address.
     *
     * @return the key resolver
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from header (set by JWT filter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }

            // Fall back to IP address
            String ipAddress = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

            return Mono.just(ipAddress);
        };
    }

    /**
     * Configures Redis-based rate limiter.
     *
     * <p>Rate Limiter Configuration:
     * - Replenish Rate: Number of tokens added per second
     * - Burst Capacity: Maximum number of tokens in bucket
     * - Requested Tokens: Tokens consumed per request
     *
     * @return the Redis rate limiter
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        log.info("Configuring Redis-based rate limiter");

        // Default: 100 requests per second, burst of 200
        return new RedisRateLimiter(100, 200, 1);
    }
}
