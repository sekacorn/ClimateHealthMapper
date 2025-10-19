package com.climate.collaboration.service;

import com.climate.collaboration.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MbtiCollaborationService
 */
@ExtendWith(MockitoExtension.class)
class MbtiCollaborationServiceTest {

    @InjectMocks
    private MbtiCollaborationService mbtiCollaborationService;

    private WebSocketMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = WebSocketMessage.builder()
            .action("ZOOM")
            .userId("user-1")
            .userName("John Doe")
            .data(Map.of("level", 10))
            .build();
    }

    @Test
    void testEnhanceMessage_ENTJ_Success() {
        // Arrange
        testMessage.setMbtiType("ENTJ");

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNotNull(enhanced.getMbtiEnhancements());
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("leadershipMode"));
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("showStrategicView"));
        assertEquals("directive", enhanced.getMbtiEnhancements().get("collaborationStyle"));
    }

    @Test
    void testEnhanceMessage_INFJ_Success() {
        // Arrange
        testMessage.setMbtiType("INFJ");

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNotNull(enhanced.getMbtiEnhancements());
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("empathyMode"));
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("showHumanImpact"));
        assertEquals("supportive", enhanced.getMbtiEnhancements().get("collaborationStyle"));
    }

    @Test
    void testEnhanceMessage_ESTP_Success() {
        // Arrange
        testMessage.setMbtiType("ESTP");

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNotNull(enhanced.getMbtiEnhancements());
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("actionMode"));
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("showRealTimeUpdates"));
        assertEquals("dynamic", enhanced.getMbtiEnhancements().get("collaborationStyle"));
    }

    @Test
    void testEnhanceMessage_INTJ_Success() {
        // Arrange
        testMessage.setMbtiType("INTJ");

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNotNull(enhanced.getMbtiEnhancements());
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("structuredMode"));
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("showSystemPatterns"));
        assertEquals("analytical", enhanced.getMbtiEnhancements().get("collaborationStyle"));
    }

    @Test
    void testEnhanceMessage_ESFJ_Success() {
        // Arrange
        testMessage.setMbtiType("ESFJ");

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNotNull(enhanced.getMbtiEnhancements());
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("supportMode"));
        assertTrue((Boolean) enhanced.getMbtiEnhancements().get("showCommunityNeeds"));
        assertEquals("nurturing", enhanced.getMbtiEnhancements().get("collaborationStyle"));
    }

    @Test
    void testEnhanceMessage_AllTypes_Success() {
        // Test all 16 MBTI types
        String[] allTypes = {
            "ENTJ", "INTJ", "ENTP", "INTP",
            "ENFJ", "INFJ", "ENFP", "INFP",
            "ESTJ", "ISTJ", "ESFJ", "ISFJ",
            "ESTP", "ISTP", "ESFP", "ISFP"
        };

        for (String type : allTypes) {
            testMessage.setMbtiType(type);
            WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);
            assertNotNull(enhanced.getMbtiEnhancements(),
                "Enhancements should not be null for " + type);
            assertFalse(enhanced.getMbtiEnhancements().isEmpty(),
                "Enhancements should not be empty for " + type);
        }
    }

    @Test
    void testEnhanceMessage_NullMbtiType_ReturnsOriginal() {
        // Arrange
        testMessage.setMbtiType(null);

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNull(enhanced.getMbtiEnhancements());
    }

    @Test
    void testEnhanceMessage_EmptyMbtiType_ReturnsOriginal() {
        // Arrange
        testMessage.setMbtiType("");

        // Act
        WebSocketMessage enhanced = mbtiCollaborationService.enhanceMessage(testMessage);

        // Assert
        assertNull(enhanced.getMbtiEnhancements());
    }

    @Test
    void testGetTeamDynamicsInsight_Success() {
        // Arrange
        List<String> mbtiTypes = Arrays.asList("ENTJ", "INFJ", "ESTP", "ISTJ");

        // Act
        Map<String, Object> insights = mbtiCollaborationService.getTeamDynamicsInsight(mbtiTypes);

        // Assert
        assertNotNull(insights);
        assertTrue(insights.containsKey("typeDistribution"));
        assertTrue(insights.containsKey("strengthAreas"));
        assertTrue(insights.containsKey("potentialChallenges"));
        assertTrue(insights.containsKey("collaborationTips"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) insights.get("typeDistribution");
        assertEquals(1, distribution.get("ENTJ"));
        assertEquals(1, distribution.get("INFJ"));
    }

    @Test
    void testGetTeamDynamicsInsight_IdentifiesAnalyticalStrength() {
        // Arrange
        List<String> mbtiTypes = Arrays.asList("ENTJ", "INTJ", "ENTP");

        // Act
        Map<String, Object> insights = mbtiCollaborationService.getTeamDynamicsInsight(mbtiTypes);

        // Assert
        @SuppressWarnings("unchecked")
        List<String> strengths = (List<String>) insights.get("strengthAreas");
        assertTrue(strengths.stream()
            .anyMatch(s -> s.contains("analytical") || s.contains("strategic")));
    }

    @Test
    void testGetTeamDynamicsInsight_IdentifiesPeopleSkills() {
        // Arrange
        List<String> mbtiTypes = Arrays.asList("ENFJ", "INFJ", "ESFJ");

        // Act
        Map<String, Object> insights = mbtiCollaborationService.getTeamDynamicsInsight(mbtiTypes);

        // Assert
        @SuppressWarnings("unchecked")
        List<String> strengths = (List<String>) insights.get("strengthAreas");
        assertTrue(strengths.stream()
            .anyMatch(s -> s.contains("people") || s.contains("empathy")));
    }

    @Test
    void testGetTeamDynamicsInsight_IdentifiesIntrovertExtrovertMix() {
        // Arrange
        List<String> mbtiTypes = Arrays.asList("ENTJ", "INFJ");

        // Act
        Map<String, Object> insights = mbtiCollaborationService.getTeamDynamicsInsight(mbtiTypes);

        // Assert
        @SuppressWarnings("unchecked")
        List<String> challenges = (List<String>) insights.get("potentialChallenges");
        assertTrue(challenges.stream()
            .anyMatch(s -> s.contains("introverted") && s.contains("extroverted")));
    }
}
