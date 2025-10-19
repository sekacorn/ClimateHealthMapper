package com.climate.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for rate limiting and caching.
 *
 * <p>This configuration establishes the Redis connection and provides
 * templates for reactive Redis operations used in rate limiting and caching.
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates a reactive Redis connection factory.
     *
     * @return the reactive Redis connection factory
     */
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{}", redisHost, redisPort);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
            log.debug("Redis password configured");
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Creates a reactive Redis template for String operations.
     *
     * @param factory the reactive Redis connection factory
     * @return the reactive Redis template
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
        ReactiveRedisConnectionFactory factory) {

        log.info("Configuring reactive Redis template");

        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
            .<String, String>newSerializationContext()
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}
