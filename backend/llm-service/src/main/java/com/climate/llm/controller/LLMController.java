package com.climate.llm.controller;

import com.climate.llm.dto.*;
import com.climate.llm.model.LLMResponse;
import com.climate.llm.model.QueryContext;
import com.climate.llm.service.LLMService;
import com.climate.llm.service.TroubleshootingService;
import com.climate.llm.repository.QueryContextRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for LLM operations with MBTI-tailored responses
 */
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LLMController {

    private final LLMService llmService;
    private final TroubleshootingService troubleshootingService;
    private final QueryContextRepository queryContextRepository;

    /**
     * Process natural language query with MBTI context
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponseDto> processQuery(@Valid @RequestBody QueryRequestDto request) {
        log.info("Processing query for user: {}, MBTI: {}", request.getUserId(), request.getMbtiType());

        try {
            QueryResponseDto response = llmService.processQuery(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(QueryResponseDto.builder()
                    .success(false)
                    .error("Failed to process query: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Troubleshoot system errors and provide solutions
     */
    @PostMapping("/troubleshoot")
    public ResponseEntity<TroubleshootResponseDto> troubleshoot(
            @Valid @RequestBody TroubleshootRequestDto request) {
        log.info("Troubleshooting error: {} from service: {}",
            request.getErrorType(), request.getServiceName());

        try {
            TroubleshootResponseDto response = troubleshootingService.analyze(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during troubleshooting: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TroubleshootResponseDto.builder()
                    .success(false)
                    .error("Troubleshooting failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get query history for a user
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<QueryHistoryDto>> getHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching query history for user: {}", userId);

        try {
            PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

            List<QueryContext> contexts = queryContextRepository
                .findByUserId(userId, pageRequest);

            List<QueryHistoryDto> history = contexts.stream()
                .map(this::convertToHistoryDto)
                .toList();

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "llm-service");
        health.put("timestamp", System.currentTimeMillis());

        // Check LLM provider connectivity
        Map<String, String> providers = new HashMap<>();
        providers.put("huggingface", llmService.checkProviderHealth("huggingface"));
        providers.put("openai", llmService.checkProviderHealth("openai"));
        providers.put("xai", llmService.checkProviderHealth("xai"));

        health.put("providers", providers);

        return ResponseEntity.ok(health);
    }

    /**
     * Get MBTI personality insights
     */
    @GetMapping("/mbti/{type}/insights")
    public ResponseEntity<MbtiInsightsDto> getMbtiInsights(@PathVariable String type) {
        log.info("Fetching MBTI insights for type: {}", type);

        try {
            MbtiInsightsDto insights = llmService.getMbtiInsights(type.toUpperCase());
            return ResponseEntity.ok(insights);
        } catch (IllegalArgumentException e) {
            log.error("Invalid MBTI type: {}", type);
            return ResponseEntity.badRequest().build();
        }
    }

    private QueryHistoryDto convertToHistoryDto(QueryContext context) {
        return QueryHistoryDto.builder()
            .id(context.getId())
            .queryText(context.getQueryText())
            .mbtiType(context.getMbtiType())
            .location(context.getLocation())
            .createdAt(context.getCreatedAt())
            .build();
    }
}
