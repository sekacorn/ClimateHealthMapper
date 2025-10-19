package com.climate.collaboration.service;

import com.climate.collaboration.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MBTI-tailored collaboration features service
 * Provides personality-specific enhancements for all 16 MBTI types
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MbtiCollaborationService {

    /**
     * Enhance message with MBTI-specific features
     */
    public WebSocketMessage enhanceMessage(WebSocketMessage message) {
        String mbtiType = message.getMbtiType();

        if (mbtiType == null || mbtiType.isEmpty()) {
            return message;
        }

        Map<String, Object> mbtiEnhancements = new HashMap<>();

        switch (mbtiType.toUpperCase()) {
            case "ENTJ": // The Commander
                mbtiEnhancements = getEntjEnhancements(message);
                break;
            case "INTJ": // The Architect
                mbtiEnhancements = getIntjEnhancements(message);
                break;
            case "ENTP": // The Debater
                mbtiEnhancements = getEntpEnhancements(message);
                break;
            case "INTP": // The Logician
                mbtiEnhancements = getIntpEnhancements(message);
                break;
            case "ENFJ": // The Protagonist
                mbtiEnhancements = getEnfjEnhancements(message);
                break;
            case "INFJ": // The Advocate
                mbtiEnhancements = getInfjEnhancements(message);
                break;
            case "ENFP": // The Campaigner
                mbtiEnhancements = getEnfpEnhancements(message);
                break;
            case "INFP": // The Mediator
                mbtiEnhancements = getInfpEnhancements(message);
                break;
            case "ESTJ": // The Executive
                mbtiEnhancements = getEstjEnhancements(message);
                break;
            case "ISTJ": // The Logistician
                mbtiEnhancements = getIstjEnhancements(message);
                break;
            case "ESFJ": // The Consul
                mbtiEnhancements = getEsfjEnhancements(message);
                break;
            case "ISFJ": // The Defender
                mbtiEnhancements = getIsfjEnhancements(message);
                break;
            case "ESTP": // The Entrepreneur
                mbtiEnhancements = getEstpEnhancements(message);
                break;
            case "ISTP": // The Virtuoso
                mbtiEnhancements = getIstpEnhancements(message);
                break;
            case "ESFP": // The Entertainer
                mbtiEnhancements = getEsfpEnhancements(message);
                break;
            case "ISFP": // The Adventurer
                mbtiEnhancements = getIsfpEnhancements(message);
                break;
            default:
                log.warn("Unknown MBTI type: {}", mbtiType);
        }

        message.setMbtiEnhancements(mbtiEnhancements);
        return message;
    }

    // ENTJ - The Commander: Strategic leadership
    private Map<String, Object> getEntjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("leadershipMode", true);
        enhancements.put("showStrategicView", true);
        enhancements.put("enableGoalTracking", true);
        enhancements.put("priorityHighlight", "high");
        enhancements.put("decisionSupport", Arrays.asList(
            "Strategic implication analysis",
            "Resource allocation optimizer",
            "Timeline projections"
        ));
        enhancements.put("collaborationStyle", "directive");
        enhancements.put("preferredView", "executive_dashboard");
        return enhancements;
    }

    // INTJ - The Architect: Systematic planning
    private Map<String, Object> getIntjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("structuredMode", true);
        enhancements.put("showSystemPatterns", true);
        enhancements.put("enableLongTermPlanning", true);
        enhancements.put("dataDepth", "comprehensive");
        enhancements.put("analysisTools", Arrays.asList(
            "Pattern recognition",
            "Predictive modeling",
            "System architecture view"
        ));
        enhancements.put("collaborationStyle", "analytical");
        enhancements.put("preferredView", "structural_overview");
        return enhancements;
    }

    // ENTP - The Debater: Innovation and exploration
    private Map<String, Object> getEntpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("innovationMode", true);
        enhancements.put("showAlternatives", true);
        enhancements.put("enableBrainstorming", true);
        enhancements.put("explorationDepth", "broad");
        enhancements.put("creativeTools", Arrays.asList(
            "What-if scenarios",
            "Alternative solutions",
            "Debate perspectives"
        ));
        enhancements.put("collaborationStyle", "challenging");
        enhancements.put("preferredView", "possibility_explorer");
        return enhancements;
    }

    // INTP - The Logician: Logical analysis
    private Map<String, Object> getIntpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("logicalMode", true);
        enhancements.put("showDataRelationships", true);
        enhancements.put("enableDeepAnalysis", true);
        enhancements.put("precisionLevel", "maximum");
        enhancements.put("analyticalTools", Arrays.asList(
            "Logical consistency checker",
            "Causal relationship mapper",
            "Theoretical frameworks"
        ));
        enhancements.put("collaborationStyle", "theoretical");
        enhancements.put("preferredView", "logical_framework");
        return enhancements;
    }

    // ENFJ - The Protagonist: People-focused leadership
    private Map<String, Object> getEnfjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("harmonyMode", true);
        enhancements.put("showTeamDynamics", true);
        enhancements.put("enableMentoring", true);
        enhancements.put("socialAwareness", "high");
        enhancements.put("peopleTools", Arrays.asList(
            "Team collaboration insights",
            "Consensus builder",
            "Impact on communities"
        ));
        enhancements.put("collaborationStyle", "inspiring");
        enhancements.put("preferredView", "community_impact");
        return enhancements;
    }

    // INFJ - The Advocate: Empathetic insights
    private Map<String, Object> getInfjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("empathyMode", true);
        enhancements.put("showHumanImpact", true);
        enhancements.put("enableVisionSharing", true);
        enhancements.put("insightDepth", "profound");
        enhancements.put("empathyTools", Arrays.asList(
            "Human impact visualization",
            "Long-term vision mapper",
            "Values alignment checker"
        ));
        enhancements.put("collaborationStyle", "supportive");
        enhancements.put("preferredView", "humanitarian_perspective");
        return enhancements;
    }

    // ENFP - The Campaigner: Enthusiastic exploration
    private Map<String, Object> getEnfpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("enthusiasmMode", true);
        enhancements.put("showOpportunities", true);
        enhancements.put("enableCreativeExploration", true);
        enhancements.put("energyLevel", "high");
        enhancements.put("inspirationTools", Arrays.asList(
            "Opportunity finder",
            "Connection mapper",
            "Inspiration board"
        ));
        enhancements.put("collaborationStyle", "energizing");
        enhancements.put("preferredView", "opportunities_landscape");
        return enhancements;
    }

    // INFP - The Mediator: Values-driven collaboration
    private Map<String, Object> getInfpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("valuesMode", true);
        enhancements.put("showMeaningfulImpact", true);
        enhancements.put("enableReflection", true);
        enhancements.put("authenticityLevel", "high");
        enhancements.put("meaningTools", Arrays.asList(
            "Values impact analysis",
            "Authenticity checker",
            "Personal meaning finder"
        ));
        enhancements.put("collaborationStyle", "harmonizing");
        enhancements.put("preferredView", "values_perspective");
        return enhancements;
    }

    // ESTJ - The Executive: Organized efficiency
    private Map<String, Object> getEstjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("organizationMode", true);
        enhancements.put("showProcessFlow", true);
        enhancements.put("enableTaskManagement", true);
        enhancements.put("efficiencyLevel", "maximum");
        enhancements.put("managementTools", Arrays.asList(
            "Task organizer",
            "Process optimizer",
            "Standards compliance"
        ));
        enhancements.put("collaborationStyle", "structured");
        enhancements.put("preferredView", "operational_dashboard");
        return enhancements;
    }

    // ISTJ - The Logistician: Reliable accuracy
    private Map<String, Object> getIstjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("reliabilityMode", true);
        enhancements.put("showDetailedData", true);
        enhancements.put("enableAccuracyChecks", true);
        enhancements.put("precisionLevel", "high");
        enhancements.put("reliabilityTools", Arrays.asList(
            "Data validation",
            "Historical comparison",
            "Accuracy metrics"
        ));
        enhancements.put("collaborationStyle", "methodical");
        enhancements.put("preferredView", "detailed_records");
        return enhancements;
    }

    // ESFJ - The Consul: Group harmony and support
    private Map<String, Object> getEsfjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("supportMode", true);
        enhancements.put("showCommunityNeeds", true);
        enhancements.put("enableGroupSupport", true);
        enhancements.put("careLevel", "high");
        enhancements.put("supportTools", Arrays.asList(
            "Community needs tracker",
            "Group harmony monitor",
            "Support coordinator"
        ));
        enhancements.put("collaborationStyle", "nurturing");
        enhancements.put("preferredView", "community_wellbeing");
        return enhancements;
    }

    // ISFJ - The Defender: Protective care
    private Map<String, Object> getIsfjEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("protectiveMode", true);
        enhancements.put("showVulnerabilities", true);
        enhancements.put("enableCareTracking", true);
        enhancements.put("attentionToDetail", "high");
        enhancements.put("careTools", Arrays.asList(
            "Vulnerability assessment",
            "Protection planner",
            "Care history"
        ));
        enhancements.put("collaborationStyle", "protective");
        enhancements.put("preferredView", "safety_assessment");
        return enhancements;
    }

    // ESTP - The Entrepreneur: Fast-paced action
    private Map<String, Object> getEstpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("actionMode", true);
        enhancements.put("showRealTimeUpdates", true);
        enhancements.put("enableQuickActions", true);
        enhancements.put("updateFrequency", "high");
        enhancements.put("actionTools", Arrays.asList(
            "Real-time alerts",
            "Quick decision support",
            "Action tracker"
        ));
        enhancements.put("collaborationStyle", "dynamic");
        enhancements.put("preferredView", "live_action_feed");
        return enhancements;
    }

    // ISTP - The Virtuoso: Practical problem-solving
    private Map<String, Object> getIstpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("practicalMode", true);
        enhancements.put("showMechanics", true);
        enhancements.put("enableTroubleshooting", true);
        enhancements.put("technicalDepth", "high");
        enhancements.put("technicalTools", Arrays.asList(
            "System diagnostics",
            "Problem solver",
            "Technical analysis"
        ));
        enhancements.put("collaborationStyle", "pragmatic");
        enhancements.put("preferredView", "technical_overview");
        return enhancements;
    }

    // ESFP - The Entertainer: Engaging experiences
    private Map<String, Object> getEsfpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("engagementMode", true);
        enhancements.put("showVisualAppeal", true);
        enhancements.put("enableSocialSharing", true);
        enhancements.put("interactivityLevel", "high");
        enhancements.put("engagementTools", Arrays.asList(
            "Visual storytelling",
            "Social sharing",
            "Interactive elements"
        ));
        enhancements.put("collaborationStyle", "spontaneous");
        enhancements.put("preferredView", "interactive_experience");
        return enhancements;
    }

    // ISFP - The Adventurer: Aesthetic appreciation
    private Map<String, Object> getIsfpEnhancements(WebSocketMessage message) {
        Map<String, Object> enhancements = new HashMap<>();
        enhancements.put("aestheticMode", true);
        enhancements.put("showVisualBeauty", true);
        enhancements.put("enableCreativeExpression", true);
        enhancements.put("aestheticLevel", "high");
        enhancements.put("creativeTools", Arrays.asList(
            "Visual customization",
            "Artistic perspectives",
            "Sensory experience"
        ));
        enhancements.put("collaborationStyle", "flexible");
        enhancements.put("preferredView", "aesthetic_display");
        return enhancements;
    }

    /**
     * Get collaboration recommendations based on MBTI mix
     */
    public Map<String, Object> getTeamDynamicsInsight(List<String> mbtiTypes) {
        Map<String, Object> insights = new HashMap<>();

        Map<String, Integer> typeCounts = new HashMap<>();
        for (String type : mbtiTypes) {
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        insights.put("typeDistribution", typeCounts);
        insights.put("strengthAreas", identifyTeamStrengths(mbtiTypes));
        insights.put("potentialChallenges", identifyTeamChallenges(mbtiTypes));
        insights.put("collaborationTips", getCollaborationTips(mbtiTypes));

        return insights;
    }

    private List<String> identifyTeamStrengths(List<String> mbtiTypes) {
        List<String> strengths = new ArrayList<>();

        long analyticalCount = mbtiTypes.stream()
            .filter(t -> t.contains("NT")).count();
        if (analyticalCount > 0) {
            strengths.add("Strong analytical and strategic thinking");
        }

        long peopleCount = mbtiTypes.stream()
            .filter(t -> t.contains("NF")).count();
        if (peopleCount > 0) {
            strengths.add("Excellent people skills and empathy");
        }

        long practicalCount = mbtiTypes.stream()
            .filter(t -> t.contains("SJ")).count();
        if (practicalCount > 0) {
            strengths.add("Reliable execution and attention to detail");
        }

        return strengths;
    }

    private List<String> identifyTeamChallenges(List<String> mbtiTypes) {
        List<String> challenges = new ArrayList<>();

        boolean hasIntroverts = mbtiTypes.stream().anyMatch(t -> t.startsWith("I"));
        boolean hasExtroverts = mbtiTypes.stream().anyMatch(t -> t.startsWith("E"));

        if (hasIntroverts && hasExtroverts) {
            challenges.add("Balance introverted reflection with extroverted action");
        }

        return challenges;
    }

    private List<String> getCollaborationTips(List<String> mbtiTypes) {
        List<String> tips = new ArrayList<>();

        tips.add("Leverage diverse perspectives for comprehensive solutions");
        tips.add("Ensure all voices are heard in decision-making");
        tips.add("Balance big-picture vision with practical implementation");

        return tips;
    }
}
