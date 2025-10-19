package com.climate.visualizer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for applying MBTI-specific styling to visualizations
 *
 * Provides personalized color schemes, layouts, and visual preferences
 * for all 16 MBTI personality types.
 */
@Service
@Slf4j
public class MbtiStyleService {

    private static final Set<String> VALID_MBTI_TYPES = Set.of(
        "INTJ", "INTP", "ENTJ", "ENTP",
        "INFJ", "INFP", "ENFJ", "ENFP",
        "ISTJ", "ISFJ", "ESTJ", "ESFJ",
        "ISTP", "ISFP", "ESTP", "ESFP"
    );

    /**
     * Validate MBTI type
     */
    public boolean isValidMbtiType(String mbtiType) {
        return mbtiType != null && VALID_MBTI_TYPES.contains(mbtiType.toUpperCase());
    }

    /**
     * Generate climate visualization materials based on MBTI type
     */
    public Map<String, Object> generateClimateMaterials(String mbtiType, List<?> dataPoints) {
        if (mbtiType == null || mbtiType.isEmpty()) {
            mbtiType = "INTJ"; // Default
        }

        log.info("Generating climate materials for MBTI type: {}", mbtiType);

        MbtiStyle style = getMbtiStyle(mbtiType.toUpperCase());

        List<Map<String, Object>> materials = new ArrayList<>();

        Map<String, Object> material = new HashMap<>();
        material.put("type", style.materialType);
        material.put("color", style.primaryColor);
        material.put("emissive", style.emissiveColor);
        material.put("opacity", style.opacity);
        material.put("wireframe", style.wireframe);
        material.put("metalness", style.metalness);
        material.put("roughness", style.roughness);

        materials.add(material);

        return Map.of(
            "materials", materials,
            "ambientColor", style.ambientColor,
            "backgroundColor", style.backgroundColor,
            "style", mbtiType
        );
    }

    /**
     * Generate health heatmap colors based on MBTI type
     */
    public Map<String, Object> generateHealthHeatmapColors(String mbtiType, List<?> riskPoints) {
        if (mbtiType == null || mbtiType.isEmpty()) {
            mbtiType = "INTJ"; // Default
        }

        log.info("Generating heatmap colors for MBTI type: {}", mbtiType);

        MbtiStyle style = getMbtiStyle(mbtiType.toUpperCase());

        List<String> gradient = style.heatmapGradient;

        return Map.of(
            "gradient", gradient,
            "interpolation", style.interpolationType,
            "contrast", style.contrast,
            "saturation", style.saturation,
            "style", mbtiType
        );
    }

    /**
     * Get MBTI-specific style configuration
     */
    private MbtiStyle getMbtiStyle(String mbtiType) {
        return switch (mbtiType) {
            // Analysts
            case "INTJ" -> MbtiStyle.builder()
                .materialType("MeshStandardMaterial")
                .primaryColor(0x2C3E50) // Deep blue-gray
                .emissiveColor(0x3498DB) // Bright blue
                .ambientColor(0x34495E)
                .backgroundColor(0x1A1A2E)
                .opacity(0.95)
                .wireframe(false)
                .metalness(0.3)
                .roughness(0.7)
                .heatmapGradient(Arrays.asList("#2C3E50", "#3498DB", "#9B59B6", "#E74C3C"))
                .interpolationType("linear")
                .contrast(0.8)
                .saturation(0.7)
                .build();

            case "INTP" -> MbtiStyle.builder()
                .materialType("MeshStandardMaterial")
                .primaryColor(0x16A085) // Teal
                .emissiveColor(0x1ABC9C)
                .ambientColor(0x0E6655)
                .backgroundColor(0x0B3D3D)
                .opacity(0.9)
                .wireframe(true)
                .metalness(0.2)
                .roughness(0.8)
                .heatmapGradient(Arrays.asList("#16A085", "#1ABC9C", "#F39C12", "#E67E22"))
                .interpolationType("cubic")
                .contrast(0.9)
                .saturation(0.6)
                .build();

            case "ENTJ" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0xC0392B) // Bold red
                .emissiveColor(0xE74C3C)
                .ambientColor(0x922B21)
                .backgroundColor(0x1C1C1C)
                .opacity(1.0)
                .wireframe(false)
                .metalness(0.6)
                .roughness(0.4)
                .heatmapGradient(Arrays.asList("#C0392B", "#E74C3C", "#F39C12", "#F1C40F"))
                .interpolationType("linear")
                .contrast(1.0)
                .saturation(0.9)
                .build();

            case "ENTP" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0x8E44AD) // Purple
                .emissiveColor(0x9B59B6)
                .ambientColor(0x6C3483)
                .backgroundColor(0x1A1A2E)
                .opacity(0.95)
                .wireframe(false)
                .metalness(0.4)
                .roughness(0.6)
                .heatmapGradient(Arrays.asList("#8E44AD", "#9B59B6", "#3498DB", "#1ABC9C"))
                .interpolationType("cubic")
                .contrast(0.85)
                .saturation(0.8)
                .build();

            // Diplomats
            case "INFJ" -> MbtiStyle.builder()
                .materialType("MeshLambertMaterial")
                .primaryColor(0x5D6D7E) // Soft gray-blue
                .emissiveColor(0x85929E)
                .ambientColor(0x34495E)
                .backgroundColor(0x212F3D)
                .opacity(0.85)
                .wireframe(false)
                .metalness(0.2)
                .roughness(0.9)
                .heatmapGradient(Arrays.asList("#5D6D7E", "#85929E", "#AED6F1", "#D4E6F1"))
                .interpolationType("smooth")
                .contrast(0.6)
                .saturation(0.5)
                .build();

            case "INFP" -> MbtiStyle.builder()
                .materialType("MeshLambertMaterial")
                .primaryColor(0xAF7AC5) // Lavender
                .emissiveColor(0xD7BDE2)
                .ambientColor(0x7D3C98)
                .backgroundColor(0x2C2C54)
                .opacity(0.8)
                .wireframe(false)
                .metalness(0.1)
                .roughness(0.95)
                .heatmapGradient(Arrays.asList("#AF7AC5", "#D7BDE2", "#F8BBD0", "#FCE4EC"))
                .interpolationType("smooth")
                .contrast(0.5)
                .saturation(0.6)
                .build();

            case "ENFJ" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0x27AE60) // Warm green
                .emissiveColor(0x2ECC71)
                .ambientColor(0x1E8449)
                .backgroundColor(0x0E4D2C)
                .opacity(0.9)
                .wireframe(false)
                .metalness(0.3)
                .roughness(0.6)
                .heatmapGradient(Arrays.asList("#27AE60", "#2ECC71", "#F39C12", "#E67E22"))
                .interpolationType("linear")
                .contrast(0.75)
                .saturation(0.8)
                .build();

            case "ENFP" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0xF39C12) // Vibrant orange
                .emissiveColor(0xF1C40F)
                .ambientColor(0xD68910)
                .backgroundColor(0x1C2833)
                .opacity(0.9)
                .wireframe(false)
                .metalness(0.3)
                .roughness(0.5)
                .heatmapGradient(Arrays.asList("#F39C12", "#F1C40F", "#E74C3C", "#E67E22"))
                .interpolationType("linear")
                .contrast(0.9)
                .saturation(0.95)
                .build();

            // Sentinels
            case "ISTJ" -> MbtiStyle.builder()
                .materialType("MeshStandardMaterial")
                .primaryColor(0x2E4053) // Navy blue
                .emissiveColor(0x34495E)
                .ambientColor(0x1B2631)
                .backgroundColor(0x0B0C10)
                .opacity(1.0)
                .wireframe(false)
                .metalness(0.5)
                .roughness(0.8)
                .heatmapGradient(Arrays.asList("#2E4053", "#34495E", "#5D6D7E", "#85929E"))
                .interpolationType("linear")
                .contrast(0.7)
                .saturation(0.4)
                .build();

            case "ISFJ" -> MbtiStyle.builder()
                .materialType("MeshLambertMaterial")
                .primaryColor(0x7D6608) // Warm brown
                .emissiveColor(0xB7950B)
                .ambientColor(0x5B4B00)
                .backgroundColor(0x1C1C1C)
                .opacity(0.9)
                .wireframe(false)
                .metalness(0.2)
                .roughness(0.9)
                .heatmapGradient(Arrays.asList("#7D6608", "#B7950B", "#F39C12", "#F8C471"))
                .interpolationType("smooth")
                .contrast(0.6)
                .saturation(0.6)
                .build();

            case "ESTJ" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0x78281F) // Dark red
                .emissiveColor(0xA93226)
                .ambientColor(0x641E16)
                .backgroundColor(0x1C1C1C)
                .opacity(1.0)
                .wireframe(false)
                .metalness(0.6)
                .roughness(0.5)
                .heatmapGradient(Arrays.asList("#78281F", "#A93226", "#E74C3C", "#EC7063"))
                .interpolationType("linear")
                .contrast(0.85)
                .saturation(0.8)
                .build();

            case "ESFJ" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0xD35400) // Burnt orange
                .emissiveColor(0xE67E22)
                .ambientColor(0xA04000)
                .backgroundColor(0x1A1A2E)
                .opacity(0.95)
                .wireframe(false)
                .metalness(0.3)
                .roughness(0.6)
                .heatmapGradient(Arrays.asList("#D35400", "#E67E22", "#F39C12", "#F8C471"))
                .interpolationType("linear")
                .contrast(0.8)
                .saturation(0.85)
                .build();

            // Explorers
            case "ISTP" -> MbtiStyle.builder()
                .materialType("MeshStandardMaterial")
                .primaryColor(0x424949) // Cool gray
                .emissiveColor(0x616A6B)
                .ambientColor(0x273746)
                .backgroundColor(0x17202A)
                .opacity(0.95)
                .wireframe(true)
                .metalness(0.7)
                .roughness(0.5)
                .heatmapGradient(Arrays.asList("#424949", "#616A6B", "#839192", "#ABB2B9"))
                .interpolationType("linear")
                .contrast(0.75)
                .saturation(0.3)
                .build();

            case "ISFP" -> MbtiStyle.builder()
                .materialType("MeshLambertMaterial")
                .primaryColor(0xAD6495) // Rose
                .emissiveColor(0xD7A4C5)
                .ambientColor(0x7E4970)
                .backgroundColor(0x2C1B2E)
                .opacity(0.85)
                .wireframe(false)
                .metalness(0.2)
                .roughness(0.9)
                .heatmapGradient(Arrays.asList("#AD6495", "#D7A4C5", "#F5B7DC", "#FDE2F3"))
                .interpolationType("smooth")
                .contrast(0.65)
                .saturation(0.7)
                .build();

            case "ESTP" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0xE74C3C) // Bright red
                .emissiveColor(0xEC7063)
                .ambientColor(0xC0392B)
                .backgroundColor(0x0F0F0F)
                .opacity(1.0)
                .wireframe(false)
                .metalness(0.5)
                .roughness(0.4)
                .heatmapGradient(Arrays.asList("#E74C3C", "#EC7063", "#F39C12", "#F1C40F"))
                .interpolationType("linear")
                .contrast(0.95)
                .saturation(1.0)
                .build();

            case "ESFP" -> MbtiStyle.builder()
                .materialType("MeshPhongMaterial")
                .primaryColor(0xF1C40F) // Bright yellow
                .emissiveColor(0xF4D03F)
                .ambientColor(0xD4AC0D)
                .backgroundColor(0x1C1C1C)
                .opacity(0.95)
                .wireframe(false)
                .metalness(0.3)
                .roughness(0.5)
                .heatmapGradient(Arrays.asList("#F1C40F", "#F4D03F", "#F39C12", "#E67E22"))
                .interpolationType("linear")
                .contrast(0.9)
                .saturation(1.0)
                .build();

            default -> getMbtiStyle("INTJ"); // Default fallback
        };
    }

    /**
     * MBTI Style Configuration
     */
    @lombok.Data
    @lombok.Builder
    private static class MbtiStyle {
        private String materialType;
        private int primaryColor;
        private int emissiveColor;
        private int ambientColor;
        private int backgroundColor;
        private double opacity;
        private boolean wireframe;
        private double metalness;
        private double roughness;
        private List<String> heatmapGradient;
        private String interpolationType;
        private double contrast;
        private double saturation;
    }
}
