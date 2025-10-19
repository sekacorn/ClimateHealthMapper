package com.climate.integrator.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.climate.integrator.model.HealthData;
import com.climate.integrator.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service for processing FHIR health data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HealthDataService {

    private final HealthDataRepository healthDataRepository;
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Process and store FHIR health data
     */
    @Transactional
    public List<HealthData> processFhirData(MultipartFile file, String userId) throws IOException {
        log.info("Processing FHIR data for user: {}", userId);

        String fhirJson = new String(file.getBytes());
        IParser parser = fhirContext.newJsonParser();

        List<HealthData> dataList = new ArrayList<>();

        try {
            // Try to parse as Bundle first
            Bundle bundle = parser.parseResource(Bundle.class, fhirJson);
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                HealthData healthData = parseResource(resource, userId);
                if (healthData != null) {
                    dataList.add(healthData);
                }
            }
        } catch (Exception e) {
            // Try to parse as single resource
            try {
                Resource resource = parser.parseResource(Resource.class, fhirJson);
                HealthData healthData = parseResource(resource, userId);
                if (healthData != null) {
                    dataList.add(healthData);
                }
            } catch (Exception ex) {
                log.error("Error parsing FHIR resource: {}", ex.getMessage());
                throw new IllegalArgumentException("Invalid FHIR format: " + ex.getMessage());
            }
        }

        // Save all records
        List<HealthData> savedData = healthDataRepository.saveAll(dataList);
        log.info("Successfully saved {} health data records", savedData.size());

        return savedData;
    }

    /**
     * Parse FHIR resource to HealthData entity
     */
    private HealthData parseResource(Resource resource, String userId) {
        if (resource instanceof Observation) {
            return parseObservation((Observation) resource, userId);
        } else if (resource instanceof Condition) {
            return parseCondition((Condition) resource, userId);
        } else if (resource instanceof MedicationRequest) {
            return parseMedicationRequest((MedicationRequest) resource, userId);
        } else if (resource instanceof Procedure) {
            return parseProcedure((Procedure) resource, userId);
        } else {
            log.warn("Unsupported FHIR resource type: {}", resource.getResourceType());
            return null;
        }
    }

    /**
     * Parse FHIR Observation
     */
    private HealthData parseObservation(Observation observation, String userId) {
        HealthData.HealthDataBuilder builder = HealthData.builder()
            .userId(userId)
            .resourceType("Observation")
            .fhirResourceId(observation.getId())
            .rawFhirJson(fhirContext.newJsonParser().encodeResourceToString(observation));

        // Date
        if (observation.hasEffectiveDateTimeType()) {
            builder.observationDate(toLocalDateTime(observation.getEffectiveDateTimeType().getValue()));
        } else if (observation.hasIssued()) {
            builder.observationDate(toLocalDateTime(observation.getIssued()));
        } else {
            builder.observationDate(LocalDateTime.now());
        }

        // Code
        if (observation.hasCode()) {
            CodeableConcept code = observation.getCode();
            if (code.hasCoding() && !code.getCoding().isEmpty()) {
                Coding coding = code.getCodingFirstRep();
                builder.code(coding.getCode())
                    .codeSystem(coding.getSystem())
                    .displayName(coding.getDisplay());
            }
        }

        // Category
        if (observation.hasCategory()) {
            CodeableConcept category = observation.getCategoryFirstRep();
            if (category.hasCoding()) {
                builder.category(category.getCodingFirstRep().getCode());
            }
        }

        // Value
        if (observation.hasValue()) {
            Type value = observation.getValue();
            if (value instanceof Quantity) {
                Quantity quantity = (Quantity) value;
                builder.valueType("Quantity")
                    .valueQuantity(quantity.getValue().doubleValue())
                    .valueUnit(quantity.getUnit());
            } else if (value instanceof CodeableConcept) {
                CodeableConcept codeable = (CodeableConcept) value;
                builder.valueType("CodeableConcept")
                    .valueCode(codeable.getCodingFirstRep().getCode())
                    .valueString(codeable.getText());
            } else if (value instanceof StringType) {
                builder.valueType("String")
                    .valueString(((StringType) value).getValue());
            }
        }

        // Interpretation
        if (observation.hasInterpretation()) {
            builder.interpretation(observation.getInterpretationFirstRep()
                .getCodingFirstRep().getCode());
        }

        // Reference range
        if (observation.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent range =
                observation.getReferenceRangeFirstRep();
            if (range.hasLow()) {
                builder.referenceLow(range.getLow().getValue().doubleValue());
            }
            if (range.hasHigh()) {
                builder.referenceHigh(range.getHigh().getValue().doubleValue());
            }
        }

        // Performer
        if (observation.hasPerformer()) {
            Reference performer = observation.getPerformerFirstRep();
            builder.practitionerId(performer.getReference())
                .practitionerName(performer.getDisplay());
        }

        return builder.build();
    }

    /**
     * Parse FHIR Condition
     */
    private HealthData parseCondition(Condition condition, String userId) {
        HealthData.HealthDataBuilder builder = HealthData.builder()
            .userId(userId)
            .resourceType("Condition")
            .fhirResourceId(condition.getId())
            .rawFhirJson(fhirContext.newJsonParser().encodeResourceToString(condition));

        // Date
        if (condition.hasRecordedDate()) {
            builder.observationDate(toLocalDateTime(condition.getRecordedDate()));
        } else {
            builder.observationDate(LocalDateTime.now());
        }

        // Code
        if (condition.hasCode()) {
            CodeableConcept code = condition.getCode();
            if (code.hasCoding()) {
                Coding coding = code.getCodingFirstRep();
                builder.code(coding.getCode())
                    .codeSystem(coding.getSystem())
                    .displayName(coding.getDisplay());
            }
        }

        // Clinical status
        if (condition.hasClinicalStatus()) {
            builder.clinicalStatus(condition.getClinicalStatus()
                .getCodingFirstRep().getCode());
        }

        // Verification status
        if (condition.hasVerificationStatus()) {
            builder.verificationStatus(condition.getVerificationStatus()
                .getCodingFirstRep().getCode());
        }

        // Severity
        if (condition.hasSeverity()) {
            builder.severity(condition.getSeverity().getCodingFirstRep().getCode());
        }

        // Category
        if (condition.hasCategory()) {
            builder.category(condition.getCategoryFirstRep()
                .getCodingFirstRep().getCode());
        }

        return builder.build();
    }

    /**
     * Parse FHIR MedicationRequest
     */
    private HealthData parseMedicationRequest(MedicationRequest medRequest, String userId) {
        HealthData.HealthDataBuilder builder = HealthData.builder()
            .userId(userId)
            .resourceType("MedicationRequest")
            .fhirResourceId(medRequest.getId())
            .rawFhirJson(fhirContext.newJsonParser().encodeResourceToString(medRequest));

        // Date
        if (medRequest.hasAuthoredOn()) {
            builder.observationDate(toLocalDateTime(medRequest.getAuthoredOn()));
        } else {
            builder.observationDate(LocalDateTime.now());
        }

        // Medication
        if (medRequest.hasMedication()) {
            Type medication = medRequest.getMedication();
            if (medication instanceof CodeableConcept) {
                CodeableConcept medCode = (CodeableConcept) medication;
                if (medCode.hasCoding()) {
                    Coding coding = medCode.getCodingFirstRep();
                    builder.medicationCode(coding.getCode())
                        .code(coding.getCode())
                        .displayName(coding.getDisplay());
                }
            }
        }

        // Status
        if (medRequest.hasStatus()) {
            builder.clinicalStatus(medRequest.getStatus().toCode());
        }

        // Dosage
        if (medRequest.hasDosageInstruction()) {
            Dosage dosage = medRequest.getDosageInstructionFirstRep();
            if (dosage.hasText()) {
                builder.dosage(dosage.getText());
            }
            if (dosage.hasTiming() && dosage.getTiming().hasCode()) {
                builder.frequency(dosage.getTiming().getCode().getText());
            }
        }

        return builder.build();
    }

    /**
     * Parse FHIR Procedure
     */
    private HealthData parseProcedure(Procedure procedure, String userId) {
        HealthData.HealthDataBuilder builder = HealthData.builder()
            .userId(userId)
            .resourceType("Procedure")
            .fhirResourceId(procedure.getId())
            .rawFhirJson(fhirContext.newJsonParser().encodeResourceToString(procedure));

        // Date
        if (procedure.hasPerformed()) {
            Type performed = procedure.getPerformed();
            if (performed instanceof DateTimeType) {
                builder.observationDate(toLocalDateTime(
                    ((DateTimeType) performed).getValue()));
            } else if (performed instanceof Period) {
                Period period = (Period) performed;
                if (period.hasStart()) {
                    builder.observationDate(toLocalDateTime(period.getStart()));
                }
            }
        } else {
            builder.observationDate(LocalDateTime.now());
        }

        // Code
        if (procedure.hasCode()) {
            CodeableConcept code = procedure.getCode();
            if (code.hasCoding()) {
                Coding coding = code.getCodingFirstRep();
                builder.code(coding.getCode())
                    .codeSystem(coding.getSystem())
                    .displayName(coding.getDisplay());
            }
        }

        // Status
        if (procedure.hasStatus()) {
            builder.clinicalStatus(procedure.getStatus().toCode());
        }

        return builder.build();
    }

    /**
     * Get all health data for a user
     */
    @Cacheable(value = "healthData", key = "#userId")
    public List<HealthData> getHealthData(String userId) {
        log.info("Fetching health data for user: {}", userId);
        return healthDataRepository.findByUserId(userId);
    }

    /**
     * Get health data by resource type
     */
    public List<HealthData> getHealthDataByType(String userId, String resourceType) {
        log.info("Fetching health data for user: {}, type: {}", userId, resourceType);
        return healthDataRepository.findByUserIdAndResourceType(userId, resourceType);
    }

    /**
     * Convert Date to LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
