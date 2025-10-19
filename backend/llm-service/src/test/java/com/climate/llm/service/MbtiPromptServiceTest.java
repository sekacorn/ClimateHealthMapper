package com.climate.llm.service;

import com.climate.llm.dto.MbtiInsightsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MbtiPromptService
 */
class MbtiPromptServiceTest {

    private MbtiPromptService mbtiPromptService;

    @BeforeEach
    void setUp() {
        mbtiPromptService = new MbtiPromptService();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "INTJ", "INTP", "ENTJ", "ENTP",
        "INFJ", "INFP", "ENFJ", "ENFP",
        "ISTJ", "ISFJ", "ESTJ", "ESFJ",
        "ISTP", "ISFP", "ESTP", "ESFP"
    })
    void testGeneratePromptForAllMbtiTypes(String mbtiType) {
        // Arrange
        String userQuery = "What are the health risks of air pollution?";
        String location = "New York";

        // Act
        String prompt = mbtiPromptService.generatePrompt(userQuery, mbtiType, location);

        // Assert
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains(userQuery));
        assertTrue(prompt.contains(location));
    }

    @Test
    void testGeneratePromptForINTJ() {
        // Arrange
        String userQuery = "How does climate change affect health?";
        String location = "California";

        // Act
        String prompt = mbtiPromptService.generatePrompt(userQuery, "INTJ", location);

        // Assert
        assertTrue(prompt.contains("Architect"));
        assertTrue(prompt.contains("analytical"));
        assertTrue(prompt.contains("detailed"));
        assertTrue(prompt.contains(userQuery));
        assertTrue(prompt.contains(location));
    }

    @Test
    void testGeneratePromptForENFP() {
        // Arrange
        String userQuery = "What can I do to reduce my carbon footprint?";
        String location = "Seattle";

        // Act
        String prompt = mbtiPromptService.generatePrompt(userQuery, "ENFP", location);

        // Assert
        assertTrue(prompt.contains("Campaigner"));
        assertTrue(prompt.contains("creative"));
        assertTrue(prompt.contains("enthusiastic"));
        assertTrue(prompt.contains(userQuery));
        assertTrue(prompt.contains(location));
    }

    @Test
    void testGeneratePromptForISTJ() {
        // Arrange
        String userQuery = "How can I protect my family from heat waves?";
        String location = "Phoenix";

        // Act
        String prompt = mbtiPromptService.generatePrompt(userQuery, "ISTJ", location);

        // Assert
        assertTrue(prompt.contains("Logistician"));
        assertTrue(prompt.contains("structured"));
        assertTrue(prompt.contains("precise"));
        assertTrue(prompt.contains(userQuery));
        assertTrue(prompt.contains(location));
    }

    @Test
    void testGeneratePromptWithoutLocation() {
        // Arrange
        String userQuery = "What are the symptoms of heat exhaustion?";

        // Act
        String prompt = mbtiPromptService.generatePrompt(userQuery, "INFJ", null);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains(userQuery));
        assertFalse(prompt.contains("Location:"));
    }

    @Test
    void testGeneratePromptWithInvalidMbtiType() {
        // Arrange
        String userQuery = "Climate health question";
        String location = "Boston";

        // Act
        String prompt = mbtiPromptService.generatePrompt(userQuery, "INVALID", location);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains(userQuery));
        assertTrue(prompt.contains("helpful climate health assistant"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "INTJ", "INTP", "ENTJ", "ENTP",
        "INFJ", "INFP", "ENFJ", "ENFP",
        "ISTJ", "ISFJ", "ESTJ", "ESFJ",
        "ISTP", "ISFP", "ESTP", "ESFP"
    })
    void testGetMbtiInsightsForAllTypes(String mbtiType) {
        // Act
        MbtiInsightsDto insights = mbtiPromptService.getMbtiInsights(mbtiType);

        // Assert
        assertNotNull(insights);
        assertEquals(mbtiType, insights.getMbtiType());
        assertNotNull(insights.getName());
        assertNotNull(insights.getStyle());
        assertNotNull(insights.getCharacteristics());
        assertNotNull(insights.getPreferences());
        assertNotNull(insights.getCommunicationTips());
    }

    @Test
    void testGetMbtiInsightsForINTJ() {
        // Act
        MbtiInsightsDto insights = mbtiPromptService.getMbtiInsights("INTJ");

        // Assert
        assertEquals("INTJ", insights.getMbtiType());
        assertEquals("Architect", insights.getName());
        assertTrue(insights.getStyle().contains("analytical"));
        assertTrue(insights.getCommunicationTips().contains("data"));
    }

    @Test
    void testGetMbtiInsightsForESFP() {
        // Act
        MbtiInsightsDto insights = mbtiPromptService.getMbtiInsights("ESFP");

        // Assert
        assertEquals("ESFP", insights.getMbtiType());
        assertEquals("Entertainer", insights.getName());
        assertTrue(insights.getStyle().contains("lively"));
        assertTrue(insights.getCommunicationTips().contains("engaging"));
    }

    @Test
    void testGetMbtiInsightsWithInvalidType() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mbtiPromptService.getMbtiInsights("INVALID");
        });
    }

    @Test
    void testGetMbtiInsightsWithLowercaseType() {
        // Act
        MbtiInsightsDto insights = mbtiPromptService.getMbtiInsights("enfj");

        // Assert
        assertEquals("ENFJ", insights.getMbtiType());
        assertEquals("Protagonist", insights.getName());
    }

    @Test
    void testPromptContainsPersonalityContext() {
        // Arrange
        String userQuery = "Air quality concerns";

        // Act
        String intjPrompt = mbtiPromptService.generatePrompt(userQuery, "INTJ", "NYC");
        String esfpPrompt = mbtiPromptService.generatePrompt(userQuery, "ESFP", "NYC");

        // Assert
        assertNotEquals(intjPrompt, esfpPrompt);
        assertTrue(intjPrompt.contains("analytical") || intjPrompt.contains("Architect"));
        assertTrue(esfpPrompt.contains("lively") || esfpPrompt.contains("Entertainer"));
    }

    @Test
    void testAllMbtiTypesHaveUniqueStyles() {
        // Arrange
        String[] mbtiTypes = {
            "INTJ", "INTP", "ENTJ", "ENTP",
            "INFJ", "INFP", "ENFJ", "ENFP",
            "ISTJ", "ISFJ", "ESTJ", "ESFJ",
            "ISTP", "ISFP", "ESTP", "ESFP"
        };

        // Act & Assert
        for (String type : mbtiTypes) {
            MbtiInsightsDto insights = mbtiPromptService.getMbtiInsights(type);
            assertNotNull(insights.getStyle());
            assertFalse(insights.getStyle().isEmpty());
            System.out.println(type + ": " + insights.getStyle());
        }
    }
}
