package com.climate.gateway.exception;

/**
 * Custom exception for business logic errors.
 *
 * <p>This exception is used to represent errors that are part of normal
 * business logic and should not trigger circuit breakers or retries.
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public class BusinessException extends RuntimeException {

    /**
     * Constructs a new business exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Constructs a new business exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
