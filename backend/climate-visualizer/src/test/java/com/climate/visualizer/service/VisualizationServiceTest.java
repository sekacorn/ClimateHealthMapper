package com.climate.visualizer.service;

import com.climate.visualizer.model.Visualization;
import com.climate.visualizer.repository.VisualizationRepository;
import com.climate.visualizer.utils.MultithreadingManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisualizationService
 */
@ExtendWith(MockitoExtension.class)
class VisualizationServiceTest {

    @Mock
    private VisualizationRepository visualizationRepository;

    @Mock
    private MbtiStyleService mbtiStyleService;

    @Mock
    private MultithreadingManager multithreadingManager;

    @Mock
    private ResourceMonitorService resourceMonitorService;

    @InjectMocks
    private VisualizationService visualizationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Use reflection to set the ObjectMapper
        try {
            var field = VisualizationService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(visualizationService, objectMapper);
        } catch (Exception e) {
            fail("Failed to inject ObjectMapper: " + e.getMessage());
        }
    }

    @Test
    void testGenerateClimateMap_Success() throws Exception {
        // Arrange
        String userId = "user123";
        Double latitude = 40.7128;
        Double longitude = -74.0060;
        Double radius = 50.0;
        String mbtiType = "INTJ";
        String quality = "medium";

        Map<String, Object> mockMaterials = new HashMap<>();
        mockMaterials.put("materials", Arrays.asList(
            Map.of("type", "MeshStandardMaterial", "color", 0x2C3E50)
        ));

        when(mbtiStyleService.isValidMbtiType(mbtiType)).thenReturn(true);
        when(mbtiStyleService.generateClimateMaterials(eq(mbtiType), any()))
            .thenReturn(mockMaterials);

        when(multithreadingManager.executeAsync(any(java.util.function.Supplier.class)))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                return java.util.concurrent.CompletableFuture.completedFuture(supplier.get());
            });

        Visualization mockVisualization = Visualization.builder()
            .id(1L)
            .userId(userId)
            .title("Climate Map - " + latitude + ", " + longitude)
            .visualizationType(Visualization.VisualizationType.CLIMATE_MAP_3D)
            .mbtiType(mbtiType)
            .visualizationData("{}")
            .processingTimeMs(100L)
            .build();

        when(visualizationRepository.save(any(Visualization.class)))
            .thenReturn(mockVisualization);

        // Act
        Map<String, Object> result = visualizationService.generateClimateMap(
            userId, latitude, longitude, radius, mbtiType, quality);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("visualizationId"));
        assertTrue(result.containsKey("data"));
        assertTrue(result.containsKey("processingTime"));
        assertTrue(result.containsKey("dataPoints"));

        assertEquals(1L, result.get("visualizationId"));

        verify(visualizationRepository, times(1)).save(any(Visualization.class));
        verify(mbtiStyleService, times(1)).generateClimateMaterials(eq(mbtiType), any());
    }

    @Test
    void testGenerateClimateMap_InvalidCoordinates() {
        // Arrange
        String userId = "user123";
        Double invalidLatitude = 100.0; // Invalid
        Double longitude = -74.0060;
        Double radius = 50.0;
        String mbtiType = "INTJ";
        String quality = "medium";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            visualizationService.generateClimateMap(
                userId, invalidLatitude, longitude, radius, mbtiType, quality);
        });

        verify(visualizationRepository, never()).save(any(Visualization.class));
    }

    @Test
    void testGenerateClimateMap_InvalidMbtiType() {
        // Arrange
        String userId = "user123";
        Double latitude = 40.7128;
        Double longitude = -74.0060;
        Double radius = 50.0;
        String mbtiType = "INVALID";
        String quality = "medium";

        when(mbtiStyleService.isValidMbtiType(mbtiType)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            visualizationService.generateClimateMap(
                userId, latitude, longitude, radius, mbtiType, quality);
        });

        verify(visualizationRepository, never()).save(any(Visualization.class));
    }

    @Test
    void testGenerateHealthHeatmap_Success() throws Exception {
        // Arrange
        String userId = "user456";
        Double latitude = 34.0522;
        Double longitude = -118.2437;
        Double radius = 50.0;
        List<String> healthFactors = Arrays.asList("heat_stress", "respiratory");
        String mbtiType = "ENFP";
        String quality = "medium";

        Map<String, Object> mockColors = new HashMap<>();
        mockColors.put("gradient", Arrays.asList("#F39C12", "#E74C3C"));

        when(mbtiStyleService.isValidMbtiType(mbtiType)).thenReturn(true);
        when(mbtiStyleService.generateHealthHeatmapColors(eq(mbtiType), any()))
            .thenReturn(mockColors);

        when(multithreadingManager.executeAsync(any(java.util.function.Supplier.class)))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                return java.util.concurrent.CompletableFuture.completedFuture(supplier.get());
            });

        Visualization mockVisualization = Visualization.builder()
            .id(2L)
            .userId(userId)
            .title("Health Heatmap - " + latitude + ", " + longitude)
            .visualizationType(Visualization.VisualizationType.HEALTH_HEATMAP)
            .mbtiType(mbtiType)
            .visualizationData("{}")
            .processingTimeMs(150L)
            .build();

        when(visualizationRepository.save(any(Visualization.class)))
            .thenReturn(mockVisualization);

        // Act
        Map<String, Object> result = visualizationService.generateHealthHeatmap(
            userId, latitude, longitude, radius, healthFactors, mbtiType, quality);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("visualizationId"));
        assertTrue(result.containsKey("data"));
        assertTrue(result.containsKey("processingTime"));
        assertTrue(result.containsKey("riskPoints"));

        assertEquals(2L, result.get("visualizationId"));

        verify(visualizationRepository, times(1)).save(any(Visualization.class));
        verify(mbtiStyleService, times(1)).generateHealthHeatmapColors(eq(mbtiType), any());
    }

    @Test
    void testGetUserVisualizations() {
        // Arrange
        String userId = "user789";
        List<Visualization> mockVisualizations = Arrays.asList(
            Visualization.builder().id(1L).userId(userId).build(),
            Visualization.builder().id(2L).userId(userId).build()
        );

        when(visualizationRepository.findByUserId(userId))
            .thenReturn(mockVisualizations);

        // Act
        List<Visualization> result = visualizationService.getUserVisualizations(userId, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(visualizationRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetUserVisualizations_WithTypeFilter() {
        // Arrange
        String userId = "user789";
        String type = "CLIMATE_MAP_3D";
        List<Visualization> mockVisualizations = Arrays.asList(
            Visualization.builder()
                .id(1L)
                .userId(userId)
                .visualizationType(Visualization.VisualizationType.CLIMATE_MAP_3D)
                .build()
        );

        when(visualizationRepository.findByUserIdAndVisualizationType(
            userId, Visualization.VisualizationType.CLIMATE_MAP_3D))
            .thenReturn(mockVisualizations);

        // Act
        List<Visualization> result = visualizationService.getUserVisualizations(userId, type);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Visualization.VisualizationType.CLIMATE_MAP_3D,
                     result.get(0).getVisualizationType());
        verify(visualizationRepository, times(1))
            .findByUserIdAndVisualizationType(userId, Visualization.VisualizationType.CLIMATE_MAP_3D);
    }

    @Test
    void testGetVisualization_Found() {
        // Arrange
        Long id = 1L;
        Visualization mockVisualization = Visualization.builder()
            .id(id)
            .userId("user123")
            .title("Test Visualization")
            .build();

        when(visualizationRepository.findById(id))
            .thenReturn(Optional.of(mockVisualization));

        // Act
        Optional<Visualization> result = visualizationService.getVisualization(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(visualizationRepository, times(1)).findById(id);
    }

    @Test
    void testGetVisualization_NotFound() {
        // Arrange
        Long id = 999L;

        when(visualizationRepository.findById(id))
            .thenReturn(Optional.empty());

        // Act
        Optional<Visualization> result = visualizationService.getVisualization(id);

        // Assert
        assertFalse(result.isPresent());
        verify(visualizationRepository, times(1)).findById(id);
    }

    @Test
    void testDeleteVisualization() {
        // Arrange
        Long id = 1L;

        doNothing().when(visualizationRepository).deleteById(id);

        // Act
        visualizationService.deleteVisualization(id);

        // Assert
        verify(visualizationRepository, times(1)).deleteById(id);
    }

    @Test
    void testValidateCoordinates_ValidLatitude() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            visualizationService.generateClimateMap(
                "user", 45.0, -74.0, 50.0, "INTJ", "medium");
        });
    }

    @Test
    void testValidateCoordinates_InvalidLatitudeTooHigh() {
        when(mbtiStyleService.isValidMbtiType(any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            visualizationService.generateClimateMap(
                "user", 91.0, -74.0, 50.0, "INTJ", "medium");
        });
    }

    @Test
    void testValidateCoordinates_InvalidLatitudeTooLow() {
        when(mbtiStyleService.isValidMbtiType(any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            visualizationService.generateClimateMap(
                "user", -91.0, -74.0, 50.0, "INTJ", "medium");
        });
    }

    @Test
    void testValidateCoordinates_InvalidLongitudeTooHigh() {
        when(mbtiStyleService.isValidMbtiType(any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            visualizationService.generateClimateMap(
                "user", 45.0, 181.0, 50.0, "INTJ", "medium");
        });
    }

    @Test
    void testValidateCoordinates_InvalidLongitudeTooLow() {
        when(mbtiStyleService.isValidMbtiType(any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            visualizationService.generateClimateMap(
                "user", 45.0, -181.0, 50.0, "INTJ", "medium");
        });
    }
}
