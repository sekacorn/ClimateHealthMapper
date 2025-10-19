package com.climate.integrator.service;

import com.climate.integrator.model.EnvData;
import com.climate.integrator.repository.EnvDataRepository;
import com.climate.integrator.utils.CsvValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnvironmentalDataService
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentalDataServiceTest {

    @Mock
    private EnvDataRepository envDataRepository;

    @Mock
    private CsvValidator csvValidator;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EnvironmentalDataService environmentalDataService;

    private String userId;
    private String dataSource;

    @BeforeEach
    void setUp() {
        userId = "test-user-123";
        dataSource = "NOAA";
    }

    @Test
    void testProcessEnvironmentalCsv_Success() throws IOException {
        // Prepare test CSV data
        String csvContent = "date,latitude,longitude,temperature,aqi,pm25\n" +
                           "2025-01-15,40.7128,-74.0060,15.5,45,12.3\n" +
                           "2025-01-16,40.7128,-74.0060,14.2,52,15.8\n";

        MultipartFile file = new MockMultipartFile(
            "file",
            "environmental-data.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Mock validator
        when(csvValidator.validateEnvironmentalCsv(anyString()))
            .thenReturn(new ArrayList<>());

        // Mock repository
        List<EnvData> savedData = createMockEnvDataList(2);
        when(envDataRepository.saveAll(anyList())).thenReturn(savedData);

        // Execute
        List<EnvData> result = environmentalDataService.processEnvironmentalCsv(
            file, userId, dataSource);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(csvValidator, times(1)).validateEnvironmentalCsv(anyString());
        verify(envDataRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testProcessEnvironmentalCsv_ValidationFailure() {
        // Prepare test CSV data with invalid content
        String csvContent = "invalid,csv,data\n1,2,3\n";

        MultipartFile file = new MockMultipartFile(
            "file",
            "invalid-data.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Mock validator to return errors
        List<String> errors = List.of("Missing required header: date");
        when(csvValidator.validateEnvironmentalCsv(anyString())).thenReturn(errors);

        // Execute and verify exception
        assertThrows(IllegalArgumentException.class, () -> {
            environmentalDataService.processEnvironmentalCsv(file, userId, dataSource);
        });

        verify(envDataRepository, never()).saveAll(anyList());
    }

    @Test
    void testProcessEnvironmentalJson_Success() throws IOException {
        // Prepare test JSON data
        String jsonContent = "[{" +
            "\"date\":\"2025-01-15\"," +
            "\"latitude\":40.7128," +
            "\"longitude\":-74.0060," +
            "\"temperature\":15.5," +
            "\"aqi\":45," +
            "\"pm25\":12.3" +
            "}]";

        MultipartFile file = new MockMultipartFile(
            "file",
            "environmental-data.json",
            "application/json",
            jsonContent.getBytes()
        );

        // Mock ObjectMapper
        ObjectMapper realObjectMapper = new ObjectMapper();
        when(objectMapper.readTree(any(java.io.InputStream.class)))
            .thenReturn(realObjectMapper.readTree(jsonContent));

        // Mock repository
        List<EnvData> savedData = createMockEnvDataList(1);
        when(envDataRepository.saveAll(anyList())).thenReturn(savedData);

        // Execute
        List<EnvData> result = environmentalDataService.processEnvironmentalJson(
            file, userId, dataSource);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(envDataRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testGetEnvironmentalData() {
        // Mock repository
        List<EnvData> mockData = createMockEnvDataList(5);
        when(envDataRepository.findByUserId(userId)).thenReturn(mockData);

        // Execute
        List<EnvData> result = environmentalDataService.getEnvironmentalData(userId);

        // Verify
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(envDataRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetEnvironmentalDataByDateRange() {
        // Prepare date range
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 1, 31, 23, 59);

        // Mock repository
        List<EnvData> mockData = createMockEnvDataList(3);
        when(envDataRepository.findByUserIdAndMeasurementDateBetween(
            userId, startDate, endDate)).thenReturn(mockData);

        // Execute
        List<EnvData> result = environmentalDataService.getEnvironmentalDataByDateRange(
            userId, startDate, endDate);

        // Verify
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(envDataRepository, times(1))
            .findByUserIdAndMeasurementDateBetween(userId, startDate, endDate);
    }

    @Test
    void testProcessEnvironmentalCsv_EmptyFile() {
        // Prepare empty file
        MultipartFile file = new MockMultipartFile(
            "file",
            "empty.csv",
            "text/csv",
            "".getBytes()
        );

        // Mock validator to return error for empty content
        List<String> errors = List.of("CSV content is empty");
        when(csvValidator.validateEnvironmentalCsv(anyString())).thenReturn(errors);

        // Execute and verify exception
        assertThrows(IllegalArgumentException.class, () -> {
            environmentalDataService.processEnvironmentalCsv(file, userId, dataSource);
        });
    }

    @Test
    void testProcessEnvironmentalCsv_MissingRequiredFields() {
        // Prepare CSV with missing required fields
        String csvContent = "date,latitude\n2025-01-15,40.7128\n";

        MultipartFile file = new MockMultipartFile(
            "file",
            "incomplete-data.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Mock validator to return error
        List<String> errors = List.of("Missing required header: longitude");
        when(csvValidator.validateEnvironmentalCsv(anyString())).thenReturn(errors);

        // Execute and verify exception
        assertThrows(IllegalArgumentException.class, () -> {
            environmentalDataService.processEnvironmentalCsv(file, userId, dataSource);
        });
    }

    @Test
    void testProcessEnvironmentalCsv_WithOptionalFields() throws IOException {
        // Prepare CSV with all fields
        String csvContent = "date,latitude,longitude,temperature,temperature_min,temperature_max," +
                           "aqi,pm25,pm10,ozone,no2,so2,co,precipitation,humidity," +
                           "wind_speed,wind_direction,uv_index,pressure,location\n" +
                           "2025-01-15,40.7128,-74.0060,15.5,12.0,18.0," +
                           "45,12.3,20.5,0.05,15.2,8.3,0.4,2.5,65," +
                           "12.5,180,4.5,1013.25,New York\n";

        MultipartFile file = new MockMultipartFile(
            "file",
            "complete-data.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Mock validator
        when(csvValidator.validateEnvironmentalCsv(anyString()))
            .thenReturn(new ArrayList<>());

        // Mock repository
        List<EnvData> savedData = createMockEnvDataList(1);
        when(envDataRepository.saveAll(anyList())).thenReturn(savedData);

        // Execute
        List<EnvData> result = environmentalDataService.processEnvironmentalCsv(
            file, userId, dataSource);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(envDataRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testProcessEnvironmentalCsv_MultipleRecords() throws IOException {
        // Prepare CSV with multiple records
        StringBuilder csvContent = new StringBuilder(
            "date,latitude,longitude,temperature,aqi\n");
        for (int i = 0; i < 100; i++) {
            csvContent.append(String.format(
                "2025-01-15,40.7128,-74.0060,%d.5,%d\n", i, i * 10));
        }

        MultipartFile file = new MockMultipartFile(
            "file",
            "multiple-records.csv",
            "text/csv",
            csvContent.toString().getBytes()
        );

        // Mock validator
        when(csvValidator.validateEnvironmentalCsv(anyString()))
            .thenReturn(new ArrayList<>());

        // Mock repository
        List<EnvData> savedData = createMockEnvDataList(100);
        when(envDataRepository.saveAll(anyList())).thenReturn(savedData);

        // Execute
        List<EnvData> result = environmentalDataService.processEnvironmentalCsv(
            file, userId, dataSource);

        // Verify
        assertNotNull(result);
        assertEquals(100, result.size());
        verify(envDataRepository, times(1)).saveAll(anyList());
    }

    /**
     * Helper method to create mock EnvData list
     */
    private List<EnvData> createMockEnvDataList(int count) {
        List<EnvData> dataList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EnvData data = EnvData.builder()
                .id((long) (i + 1))
                .userId(userId)
                .dataSource(dataSource)
                .measurementDate(LocalDateTime.now())
                .latitude(40.7128)
                .longitude(-74.0060)
                .temperature(15.5 + i)
                .aqi(45.0 + i)
                .pm25(12.3 + i)
                .build();
            dataList.add(data);
        }
        return dataList;
    }
}
