package com.climate.integrator.controller;

import com.climate.integrator.model.EnvData;
import com.climate.integrator.model.GenomicData;
import com.climate.integrator.model.HealthData;
import com.climate.integrator.service.EnvironmentalDataService;
import com.climate.integrator.service.GenomicDataService;
import com.climate.integrator.service.HealthDataService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for data upload and retrieval
 */
@RestController
@RequestMapping("/api/integrator")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DataUploadController {

    private final EnvironmentalDataService environmentalDataService;
    private final HealthDataService healthDataService;
    private final GenomicDataService genomicDataService;

    /**
     * Upload environmental data (CSV/JSON)
     *
     * @param file CSV or JSON file containing environmental data
     * @param userId User identifier
     * @param dataSource Data source (NOAA, EPA, etc.)
     * @return Response with upload status and record count
     */
    @PostMapping(value = "/upload/environmental", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadEnvironmentalData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") @NotBlank String userId,
            @RequestParam("dataSource") @NotBlank String dataSource) {

        log.info("Received environmental data upload request - userId: {}, source: {}, filename: {}",
            userId, dataSource, file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid filename"));
            }

            List<EnvData> savedData;

            // Process based on file type
            if (filename.endsWith(".csv")) {
                savedData = environmentalDataService.processEnvironmentalCsv(file, userId, dataSource);
            } else if (filename.endsWith(".json")) {
                savedData = environmentalDataService.processEnvironmentalJson(file, userId, dataSource);
            } else {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Unsupported file format. Please upload CSV or JSON"));
            }

            log.info("Successfully processed {} environmental records", savedData.size());

            return ResponseEntity.ok(createSuccessResponse(
                "Environmental data uploaded successfully",
                savedData.size(),
                "environmental"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing environmental data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error processing file: " + e.getMessage()));
        }
    }

    /**
     * Upload health data (FHIR)
     *
     * @param file FHIR JSON file (Bundle or single Resource)
     * @param userId User identifier
     * @return Response with upload status and record count
     */
    @PostMapping(value = "/upload/health", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadHealthData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") @NotBlank String userId) {

        log.info("Received health data upload request - userId: {}, filename: {}",
            userId, file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".json")) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid file format. Please upload FHIR JSON"));
            }

            // Process FHIR data
            List<HealthData> savedData = healthDataService.processFhirData(file, userId);

            log.info("Successfully processed {} health records", savedData.size());

            return ResponseEntity.ok(createSuccessResponse(
                "Health data uploaded successfully",
                savedData.size(),
                "health"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing health data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error processing FHIR file: " + e.getMessage()));
        }
    }

    /**
     * Upload genomic data (VCF)
     *
     * @param file VCF file containing genomic variants
     * @param userId User identifier
     * @return Response with upload status and record count
     */
    @PostMapping(value = "/upload/genomic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadGenomicData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") @NotBlank String userId) {

        log.info("Received genomic data upload request - userId: {}, filename: {}",
            userId, file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".vcf")) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid file format. Please upload VCF file"));
            }

            // Process VCF data
            List<GenomicData> savedData = genomicDataService.processVcfData(file, userId);

            log.info("Successfully processed {} genomic variants", savedData.size());

            return ResponseEntity.ok(createSuccessResponse(
                "Genomic data uploaded successfully",
                savedData.size(),
                "genomic"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing genomic data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error processing VCF file: " + e.getMessage()));
        }
    }

    /**
     * Get all integrated data for a user
     *
     * @param userId User identifier
     * @return Combined data from all sources
     */
    @GetMapping("/data/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserData(@PathVariable @NotBlank String userId) {
        log.info("Fetching integrated data for user: {}", userId);

        try {
            Map<String, Object> response = new HashMap<>();

            // Fetch all data types
            List<EnvData> envData = environmentalDataService.getEnvironmentalData(userId);
            List<HealthData> healthData = healthDataService.getHealthData(userId);
            List<GenomicData> genomicData = genomicDataService.getGenomicData(userId);

            response.put("userId", userId);
            response.put("environmental", Map.of(
                "count", envData.size(),
                "data", envData
            ));
            response.put("health", Map.of(
                "count", healthData.size(),
                "data", healthData
            ));
            response.put("genomic", Map.of(
                "count", genomicData.size(),
                "data", genomicData
            ));

            log.info("Retrieved data for user {}: {} env, {} health, {} genomic",
                userId, envData.size(), healthData.size(), genomicData.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving user data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving data: " + e.getMessage()));
        }
    }

    /**
     * Get environmental data for a user
     */
    @GetMapping("/data/{userId}/environmental")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getEnvironmentalData(@PathVariable @NotBlank String userId) {
        try {
            List<EnvData> data = environmentalDataService.getEnvironmentalData(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error retrieving environmental data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving data: " + e.getMessage()));
        }
    }

    /**
     * Get health data for a user
     */
    @GetMapping("/data/{userId}/health")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getHealthData(@PathVariable @NotBlank String userId) {
        try {
            List<HealthData> data = healthDataService.getHealthData(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error retrieving health data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving data: " + e.getMessage()));
        }
    }

    /**
     * Get genomic data for a user
     */
    @GetMapping("/data/{userId}/genomic")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getGenomicData(@PathVariable @NotBlank String userId) {
        try {
            List<GenomicData> data = genomicDataService.getGenomicData(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error retrieving genomic data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving data: " + e.getMessage()));
        }
    }

    /**
     * Get climate-relevant genomic variants for a user
     */
    @GetMapping("/data/{userId}/genomic/climate-relevant")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getClimateRelevantVariants(@PathVariable @NotBlank String userId) {
        try {
            List<GenomicData> data = genomicDataService.getClimateRelevantVariants(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error retrieving climate-relevant variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving data: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "climate-integrator");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Create success response
     */
    private Map<String, Object> createSuccessResponse(String message, int count, String dataType) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("recordsProcessed", count);
        response.put("dataType", dataType);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleValidationException(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(createErrorResponse(e.getMessage()));
    }

    /**
     * Exception handler for general errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("An unexpected error occurred"));
    }
}
