package com.climate.gateway.config;

import com.climate.gateway.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the API Gateway.
 *
 * <p>This configuration establishes comprehensive security measures including:
 * <ul>
 *   <li>JWT-based authentication and authorization</li>
 *   <li>OAuth2/SSO integration support</li>
 *   <li>CORS configuration for cross-origin requests</li>
 *   <li>CSRF protection (disabled for stateless API)</li>
 *   <li>Security headers and best practices</li>
 *   <li>Public endpoint whitelisting</li>
 * </ul>
 *
 * <p>Security Architecture:
 * The gateway follows a defense-in-depth approach with multiple layers:
 * 1. CORS filtering for cross-origin protection
 * 2. JWT authentication for request validation
 * 3. MFA validation for sensitive operations
 * 4. Role-based access control (RBAC)
 * 5. Rate limiting for abuse prevention
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${oauth2.issuer-uri:}")
    private String oauth2IssuerUri;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    /**
     * Configures the security filter chain.
     *
     * <p>This method sets up the core security infrastructure including:
     * - CORS configuration
     * - CSRF protection (disabled for stateless API)
     * - Public endpoints (no authentication required)
     * - Protected endpoints (authentication required)
     * - OAuth2 resource server configuration
     *
     * @param http the ServerHttpSecurity to configure
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring security filter chain for API Gateway");

        return http
            // Disable CSRF for stateless REST API
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints - no authentication required
                .pathMatchers(HttpMethod.POST, "/api/session/auth/login").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/session/auth/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/session/auth/refresh").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/session/auth/sso/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/session/health").permitAll()
                .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .pathMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()

                // All other endpoints require authentication
                .anyExchange().authenticated()
            )

            // Configure OAuth2 resource server (for SSO)
            .oauth2ResourceServer(oauth2 -> {
                if (oauth2IssuerUri != null && !oauth2IssuerUri.isEmpty()) {
                    oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder()));
                    log.info("OAuth2 resource server configured with issuer: {}", oauth2IssuerUri);
                }
            })

            // Disable default form login (we use JWT)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

            // Disable HTTP basic authentication
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

            // Stateless session management (no session state)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

            // Add custom JWT authentication filter
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

            // Build the security filter chain
            .build();
    }

    /**
     * Configures CORS settings for cross-origin requests.
     *
     * <p>CORS Configuration:
     * - Allowed origins: Configurable via properties (default: localhost:3000, localhost:4200)
     * - Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
     * - Allowed headers: All headers
     * - Exposed headers: Authorization, X-RateLimit-*, etc.
     * - Credentials: Allowed (for cookies/authorization headers)
     * - Max age: 3600 seconds (1 hour)
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS with allowed origins: {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();

        // Parse and set allowed origins
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Parse and set allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Expose specific headers to the client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "X-User-Id",
            "X-Request-Id"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configures the JWT decoder for OAuth2 SSO integration.
     *
     * <p>This decoder is used when the application is configured with an OAuth2
     * issuer URI (e.g., Auth0, Okta, Keycloak). It validates tokens issued by
     * the OAuth2 provider.
     *
     * @return the ReactiveJwtDecoder
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        if (oauth2IssuerUri != null && !oauth2IssuerUri.isEmpty()) {
            log.info("Configuring JWT decoder for OAuth2 issuer: {}", oauth2IssuerUri);
            return NimbusReactiveJwtDecoder.withIssuerLocation(oauth2IssuerUri).build();
        } else {
            // Use symmetric key for internal JWT validation
            log.info("Configuring JWT decoder with symmetric key");
            SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            return NimbusReactiveJwtDecoder.withSecretKey(key).build();
        }
    }
}
