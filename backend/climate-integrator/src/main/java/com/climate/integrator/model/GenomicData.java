package com.climate.integrator.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Entity for Genomic Data (VCF)
 *
 * Stores parsed VCF (Variant Call Format) genomic variants
 * relevant to climate-health interactions
 */
@Entity
@Table(name = "genomic_data", indexes = {
    @Index(name = "idx_user_id_genomic", columnList = "userId"),
    @Index(name = "idx_chromosome_position", columnList = "chromosome, position"),
    @Index(name = "idx_gene_name", columnList = "geneName")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenomicData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User ID is required")
    @Column(nullable = false)
    private String userId;

    @NotBlank(message = "Chromosome is required")
    @Column(nullable = false)
    private String chromosome; // chr1, chr2, ..., chrX, chrY

    @NotNull(message = "Position is required")
    @Column(nullable = false)
    private Long position; // Genomic position

    private String rsId; // dbSNP reference SNP ID

    @NotBlank(message = "Reference allele is required")
    @Column(nullable = false)
    private String referenceAllele;

    @NotBlank(message = "Alternate allele is required")
    @Column(nullable = false)
    private String alternateAllele;

    private Double qualityScore; // Phred quality score

    private String filter; // PASS, or reason for failure

    // Genotype information
    private String genotype; // 0/0, 0/1, 1/1, etc.
    private Integer genotypeQuality;
    private Integer readDepth;

    // Annotation fields
    private String geneName;
    private String geneId;
    private String transcriptId;
    private String variantType; // SNV, insertion, deletion, etc.
    private String consequence; // missense, synonymous, frameshift, etc.
    private String impact; // HIGH, MODERATE, LOW, MODIFIER

    // Clinical significance
    private String clinicalSignificance; // pathogenic, benign, uncertain, etc.
    private String phenotype;

    // Climate-health relevance
    private Boolean climateRelevant; // Flag for climate-sensitive genes
    private String climateImpactCategory; // heat-stress, air-quality-response, etc.

    // Population frequency
    private Double alleleFrequency;
    private String populationDatabase; // gnomAD, 1000 Genomes, etc.

    @Column(columnDefinition = "TEXT")
    private String vcfInfo; // Complete INFO field from VCF

    @Column(columnDefinition = "TEXT")
    private String annotations; // JSON for additional annotations

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
