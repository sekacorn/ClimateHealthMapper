package com.climate.integrator.service;

import com.climate.integrator.model.EnvData;
import com.climate.integrator.repository.EnvDataRepository;
import com.climate.integrator.utils.CsvValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing environmental data from NOAA, EPA, and other sources
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnvironmentalDataService {

    private final EnvDataRepository envDataRepository;
    private final CsvValidator csvValidator;
    private final ObjectMapper objectMapper;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    /**
     * Process and store environmental data from CSV file
     */
    @Transactional
    public List<EnvData> processEnvironmentalCsv(MultipartFile file, String userId, String dataSource)
            throws IOException {
        log.info("Processing environmental CSV for user: {}, source: {}", userId, dataSource);

        // Read file content
        String content = new String(file.getBytes());

        // Validate CSV
        List<String> validationErrors = csvValidator.validateEnvironmentalCsv(content);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("CSV validation failed: " +
                String.join(", ", validationErrors));
        }

        List<EnvData> dataList = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setIgnoreHeaderCase(true)
                 .setTrim(true)
                 .build())) {

            for (CSVRecord record : parser) {
                try {
                    EnvData envData = parseCsvRecord(record, userId, dataSource);
                    dataList.add(envData);
                } catch (Exception e) {
                    log.error("Error parsing CSV record {}: {}", record.getRecordNumber(), e.getMessage());
                    // Continue processing other records
                }
            }
        }

        // Save all records
        List<EnvData> savedData = envDataRepository.saveAll(dataList);
        log.info("Successfully saved {} environmental data records", savedData.size());

        return savedData;
    }

    /**
     * Process and store environmental data from JSON
     */
    @Transactional
    public List<EnvData> processEnvironmentalJson(MultipartFile file, String userId, String dataSource)
            throws IOException {
        log.info("Processing environmental JSON for user: {}, source: {}", userId, dataSource);

        JsonNode rootNode = objectMapper.readTree(file.getInputStream());
        List<EnvData> dataList = new ArrayList<>();

        // Handle array or single object
        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                EnvData envData = parseJsonNode(node, userId, dataSource);
                dataList.add(envData);
            }
        } else {
            EnvData envData = parseJsonNode(rootNode, userId, dataSource);
            dataList.add(envData);
        }

        // Save all records
        List<EnvData> savedData = envDataRepository.saveAll(dataList);
        log.info("Successfully saved {} environmental data records from JSON", savedData.size());

        return savedData;
    }

    /**
     * Parse CSV record to EnvData entity
     */
    private EnvData parseCsvRecord(CSVRecord record, String userId, String dataSource) {
        return EnvData.builder()
            .userId(userId)
            .dataSource(dataSource)
            .measurementDate(parseDate(record.get("date")))
            .latitude(parseDouble(record.get("latitude")))
            .longitude(parseDouble(record.get("longitude")))
            .location(getOptionalValue(record, "location"))
            .temperature(parseOptionalDouble(record, "temperature"))
            .temperatureMin(parseOptionalDouble(record, "temperature_min"))
            .temperatureMax(parseOptionalDouble(record, "temperature_max"))
            .aqi(parseOptionalDouble(record, "aqi"))
            .pm25(parseOptionalDouble(record, "pm25"))
            .pm10(parseOptionalDouble(record, "pm10"))
            .ozone(parseOptionalDouble(record, "ozone"))
            .no2(parseOptionalDouble(record, "no2"))
            .so2(parseOptionalDouble(record, "so2"))
            .co(parseOptionalDouble(record, "co"))
            .precipitation(parseOptionalDouble(record, "precipitation"))
            .humidity(parseOptionalDouble(record, "humidity"))
            .windSpeed(parseOptionalDouble(record, "wind_speed"))
            .windDirection(parseOptionalDouble(record, "wind_direction"))
            .uvIndex(parseOptionalDouble(record, "uv_index"))
            .pressure(parseOptionalDouble(record, "pressure"))
            .build();
    }

    /**
     * Parse JSON node to EnvData entity
     */
    private EnvData parseJsonNode(JsonNode node, String userId, String dataSource) {
        return EnvData.builder()
            .userId(userId)
            .dataSource(dataSource)
            .measurementDate(parseDate(node.get("date").asText()))
            .latitude(node.get("latitude").asDouble())
            .longitude(node.get("longitude").asDouble())
            .location(node.has("location") ? node.get("location").asText() : null)
            .temperature(getJsonDouble(node, "temperature"))
            .temperatureMin(getJsonDouble(node, "temperature_min"))
            .temperatureMax(getJsonDouble(node, "temperature_max"))
            .aqi(getJsonDouble(node, "aqi"))
            .pm25(getJsonDouble(node, "pm25"))
            .pm10(getJsonDouble(node, "pm10"))
            .ozone(getJsonDouble(node, "ozone"))
            .no2(getJsonDouble(node, "no2"))
            .so2(getJsonDouble(node, "so2"))
            .co(getJsonDouble(node, "co"))
            .precipitation(getJsonDouble(node, "precipitation"))
            .humidity(getJsonDouble(node, "humidity"))
            .windSpeed(getJsonDouble(node, "wind_speed"))
            .windDirection(getJsonDouble(node, "wind_direction"))
            .uvIndex(getJsonDouble(node, "uv_index"))
            .pressure(getJsonDouble(node, "pressure"))
            .metadata(node.toString())
            .build();
    }

    /**
     * Get all environmental data for a user
     */
    @Cacheable(value = "envData", key = "#userId")
    public List<EnvData> getEnvironmentalData(String userId) {
        log.info("Fetching environmental data for user: {}", userId);
        return envDataRepository.findByUserId(userId);
    }

    /**
     * Get environmental data within date range
     */
    public List<EnvData> getEnvironmentalDataByDateRange(
            String userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching environmental data for user: {} from {} to {}",
            userId, startDate, endDate);
        return envDataRepository.findByUserIdAndMeasurementDateBetween(userId, startDate, endDate);
    }

    /**
     * Parse date string with multiple formats
     */
    private LocalDateTime parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception e) {
                // Try next formatter
            }
        }
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }

    /**
     * Parse double value safely
     */
    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + value);
        }
    }

    /**
     * Parse optional double value
     */
    private Double parseOptionalDouble(CSVRecord record, String columnName) {
        try {
            if (record.isMapped(columnName)) {
                String value = record.get(columnName);
                if (value != null && !value.trim().isEmpty()) {
                    return Double.parseDouble(value);
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid number for column {}: {}", columnName, record.get(columnName));
        }
        return null;
    }

    /**
     * Get optional CSV value
     */
    private String getOptionalValue(CSVRecord record, String columnName) {
        try {
            if (record.isMapped(columnName)) {
                return record.get(columnName);
            }
        } catch (Exception e) {
            // Column doesn't exist
        }
        return null;
    }

    /**
     * Get optional JSON double value
     */
    private Double getJsonDouble(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asDouble();
        }
        return null;
    }
}
