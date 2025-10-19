package com.climate.integrator.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for CSV validation and parsing
 */
@Component
@Slf4j
public class CsvValidator {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_RECORDS = 100000;
    private static final Set<String> REQUIRED_ENV_HEADERS = Set.of(
        "date", "latitude", "longitude"
    );

    /**
     * Validates CSV content and structure
     *
     * @param csvContent CSV content as string
     * @param requiredHeaders Required header names
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateCsv(String csvContent, Set<String> requiredHeaders) {
        List<String> errors = new ArrayList<>();

        // Check file size
        if (csvContent == null || csvContent.isEmpty()) {
            errors.add("CSV content is empty");
            return errors;
        }

        if (csvContent.length() > MAX_FILE_SIZE) {
            errors.add("CSV file size exceeds maximum allowed size of " + MAX_FILE_SIZE + " bytes");
            return errors;
        }

        try (Reader reader = new StringReader(csvContent);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setIgnoreHeaderCase(true)
                 .setTrim(true)
                 .build())) {

            // Validate headers
            Set<String> headers = parser.getHeaderMap().keySet();
            for (String required : requiredHeaders) {
                if (!headers.stream().anyMatch(h -> h.equalsIgnoreCase(required))) {
                    errors.add("Missing required header: " + required);
                }
            }

            if (!errors.isEmpty()) {
                return errors;
            }

            // Validate records
            int recordCount = 0;
            for (CSVRecord record : parser) {
                recordCount++;

                if (recordCount > MAX_RECORDS) {
                    errors.add("CSV exceeds maximum allowed records of " + MAX_RECORDS);
                    break;
                }

                // Validate individual record
                List<String> recordErrors = validateRecord(record, recordCount);
                errors.addAll(recordErrors);

                // Stop if too many errors
                if (errors.size() > 100) {
                    errors.add("Too many validation errors. Stopping validation.");
                    break;
                }
            }

            if (recordCount == 0) {
                errors.add("CSV file contains no data records");
            }

        } catch (IOException e) {
            log.error("Error parsing CSV: {}", e.getMessage());
            errors.add("Error parsing CSV: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Validates a single CSV record
     */
    private List<String> validateRecord(CSVRecord record, int rowNumber) {
        List<String> errors = new ArrayList<>();

        // Validate date field
        String dateValue = record.get("date");
        if (dateValue == null || dateValue.trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": date is required");
        } else {
            if (!isValidDate(dateValue)) {
                errors.add("Row " + rowNumber + ": invalid date format - " + dateValue);
            }
        }

        // Validate latitude
        String latValue = record.get("latitude");
        if (!isValidLatitude(latValue)) {
            errors.add("Row " + rowNumber + ": invalid latitude - " + latValue);
        }

        // Validate longitude
        String lonValue = record.get("longitude");
        if (!isValidLongitude(lonValue)) {
            errors.add("Row " + rowNumber + ": invalid longitude - " + lonValue);
        }

        return errors;
    }

    /**
     * Validates date string
     */
    public boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }

        // Try common date formats
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime.parse(dateStr, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        return false;
    }

    /**
     * Validates latitude value
     */
    public boolean isValidLatitude(String latStr) {
        try {
            double lat = Double.parseDouble(latStr);
            return lat >= -90.0 && lat <= 90.0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Validates longitude value
     */
    public boolean isValidLongitude(String lonStr) {
        try {
            double lon = Double.parseDouble(lonStr);
            return lon >= -180.0 && lon <= 180.0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Validates numeric value
     */
    public boolean isValidNumber(String numStr) {
        try {
            Double.parseDouble(numStr);
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Sanitizes input string to prevent injection attacks
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'%;()&+]", "");
    }

    /**
     * Validates environmental CSV specifically
     */
    public List<String> validateEnvironmentalCsv(String csvContent) {
        return validateCsv(csvContent, REQUIRED_ENV_HEADERS);
    }
}
