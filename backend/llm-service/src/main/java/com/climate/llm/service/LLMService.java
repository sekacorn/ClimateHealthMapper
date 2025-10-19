package com.climate.llm.service;

import com.climate.llm.dto.*;
import com.climate.llm.model.LLMResponse;
import com.climate.llm.model.QueryContext;
import com.climate.llm.repository.QueryContextRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for LLM integration with multiple providers
 * Supports Hugging Face, OpenAI-compatible APIs, and xAI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    private final MbtiPromptService mbtiPromptService;
    private final QueryContextRepository queryContextRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    private final ObjectMapper objectMapper;

    @Value("${llm.provider:huggingface}")
    private String defaultProvider;

    @Value("${llm.huggingface.api-key:}")
    private String huggingfaceApiKey;

    @Value("${llm.huggingface.model:mistralai/Mistral-7B-Instruct-v0.2}")
    private String huggingfaceModel;

    @Value("${llm.openai.api-key:}")
    private String openaiApiKey;

    @Value("${llm.openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Value("${llm.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${llm.xai.api-key:}")
    private String xaiApiKey;

    @Value("${llm.xai.model:grok-beta}")
    private String xaiModel;

    @Value("${llm.rate-limit.requests-per-minute:60}")
    private int rateLimitPerMinute;

    /**
     * Process user query with MBTI-tailored response
     */
    public QueryResponseDto processQuery(QueryRequestDto request) {
        long startTime = System.currentTimeMillis();

        // Check rate limit
        if (!checkRateLimit(request.getUserId())) {
            return QueryResponseDto.builder()
                .success(false)
                .error("Rate limit exceeded. Please try again later.")
                .build();
        }

        // Save query context
        QueryContext context = saveQueryContext(request);

        // Check cache
        String cacheKey = generateCacheKey(request);
        QueryResponseDto cachedResponse = getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            log.info("Returning cached response for query");
            cachedResponse.setCached(true);
            return cachedResponse;
        }

        // Generate MBTI-tailored prompt
        String enhancedPrompt = mbtiPromptService.generatePrompt(
            request.getQueryText(),
            request.getMbtiType(),
            request.getLocation()
        );

        // Call LLM provider
        String provider = request.getPreferredProvider() != null
            ? request.getPreferredProvider()
            : defaultProvider;

        try {
            String responseText = callLLMProvider(provider, enhancedPrompt);

            long processingTime = System.currentTimeMillis() - startTime;

            QueryResponseDto response = QueryResponseDto.builder()
                .success(true)
                .queryId(context.getId())
                .responseText(responseText)
                .mbtiType(request.getMbtiType())
                .provider(provider)
                .processingTimeMs(processingTime)
                .cached(false)
                .timestamp(LocalDateTime.now())
                .build();

            // Cache the response
            cacheResponse(cacheKey, response);

            return response;
        } catch (Exception e) {
            log.error("Error calling LLM provider {}: {}", provider, e.getMessage(), e);
            return QueryResponseDto.builder()
                .success(false)
                .error("Failed to generate response: " + e.getMessage())
                .build();
        }
    }

    /**
     * Call LLM provider based on configuration
     */
    private String callLLMProvider(String provider, String prompt) throws IOException {
        return switch (provider.toLowerCase()) {
            case "huggingface" -> callHuggingFace(prompt);
            case "openai" -> callOpenAI(prompt);
            case "xai" -> callXAI(prompt);
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }

    /**
     * Call Hugging Face Transformers API
     */
    private String callHuggingFace(String prompt) throws IOException {
        String url = "https://api-inference.huggingface.co/models/" + huggingfaceModel;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", prompt);
        requestBody.put("parameters", Map.of(
            "max_new_tokens", 500,
            "temperature", 0.7,
            "top_p", 0.9,
            "return_full_text", false
        ));

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(requestBody),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + huggingfaceApiKey)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Hugging Face API error: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.isArray() && jsonNode.size() > 0) {
                return jsonNode.get(0).get("generated_text").asText();
            }

            throw new IOException("Unexpected response format from Hugging Face");
        }
    }

    /**
     * Call OpenAI-compatible API
     */
    private String callOpenAI(String prompt) throws IOException {
        String url = openaiBaseUrl + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openaiModel);
        requestBody.put("messages", new Object[]{
            Map.of("role", "system", "content", "You are a helpful climate health assistant."),
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(requestBody),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + openaiApiKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API error: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            return jsonNode.get("choices").get(0)
                .get("message").get("content").asText();
        }
    }

    /**
     * Call xAI API
     */
    private String callXAI(String prompt) throws IOException {
        String url = "https://api.x.ai/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", xaiModel);
        requestBody.put("messages", new Object[]{
            Map.of("role", "system", "content", "You are Grok, a helpful climate health assistant."),
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(requestBody),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + xaiApiKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("xAI API error: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            return jsonNode.get("choices").get(0)
                .get("message").get("content").asText();
        }
    }

    /**
     * Save query context to database
     */
    private QueryContext saveQueryContext(QueryRequestDto request) {
        QueryContext context = QueryContext.builder()
            .userId(request.getUserId())
            .mbtiType(request.getMbtiType())
            .queryText(request.getQueryText())
            .location(request.getLocation())
            .sessionId(request.getSessionId())
            .build();

        return queryContextRepository.save(context);
    }

    /**
     * Check rate limiting
     */
    private boolean checkRateLimit(String userId) {
        String key = "rate_limit:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        return count <= rateLimitPerMinute;
    }

    /**
     * Generate cache key
     */
    private String generateCacheKey(QueryRequestDto request) {
        return String.format("query:%s:%s:%s",
            request.getMbtiType(),
            request.getQueryText().hashCode(),
            request.getLocation() != null ? request.getLocation().hashCode() : "null"
        );
    }

    /**
     * Get cached response
     */
    @SuppressWarnings("unchecked")
    private QueryResponseDto getCachedResponse(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.convertValue(cached, QueryResponseDto.class);
            }
        } catch (Exception e) {
            log.warn("Error retrieving cached response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Cache response
     */
    private void cacheResponse(String key, QueryResponseDto response) {
        try {
            redisTemplate.opsForValue().set(key, response, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Error caching response: {}", e.getMessage());
        }
    }

    /**
     * Check provider health
     */
    public String checkProviderHealth(String provider) {
        try {
            String testPrompt = "Health check";
            callLLMProvider(provider, testPrompt);
            return "UP";
        } catch (Exception e) {
            log.warn("Provider {} health check failed: {}", provider, e.getMessage());
            return "DOWN";
        }
    }

    /**
     * Get MBTI insights
     */
    @Cacheable(value = "mbti-insights", key = "#mbtiType")
    public MbtiInsightsDto getMbtiInsights(String mbtiType) {
        return mbtiPromptService.getMbtiInsights(mbtiType);
    }
}
