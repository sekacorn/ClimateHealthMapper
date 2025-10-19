package com.climate.integrator.service;

import com.climate.integrator.model.GenomicData;
import com.climate.integrator.repository.GenomicDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing VCF genomic data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GenomicDataService {

    private final GenomicDataRepository genomicDataRepository;

    // Climate-relevant genes (example list)
    private static final Map<String, String> CLIMATE_RELEVANT_GENES = Map.of(
        "HSP70", "heat-stress-response",
        "HSP90", "heat-stress-response",
        "GPX1", "oxidative-stress-response",
        "SOD2", "oxidative-stress-response",
        "NOS3", "cardiovascular-adaptation",
        "ACE", "cardiovascular-adaptation",
        "GSTP1", "air-quality-response",
        "GSTM1", "air-quality-response"
    );

    /**
     * Process and store VCF genomic data
     */
    @Transactional
    public List<GenomicData> processVcfData(MultipartFile file, String userId) throws IOException {
        log.info("Processing VCF data for user: {}", userId);

        List<GenomicData> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            int lineNumber = 0;
            int variantCount = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header lines
                if (line.startsWith("#")) {
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    GenomicData genomicData = parseVcfLine(line, userId);
                    dataList.add(genomicData);
                    variantCount++;

                    // Log progress every 1000 variants
                    if (variantCount % 1000 == 0) {
                        log.info("Processed {} variants", variantCount);
                    }

                } catch (Exception e) {
                    log.error("Error parsing VCF line {}: {}", lineNumber, e.getMessage());
                    // Continue processing other lines
                }
            }

            log.info("Parsed {} variants from VCF file", variantCount);
        }

        // Save all records in batches
        List<GenomicData> savedData = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, dataList.size());
            List<GenomicData> batch = dataList.subList(i, endIndex);
            savedData.addAll(genomicDataRepository.saveAll(batch));
            log.info("Saved batch: {} - {} of {}", i, endIndex, dataList.size());
        }

        log.info("Successfully saved {} genomic data records", savedData.size());
        return savedData;
    }

    /**
     * Parse a single VCF line to GenomicData entity
     */
    private GenomicData parseVcfLine(String line, String userId) {
        String[] fields = line.split("\t");

        if (fields.length < 8) {
            throw new IllegalArgumentException("Invalid VCF line: insufficient fields");
        }

        // Standard VCF fields
        String chromosome = normalizeChromosome(fields[0]);
        Long position = Long.parseLong(fields[1]);
        String id = fields[2].equals(".") ? null : fields[2];
        String ref = fields[3];
        String alt = fields[4];
        String qualStr = fields[5];
        String filter = fields[6];
        String info = fields[7];

        // Parse QUAL
        Double quality = null;
        if (!qualStr.equals(".")) {
            try {
                quality = Double.parseDouble(qualStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid QUAL value: {}", qualStr);
            }
        }

        // Parse INFO field
        Map<String, String> infoMap = parseInfoField(info);

        // Extract genotype if FORMAT and sample columns exist
        String genotype = null;
        Integer genotypeQuality = null;
        Integer readDepth = null;

        if (fields.length >= 10) {
            String format = fields[8];
            String sample = fields[9];
            Map<String, String> genotypeMap = parseGenotypeField(format, sample);

            genotype = genotypeMap.get("GT");
            if (genotypeMap.containsKey("GQ")) {
                try {
                    genotypeQuality = Integer.parseInt(genotypeMap.get("GQ"));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            if (genotypeMap.containsKey("DP")) {
                try {
                    readDepth = Integer.parseInt(genotypeMap.get("DP"));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        // Extract annotation from INFO
        String geneName = infoMap.get("GENE");
        String geneId = infoMap.get("GENE_ID");
        String transcriptId = infoMap.get("TRANSCRIPT_ID");
        String variantType = infoMap.get("VARIANT_TYPE");
        String consequence = infoMap.get("Consequence");
        String impact = infoMap.get("IMPACT");
        String clinicalSignificance = infoMap.get("CLNSIG");

        // Determine if climate-relevant
        boolean climateRelevant = false;
        String climateImpactCategory = null;
        if (geneName != null) {
            climateImpactCategory = CLIMATE_RELEVANT_GENES.get(geneName);
            climateRelevant = climateImpactCategory != null;
        }

        // Parse allele frequency
        Double alleleFrequency = null;
        String af = infoMap.get("AF");
        if (af != null) {
            try {
                alleleFrequency = Double.parseDouble(af);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        return GenomicData.builder()
            .userId(userId)
            .chromosome(chromosome)
            .position(position)
            .rsId(id)
            .referenceAllele(ref)
            .alternateAllele(alt)
            .qualityScore(quality)
            .filter(filter)
            .genotype(genotype)
            .genotypeQuality(genotypeQuality)
            .readDepth(readDepth)
            .geneName(geneName)
            .geneId(geneId)
            .transcriptId(transcriptId)
            .variantType(variantType)
            .consequence(consequence)
            .impact(impact)
            .clinicalSignificance(clinicalSignificance)
            .climateRelevant(climateRelevant)
            .climateImpactCategory(climateImpactCategory)
            .alleleFrequency(alleleFrequency)
            .vcfInfo(info)
            .build();
    }

    /**
     * Parse VCF INFO field into key-value map
     */
    private Map<String, String> parseInfoField(String info) {
        Map<String, String> infoMap = new HashMap<>();

        if (info.equals(".")) {
            return infoMap;
        }

        String[] pairs = info.split(";");
        for (String pair : pairs) {
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex > 0) {
                String key = pair.substring(0, equalsIndex);
                String value = pair.substring(equalsIndex + 1);
                infoMap.put(key, value);
            } else {
                // Flag with no value
                infoMap.put(pair, "true");
            }
        }

        return infoMap;
    }

    /**
     * Parse VCF genotype field
     */
    private Map<String, String> parseGenotypeField(String format, String sample) {
        Map<String, String> genotypeMap = new HashMap<>();

        String[] formatFields = format.split(":");
        String[] sampleFields = sample.split(":");

        for (int i = 0; i < Math.min(formatFields.length, sampleFields.length); i++) {
            genotypeMap.put(formatFields[i], sampleFields[i]);
        }

        return genotypeMap;
    }

    /**
     * Normalize chromosome name
     */
    private String normalizeChromosome(String chr) {
        // Convert to standard format (chr1, chr2, etc.)
        if (chr.startsWith("chr")) {
            return chr;
        } else {
            return "chr" + chr;
        }
    }

    /**
     * Get all genomic data for a user
     */
    @Cacheable(value = "genomicData", key = "#userId")
    public List<GenomicData> getGenomicData(String userId) {
        log.info("Fetching genomic data for user: {}", userId);
        return genomicDataRepository.findByUserId(userId);
    }

    /**
     * Get climate-relevant variants for a user
     */
    @Cacheable(value = "climateVariants", key = "#userId")
    public List<GenomicData> getClimateRelevantVariants(String userId) {
        log.info("Fetching climate-relevant variants for user: {}", userId);
        return genomicDataRepository.findByUserIdAndClimateRelevant(userId, true);
    }

    /**
     * Get high-impact variants for a user
     */
    public List<GenomicData> getHighImpactVariants(String userId) {
        log.info("Fetching high-impact variants for user: {}", userId);
        return genomicDataRepository.findHighImpactVariants(userId);
    }

    /**
     * Get pathogenic variants for a user
     */
    public List<GenomicData> getPathogenicVariants(String userId) {
        log.info("Fetching pathogenic variants for user: {}", userId);
        return genomicDataRepository.findPathogenicVariants(userId);
    }
}
