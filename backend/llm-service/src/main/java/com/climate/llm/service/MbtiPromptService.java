package com.climate.llm.service;

import com.climate.llm.dto.MbtiInsightsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating MBTI-tailored prompts and insights
 * Supports all 16 MBTI personality types with unique response styles
 */
@Service
@Slf4j
public class MbtiPromptService {

    private static final Map<String, MbtiProfile> MBTI_PROFILES = new HashMap<>();

    static {
        // Analysts
        MBTI_PROFILES.put("INTJ", new MbtiProfile(
            "Architect",
            "analytical, detailed",
            "Provide comprehensive, data-driven analysis with strategic insights. Focus on long-term implications and systematic solutions. Use precise terminology and include statistical evidence.",
            "Strategic thinkers who value independence and competence",
            "Detailed analysis, evidence-based recommendations, systematic approaches"
        ));

        MBTI_PROFILES.put("INTP", new MbtiProfile(
            "Logician",
            "logical, in-depth",
            "Offer thorough logical analysis with theoretical frameworks. Explore multiple perspectives and underlying mechanisms. Present information in a structured, analytical manner.",
            "Innovative problem-solvers who seek to understand systems",
            "Theoretical explanations, logical reasoning, comprehensive data"
        ));

        MBTI_PROFILES.put("ENTJ", new MbtiProfile(
            "Commander",
            "strategic, results-focused",
            "Deliver actionable strategies with clear goals and measurable outcomes. Emphasize efficiency and effectiveness. Present bold recommendations with implementation timelines.",
            "Natural leaders who focus on achieving objectives",
            "Strategic plans, measurable goals, decisive action items"
        ));

        MBTI_PROFILES.put("ENTP", new MbtiProfile(
            "Debater",
            "witty, exploratory",
            "Present innovative ideas with intellectual curiosity. Explore alternative solutions and challenge conventional thinking. Use engaging, thought-provoking language with creative angles.",
            "Innovative thinkers who enjoy intellectual challenges",
            "Novel approaches, multiple perspectives, creative solutions"
        ));

        // Diplomats
        MBTI_PROFILES.put("INFJ", new MbtiProfile(
            "Advocate",
            "empathetic, holistic",
            "Provide compassionate, meaningful insights that consider human impact. Connect environmental issues to personal values and community wellbeing. Use inspiring, purposeful language.",
            "Idealistic visionaries who seek meaning and connection",
            "Holistic perspectives, values-based guidance, long-term vision"
        ));

        MBTI_PROFILES.put("INFP", new MbtiProfile(
            "Mediator",
            "creative, value-driven",
            "Offer authentic, values-aligned recommendations that honor individual expression. Emphasize personal meaning and ethical considerations. Use gentle, encouraging language.",
            "Idealistic individuals who value authenticity and harmony",
            "Value-based insights, personal meaning, gentle encouragement"
        ));

        MBTI_PROFILES.put("ENFJ", new MbtiProfile(
            "Protagonist",
            "inspirational, visionary",
            "Inspire positive action through motivational messaging. Emphasize collective impact and community engagement. Use uplifting, encouraging language that empowers.",
            "Charismatic leaders who inspire and organize others",
            "Inspirational messages, community solutions, collective action"
        ));

        MBTI_PROFILES.put("ENFP", new MbtiProfile(
            "Campaigner",
            "creative, enthusiastic",
            "Share exciting possibilities with enthusiastic energy. Explore creative solutions and opportunities for positive change. Use vibrant, optimistic language with imaginative ideas.",
            "Enthusiastic innovators who see potential everywhere",
            "Creative possibilities, enthusiastic encouragement, innovative ideas"
        ));

        // Sentinels
        MBTI_PROFILES.put("ISTJ", new MbtiProfile(
            "Logistician",
            "structured, precise",
            "Provide clear, factual information with proven methods. Emphasize reliability and established protocols. Use organized, step-by-step guidance with concrete details.",
            "Practical and fact-minded individuals who value order",
            "Structured information, proven methods, clear procedures"
        ));

        MBTI_PROFILES.put("ISFJ", new MbtiProfile(
            "Defender",
            "nurturing, practical",
            "Offer caring, supportive guidance with practical applications. Focus on protecting health and wellbeing. Use warm, reassuring language with actionable steps.",
            "Dedicated protectors who value tradition and loyalty",
            "Practical care, protective measures, supportive guidance"
        ));

        MBTI_PROFILES.put("ESTJ", new MbtiProfile(
            "Executive",
            "direct, results-driven",
            "Deliver efficient, no-nonsense recommendations with clear directives. Focus on practical implementation and measurable results. Use straightforward, authoritative language.",
            "Organized administrators who value efficiency",
            "Direct guidance, efficient solutions, clear action plans"
        ));

        MBTI_PROFILES.put("ESFJ", new MbtiProfile(
            "Consul",
            "supportive, community-focused",
            "Provide helpful, community-oriented advice that brings people together. Emphasize social responsibility and collective wellbeing. Use warm, considerate language.",
            "Caring individuals who value harmony and cooperation",
            "Community solutions, social support, collaborative approaches"
        ));

        // Explorers
        MBTI_PROFILES.put("ISTP", new MbtiProfile(
            "Virtuoso",
            "concise, problem-solving",
            "Offer practical, hands-on solutions with technical precision. Focus on immediate fixes and efficient troubleshooting. Use brief, direct language with actionable tips.",
            "Bold and practical experimenters who master tools",
            "Practical solutions, technical details, hands-on approaches"
        ));

        MBTI_PROFILES.put("ISFP", new MbtiProfile(
            "Adventurer",
            "gentle, encouraging",
            "Share gentle, aesthetically-pleasing information with personal touch. Emphasize harmony with nature and experiential learning. Use kind, supportive language.",
            "Flexible artists who value freedom and aesthetics",
            "Gentle guidance, experiential insights, harmonious solutions"
        ));

        MBTI_PROFILES.put("ESTP", new MbtiProfile(
            "Entrepreneur",
            "actionable, quick",
            "Deliver fast, action-oriented advice with immediate impact. Focus on practical, real-world applications. Use energetic, straightforward language with quick wins.",
            "Energetic individuals who live in the moment",
            "Quick actions, immediate results, practical tips"
        ));

        MBTI_PROFILES.put("ESFP", new MbtiProfile(
            "Entertainer",
            "lively, action-oriented",
            "Present engaging, fun information that encourages participation. Make climate health approachable and exciting. Use enthusiastic, positive language with interactive elements.",
            "Spontaneous entertainers who enjoy life's pleasures",
            "Engaging content, fun activities, positive reinforcement"
        ));
    }

    /**
     * Generate MBTI-tailored prompt
     */
    public String generatePrompt(String userQuery, String mbtiType, String location) {
        MbtiProfile profile = MBTI_PROFILES.get(mbtiType.toUpperCase());

        if (profile == null) {
            log.warn("Unknown MBTI type: {}, using default prompt", mbtiType);
            return buildDefaultPrompt(userQuery, location);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a climate health assistant tailored for ").append(profile.name)
              .append(" personality types. ");
        prompt.append(profile.instructions).append("\n\n");

        prompt.append("User Query: ").append(userQuery).append("\n");

        if (location != null && !location.isEmpty()) {
            prompt.append("Location: ").append(location).append("\n");
        }

        prompt.append("\nProvide a response that is ").append(profile.style)
              .append(", considering the user's personality preferences: ")
              .append(profile.preferences).append(".");

        return prompt.toString();
    }

    /**
     * Build default prompt for unknown MBTI types
     */
    private String buildDefaultPrompt(String userQuery, String location) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful climate health assistant. ");
        prompt.append("Provide clear, accurate information about climate health risks and recommendations.\n\n");
        prompt.append("User Query: ").append(userQuery).append("\n");

        if (location != null && !location.isEmpty()) {
            prompt.append("Location: ").append(location).append("\n");
        }

        return prompt.toString();
    }

    /**
     * Get MBTI insights
     */
    public MbtiInsightsDto getMbtiInsights(String mbtiType) {
        MbtiProfile profile = MBTI_PROFILES.get(mbtiType.toUpperCase());

        if (profile == null) {
            throw new IllegalArgumentException("Invalid MBTI type: " + mbtiType);
        }

        return MbtiInsightsDto.builder()
            .mbtiType(mbtiType.toUpperCase())
            .name(profile.name)
            .style(profile.style)
            .characteristics(profile.characteristics)
            .preferences(profile.preferences)
            .communicationTips(getCommunicationTips(mbtiType.toUpperCase()))
            .build();
    }

    /**
     * Get communication tips for MBTI type
     */
    private String getCommunicationTips(String mbtiType) {
        return switch (mbtiType) {
            case "INTJ", "INTP" -> "Provide detailed data and logical reasoning. Avoid overly emotional appeals.";
            case "ENTJ", "ESTJ" -> "Be direct and results-oriented. Focus on efficiency and practical outcomes.";
            case "ENTP" -> "Engage with creative ideas and intellectual debate. Encourage exploration.";
            case "INFJ", "INFP" -> "Connect to values and meaning. Use compassionate, authentic language.";
            case "ENFJ", "ENFP" -> "Inspire with vision and possibilities. Emphasize positive impact.";
            case "ISTJ", "ISFJ" -> "Provide clear, structured information. Emphasize reliability and safety.";
            case "ESFJ" -> "Focus on community and social harmony. Use warm, supportive tone.";
            case "ISTP" -> "Keep it practical and brief. Focus on technical solutions.";
            case "ISFP" -> "Be gentle and encouraging. Connect to personal experience.";
            case "ESTP", "ESFP" -> "Make it engaging and action-oriented. Focus on immediate, fun activities.";
            default -> "Communicate clearly and respectfully, adapting to user preferences.";
        };
    }

    /**
     * Internal class to hold MBTI profile data
     */
    private static class MbtiProfile {
        String name;
        String style;
        String instructions;
        String characteristics;
        String preferences;

        MbtiProfile(String name, String style, String instructions,
                   String characteristics, String preferences) {
            this.name = name;
            this.style = style;
            this.instructions = instructions;
            this.characteristics = characteristics;
            this.preferences = preferences;
        }
    }
}
