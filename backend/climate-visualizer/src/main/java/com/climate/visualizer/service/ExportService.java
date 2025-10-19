package com.climate.visualizer.service;

import com.climate.visualizer.model.Visualization;
import com.climate.visualizer.repository.VisualizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for exporting visualizations to various formats
 *
 * Supports PNG, SVG, and STL export formats with
 * configurable quality and dimensions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final VisualizationRepository visualizationRepository;
    private final ObjectMapper objectMapper;

    @Value("${export.output.directory:./exports}")
    private String exportDirectory;

    /**
     * Export visualization to specified format
     */
    public Map<String, Object> exportVisualization(
            Long visualizationId,
            String format,
            Map<String, Object> options) {

        try {
            log.info("Exporting visualization {} to format: {}", visualizationId, format);

            // Fetch visualization
            Visualization visualization = visualizationRepository.findById(visualizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Visualization not found with ID: " + visualizationId));

            // Create export directory if not exists
            File exportDir = new File(exportDirectory);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Export based on format
            String filePath = switch (format.toUpperCase()) {
                case "PNG" -> exportToPNG(visualization, options);
                case "SVG" -> exportToSVG(visualization, options);
                case "STL" -> exportToSTL(visualization, options);
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            };

            // Update visualization with export path
            updateExportMetadata(visualization, format, filePath);

            log.info("Visualization exported successfully to: {}", filePath);

            return Map.of(
                "success", true,
                "format", format,
                "filePath", filePath,
                "fileSize", new File(filePath).length(),
                "exportedAt", LocalDateTime.now().toString()
            );

        } catch (Exception e) {
            log.error("Error exporting visualization", e);
            throw new RuntimeException("Failed to export visualization: " + e.getMessage(), e);
        }
    }

    /**
     * Export to PNG format
     */
    private String exportToPNG(Visualization visualization, Map<String, Object> options) {
        try {
            int width = (int) options.getOrDefault("width", 1920);
            int height = (int) options.getOrDefault("height", 1080);

            // Create image
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Render visualization data
            renderVisualizationToGraphics(visualization, g2d, width, height);

            g2d.dispose();

            // Save to file
            String filename = generateFilename(visualization, "png");
            String filePath = exportDirectory + File.separator + filename;
            ImageIO.write(image, "PNG", new File(filePath));

            log.info("PNG export completed: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error exporting to PNG", e);
            throw new RuntimeException("PNG export failed", e);
        }
    }

    /**
     * Export to SVG format
     */
    private String exportToSVG(Visualization visualization, Map<String, Object> options) {
        try {
            int width = (int) options.getOrDefault("width", 1920);
            int height = (int) options.getOrDefault("height", 1080);

            StringBuilder svg = new StringBuilder();
            svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            svg.append(String.format("<svg width=\"%d\" height=\"%d\" ", width, height));
            svg.append("xmlns=\"http://www.w3.org/2000/svg\" ");
            svg.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");

            // Add metadata
            svg.append("<metadata>\n");
            svg.append(String.format("  <title>%s</title>\n", visualization.getTitle()));
            svg.append(String.format("  <description>Generated by ClimateHealthMapper</description>\n"));
            svg.append("</metadata>\n");

            // Render visualization data to SVG
            svg.append(renderVisualizationToSVG(visualization, width, height));

            svg.append("</svg>");

            // Save to file
            String filename = generateFilename(visualization, "svg");
            String filePath = exportDirectory + File.separator + filename;
            FileUtils.writeStringToFile(new File(filePath), svg.toString(), StandardCharsets.UTF_8);

            log.info("SVG export completed: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error exporting to SVG", e);
            throw new RuntimeException("SVG export failed", e);
        }
    }

    /**
     * Export to STL format (3D printing)
     */
    private String exportToSTL(Visualization visualization, Map<String, Object> options) {
        try {
            // STL is for 3D models only
            if (visualization.getVisualizationType() != Visualization.VisualizationType.CLIMATE_MAP_3D) {
                throw new IllegalArgumentException("STL export only supported for 3D climate maps");
            }

            StringBuilder stl = new StringBuilder();
            stl.append("solid ClimateMap\n");

            // Parse visualization data
            Map<String, Object> vizData = objectMapper.readValue(
                visualization.getVisualizationData(), Map.class);

            // Extract geometry and generate STL facets
            stl.append(generateSTLFacets(vizData));

            stl.append("endsolid ClimateMap\n");

            // Save to file
            String filename = generateFilename(visualization, "stl");
            String filePath = exportDirectory + File.separator + filename;
            FileUtils.writeStringToFile(new File(filePath), stl.toString(), StandardCharsets.UTF_8);

            log.info("STL export completed: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error exporting to STL", e);
            throw new RuntimeException("STL export failed", e);
        }
    }

    private void renderVisualizationToGraphics(
            Visualization visualization,
            Graphics2D g2d,
            int width,
            int height) {

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        try {
            Map<String, Object> vizData = objectMapper.readValue(
                visualization.getVisualizationData(), Map.class);

            // Render based on visualization type
            if (visualization.getVisualizationType() == Visualization.VisualizationType.HEALTH_HEATMAP) {
                renderHeatmap(vizData, g2d, width, height);
            } else {
                render3DProjection(vizData, g2d, width, height);
            }

            // Add title
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString(visualization.getTitle(), 20, 40);

        } catch (Exception e) {
            log.error("Error rendering visualization", e);
            g2d.setColor(Color.RED);
            g2d.drawString("Error rendering visualization", 20, height / 2);
        }
    }

    private void renderHeatmap(Map<String, Object> vizData, Graphics2D g2d, int width, int height) {
        // Simplified heatmap rendering
        Map<String, Object> grid = (Map<String, Object>) vizData.get("grid");
        if (grid == null) return;

        double[][] data = (double[][]) grid.get("grid");
        int gridWidth = (int) grid.get("width");
        int gridHeight = (int) grid.get("height");

        int cellWidth = width / gridWidth;
        int cellHeight = height / gridHeight;

        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                double value = data[i][j];
                Color color = getHeatmapColor(value);
                g2d.setColor(color);
                g2d.fillRect(j * cellWidth, i * cellHeight, cellWidth, cellHeight);
            }
        }
    }

    private void render3DProjection(Map<String, Object> vizData, Graphics2D g2d, int width, int height) {
        // Simplified 3D projection rendering
        g2d.setColor(new Color(100, 150, 200));
        g2d.fillRect(width / 4, height / 4, width / 2, height / 2);

        g2d.setColor(Color.BLACK);
        g2d.drawString("3D Climate Visualization", width / 2 - 100, height / 2);
    }

    private String renderVisualizationToSVG(Visualization visualization, int width, int height) {
        StringBuilder svg = new StringBuilder();

        try {
            Map<String, Object> vizData = objectMapper.readValue(
                visualization.getVisualizationData(), Map.class);

            // Background
            svg.append(String.format("<rect width=\"%d\" height=\"%d\" fill=\"white\"/>\n", width, height));

            // Render based on type
            if (visualization.getVisualizationType() == Visualization.VisualizationType.HEALTH_HEATMAP) {
                svg.append(renderHeatmapSVG(vizData, width, height));
            } else {
                svg.append(render3DProjectionSVG(vizData, width, height));
            }

            // Title
            svg.append(String.format("<text x=\"20\" y=\"40\" font-family=\"Arial\" font-size=\"24\" fill=\"black\">%s</text>\n",
                visualization.getTitle()));

        } catch (Exception e) {
            log.error("Error rendering SVG", e);
        }

        return svg.toString();
    }

    private String renderHeatmapSVG(Map<String, Object> vizData, int width, int height) {
        StringBuilder svg = new StringBuilder();

        Map<String, Object> grid = (Map<String, Object>) vizData.get("grid");
        if (grid != null) {
            // Simplified heatmap SVG rendering
            svg.append("<g id=\"heatmap\">\n");
            svg.append("  <rect x=\"50\" y=\"50\" width=\"").append(width - 100)
               .append("\" height=\"").append(height - 100)
               .append("\" fill=\"url(#heatGradient)\"/>\n");
            svg.append("</g>\n");
        }

        return svg.toString();
    }

    private String render3DProjectionSVG(Map<String, Object> vizData, int width, int height) {
        StringBuilder svg = new StringBuilder();

        svg.append("<g id=\"climate3d\">\n");
        svg.append(String.format("  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"#6496C8\"/>\n",
            width / 4, height / 4, width / 2, height / 2));
        svg.append("</g>\n");

        return svg.toString();
    }

    private String generateSTLFacets(Map<String, Object> vizData) {
        StringBuilder stl = new StringBuilder();

        // Extract geometries
        Object geometriesObj = vizData.get("geometries");
        if (geometriesObj instanceof java.util.List) {
            java.util.List geometries = (java.util.List) geometriesObj;
            if (!geometries.isEmpty()) {
                Map<String, Object> geometry = (Map<String, Object>) geometries.get(0);

                // Generate facets from vertices and faces
                // Simplified STL generation
                stl.append("  facet normal 0 0 1\n");
                stl.append("    outer loop\n");
                stl.append("      vertex 0 0 0\n");
                stl.append("      vertex 1 0 0\n");
                stl.append("      vertex 0 1 0\n");
                stl.append("    endloop\n");
                stl.append("  endfacet\n");
            }
        }

        return stl.toString();
    }

    private Color getHeatmapColor(double value) {
        // Map value (0-100) to color gradient (blue to red)
        int r = (int) (255 * Math.min(value / 50.0, 1.0));
        int b = (int) (255 * Math.max(1.0 - value / 50.0, 0.0));
        int g = 0;

        return new Color(r, g, b);
    }

    private String generateFilename(Visualization visualization, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedTitle = visualization.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
        return String.format("%s_%d_%s.%s", sanitizedTitle, visualization.getId(), timestamp, extension);
    }

    private void updateExportMetadata(Visualization visualization, String format, String filePath) {
        Map<String, String> exportPaths = visualization.getExportPaths();
        if (exportPaths == null) {
            exportPaths = new HashMap<>();
        }
        exportPaths.put(format, filePath);
        visualization.setExportPaths(exportPaths);
        visualization.setLastExportedAt(LocalDateTime.now());

        visualizationRepository.save(visualization);
    }
}
