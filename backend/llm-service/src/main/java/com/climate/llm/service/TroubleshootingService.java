package com.climate.llm.service;

import com.climate.llm.dto.TroubleshootRequestDto;
import com.climate.llm.dto.TroubleshootResponseDto;
import com.climate.llm.model.ErrorLog;
import com.climate.llm.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for analyzing errors and providing troubleshooting solutions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TroubleshootingService {

    private final ErrorLogRepository errorLogRepository;

    /**
     * Analyze error and provide troubleshooting suggestions
     */
    public TroubleshootResponseDto analyze(TroubleshootRequestDto request) {
        log.info("Analyzing error: {} from service: {}",
            request.getErrorType(), request.getServiceName());

        // Save error log
        ErrorLog errorLog = saveErrorLog(request);

        // Generate troubleshooting suggestions
        List<String> suggestions = generateSuggestions(request);

        // Determine severity
        String severity = determineSeverity(request);

        // Check for similar past errors
        List<ErrorLog> similarErrors = findSimilarErrors(request);
        boolean hasRecurringIssue = similarErrors.size() > 3;

        return TroubleshootResponseDto.builder()
            .success(true)
            .errorId(errorLog.getId())
            .severity(severity)
            .suggestions(suggestions)
            .relatedDocs(getRelatedDocumentation(request.getErrorType()))
            .estimatedResolutionTime(estimateResolutionTime(severity))
            .isRecurring(hasRecurringIssue)
            .similarErrorsCount(similarErrors.size())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Save error log to database
     */
    private ErrorLog saveErrorLog(TroubleshootRequestDto request) {
        ErrorLog errorLog = ErrorLog.builder()
            .errorType(request.getErrorType())
            .serviceName(request.getServiceName())
            .severity(determineSeverity(request))
            .errorMessage(request.getErrorMessage())
            .stackTrace(request.getStackTrace())
            .userId(request.getUserId())
            .sessionId(request.getSessionId())
            .resolved(false)
            .metadata(request.getMetadata())
            .build();

        return errorLogRepository.save(errorLog);
    }

    /**
     * Generate troubleshooting suggestions based on error type
     */
    private List<String> generateSuggestions(TroubleshootRequestDto request) {
        List<String> suggestions = new ArrayList<>();
        String errorType = request.getErrorType().toLowerCase();
        String errorMessage = request.getErrorMessage().toLowerCase();

        // Database errors
        if (errorType.contains("database") || errorType.contains("sql")) {
            suggestions.add("Check database connection settings in application.yml");
            suggestions.add("Verify PostgreSQL service is running");
            suggestions.add("Ensure database credentials are correct");
            suggestions.add("Check if database schema is up to date");
        }

        // API errors
        if (errorType.contains("api") || errorType.contains("http")) {
            suggestions.add("Verify API endpoint URL is correct");
            suggestions.add("Check API authentication credentials");
            suggestions.add("Ensure network connectivity to external services");
            suggestions.add("Review API rate limits and quotas");
        }

        // Redis errors
        if (errorType.contains("redis") || errorType.contains("cache")) {
            suggestions.add("Check Redis server status");
            suggestions.add("Verify Redis connection configuration");
            suggestions.add("Ensure Redis has sufficient memory");
            suggestions.add("Check Redis key expiration settings");
        }

        // LLM provider errors
        if (errorType.contains("llm") || errorMessage.contains("huggingface") ||
            errorMessage.contains("openai") || errorMessage.contains("xai")) {
            suggestions.add("Verify LLM API key is valid and active");
            suggestions.add("Check LLM provider service status");
            suggestions.add("Review rate limits for LLM API");
            suggestions.add("Try switching to alternative LLM provider");
            suggestions.add("Ensure model name is correct in configuration");
        }

        // Authentication errors
        if (errorType.contains("auth") || errorMessage.contains("unauthorized")) {
            suggestions.add("Verify authentication token is valid");
            suggestions.add("Check token expiration time");
            suggestions.add("Ensure user has required permissions");
            suggestions.add("Review authentication service logs");
        }

        // Timeout errors
        if (errorMessage.contains("timeout") || errorMessage.contains("timed out")) {
            suggestions.add("Increase timeout settings in configuration");
            suggestions.add("Check network latency to external services");
            suggestions.add("Optimize query performance");
            suggestions.add("Consider implementing retry logic");
        }

        // Memory errors
        if (errorMessage.contains("memory") || errorMessage.contains("heap")) {
            suggestions.add("Increase JVM heap size (-Xmx parameter)");
            suggestions.add("Check for memory leaks in application");
            suggestions.add("Review resource cleanup in code");
            suggestions.add("Monitor application memory usage");
        }

        // Generic suggestions if no specific match
        if (suggestions.isEmpty()) {
            suggestions.add("Review application logs for detailed error information");
            suggestions.add("Check service configuration in application.yml");
            suggestions.add("Verify all required dependencies are available");
            suggestions.add("Restart the service to clear transient issues");
            suggestions.add("Contact system administrator if issue persists");
        }

        return suggestions;
    }

    /**
     * Determine error severity
     */
    private String determineSeverity(TroubleshootRequestDto request) {
        String errorType = request.getErrorType().toLowerCase();
        String errorMessage = request.getErrorMessage().toLowerCase();

        // Critical errors
        if (errorMessage.contains("fatal") || errorMessage.contains("critical") ||
            errorType.contains("database") && errorMessage.contains("connection")) {
            return "CRITICAL";
        }

        // Errors
        if (errorType.contains("error") || errorMessage.contains("failed") ||
            errorMessage.contains("exception")) {
            return "ERROR";
        }

        // Warnings
        return "WARNING";
    }

    /**
     * Find similar past errors
     */
    private List<ErrorLog> findSimilarErrors(TroubleshootRequestDto request) {
        return errorLogRepository.findByErrorTypeAndServiceName(
            request.getErrorType(),
            request.getServiceName()
        );
    }

    /**
     * Get related documentation links
     */
    private List<String> getRelatedDocumentation(String errorType) {
        List<String> docs = new ArrayList<>();

        String type = errorType.toLowerCase();

        if (type.contains("database")) {
            docs.add("https://docs.spring.io/spring-boot/docs/current/reference/html/data.html");
            docs.add("https://www.postgresql.org/docs/");
        }

        if (type.contains("redis")) {
            docs.add("https://redis.io/documentation");
            docs.add("https://docs.spring.io/spring-data/redis/docs/current/reference/html/");
        }

        if (type.contains("llm") || type.contains("api")) {
            docs.add("https://huggingface.co/docs/api-inference/");
            docs.add("https://platform.openai.com/docs/");
            docs.add("https://docs.x.ai/");
        }

        // Always include general Spring Boot docs
        docs.add("https://docs.spring.io/spring-boot/docs/current/reference/html/");

        return docs;
    }

    /**
     * Estimate resolution time based on severity
     */
    private String estimateResolutionTime(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "15-30 minutes";
            case "ERROR" -> "30-60 minutes";
            case "WARNING" -> "1-2 hours";
            default -> "Variable";
        };
    }
}
