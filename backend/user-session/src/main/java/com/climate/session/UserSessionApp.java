package com.climate.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Climate Health Mapper User Session Service.
 * Provides authentication, SSO integration, and MFA capabilities.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class UserSessionApp {

    public static void main(String[] args) {
        SpringApplication.run(UserSessionApp.class, args);
    }
}
