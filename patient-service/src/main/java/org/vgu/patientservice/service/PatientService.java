package org.vgu.patientservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.patientservice.dto.*;
import org.vgu.patientservice.exception.KeycloakIntegrationException;
import org.vgu.patientservice.exception.PatientNotFoundException;
import org.vgu.patientservice.model.*;
import org.vgu.patientservice.repository.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final KeycloakAdminService keycloakAdminService;

    // ============================================
    // Patient Operations
    // ============================================

    public PatientResponse getPatientById(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));
        return mapToPatientResponse(patient);
    }

    public PatientResponse getPatientByKeycloakId(String keycloakUserId) {
        Patient patient = patientRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(
                        () -> new PatientNotFoundException("Patient not found with Keycloak ID: " + keycloakUserId));
        return mapToPatientResponse(patient);
    }

    /**
     * PIP: Resource attributes for ABAC (owner_id, hospital_id, sensitivity_level).
     * Patient records are always NORMAL sensitivity unless flagged otherwise.
     */
    public Optional<Map<String, Object>> getPipResourceAttributes(Long resourceId) {
        return patientRepository.findById(resourceId).map(p -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("owner_id",
                    p.getKeycloakUserId() != null ? p.getKeycloakUserId() : String.valueOf(p.getPatientId()));
            if (p.getCreatedByKeycloakId() != null && !p.getCreatedByKeycloakId().isBlank())
                attrs.put("created_by", p.getCreatedByKeycloakId());
            if (p.getHospitalId() != null)
                attrs.put("hospital_id", p.getHospitalId());
            // Patient records default to NORMAL sensitivity
            attrs.put("sensitivity_level", "NORMAL");
            return attrs;
        });
    }

    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::mapToPatientResponse)
                .collect(Collectors.toList());
    }

    public List<PatientResponse> getPatientsByHospital(String hospitalId) {
        return patientRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToPatientResponse)
                .collect(Collectors.toList());
    }

    public List<PatientResponse> searchPatients(String searchTerm) {
        return patientRepository
                .findByLastnameContainingIgnoreCaseOrFirstnameContainingIgnoreCase(searchTerm, searchTerm).stream()
                .map(this::mapToPatientResponse)
                .collect(Collectors.toList());
    }

    public PatientResponse createPatient(org.vgu.patientservice.dto.PatientCreateRequest request, String creatorKeycloakUserId) {
        if (request.getPhoneNumber() != null
                && patientRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Patient with phone number already exists: " + request.getPhoneNumber());
        }

        String keycloakUserId = request.getKeycloakUserId();

        // Create user in Keycloak if username/email provided and keycloakUserId not
        // provided
        if (keycloakUserId == null && request.getUsername() != null && !request.getUsername().isEmpty()) {
            try {
                // Keycloak user attributes: only username, email, firstName, lastName,
                // hospital_id
                Map<String, String> attributes = new HashMap<>();
                if (request.getHospitalId() != null) {
                    attributes.put("hospital_id", request.getHospitalId());
                }

                // Generate username if not provided
                String username = request.getUsername();
                if (username == null || username.isEmpty()) {
                    username = (request.getFirstname() + "." + request.getLastname()).toLowerCase()
                            .replaceAll("[^a-z0-9]", "");
                }

                // Generate email if not provided
                String email = request.getEmail();
                if (email == null || email.isEmpty()) {
                    email = username + "@hospital.local";
                }

                keycloakUserId = keycloakAdminService.createUser(
                        username,
                        email,
                        request.getPassword(),
                        request.getFirstname(),
                        request.getLastname(),
                        "PATIENT",
                        attributes);

                log.info("Created Keycloak user for patient: {} with ID: {}", username, keycloakUserId);
            } catch (KeycloakIntegrationException e) {
                log.error("Failed to create Keycloak user for patient: {}", request.getUsername(), e);
                throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
            }
        }

        Patient patient = new Patient();
        patient.setFirstname(request.getFirstname());
        patient.setLastname(request.getLastname());
        patient.setAddress(request.getAddress());
        patient.setBirthday(request.getBirthday());
        patient.setGender(request.getGender());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setEmergencyContact(request.getEmergencyContact());
        patient.setKeycloakUserId(keycloakUserId);
        patient.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
        patient.setCreatedByKeycloakId(creatorKeycloakUserId);
        patient = patientRepository.save(patient);
        log.info("Created patient with ID: {} and Keycloak ID: {}", patient.getPatientId(), keycloakUserId);
        return mapToPatientResponse(patient);
    }

    public PatientResponse updatePatient(Long patientId, org.vgu.patientservice.dto.PatientUpdateRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));

        if (request.getFirstname() != null)
            patient.setFirstname(request.getFirstname());
        if (request.getLastname() != null)
            patient.setLastname(request.getLastname());
        if (request.getAddress() != null)
            patient.setAddress(request.getAddress());
        if (request.getBirthday() != null)
            patient.setBirthday(request.getBirthday());
        if (request.getGender() != null)
            patient.setGender(request.getGender());
        if (request.getPhoneNumber() != null) {
            if (!request.getPhoneNumber().equals(patient.getPhoneNumber())
                    && patientRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
                throw new IllegalArgumentException(
                        "Phone number already used by another patient: " + request.getPhoneNumber());
            }
            patient.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmergencyContact() != null)
            patient.setEmergencyContact(request.getEmergencyContact());
        if (request.getHospitalId() != null)
            patient.setHospitalId(request.getHospitalId());

        patient = patientRepository.save(patient);
        log.info("Updated patient with ID: {}", patientId);
        return mapToPatientResponse(patient);
    }

    public void deletePatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
        patientRepository.deleteById(patientId);
        log.info("Deleted patient with ID: {}", patientId);
    }

    public PatientDetailResponse getPatientDetail(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));

        List<MedicalHistoryResponse> medicalHistory = getMedicalHistoryByPatient(patientId);
        List<PatientAllergyResponse> allergies = getAllergiesByPatient(patientId);

        return PatientDetailResponse.builder()
                .patient(mapToPatientResponse(patient))
                .medicalHistory(medicalHistory)
                .allergies(allergies)
                .build();
    }

    // ============================================
    // Medical History Operations
    // ============================================

    public List<MedicalHistoryResponse> getMedicalHistoryByPatient(Long patientId) {
        return medicalHistoryRepository.findByPatient_PatientIdOrderByCreationDateDesc(patientId).stream()
                .map(this::mapToMedicalHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<MedicalHistoryResponse> getRecentMedicalHistory(Long patientId, int limit) {
        if (limit <= 10) {
            return medicalHistoryRepository.findTop10ByPatient_PatientIdOrderByCreationDateDesc(patientId).stream()
                    .map(this::mapToMedicalHistoryResponse)
                    .collect(Collectors.toList());
        }
        return getMedicalHistoryByPatient(patientId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public MedicalHistoryResponse getMedicalHistoryById(Long id) {
        MedicalHistory history = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Medical history not found with ID: " + id));
        return mapToMedicalHistoryResponse(history);
    }

    // ============================================
    // Patient Allergy Operations
    // ============================================

    public List<PatientAllergyResponse> getAllergiesByPatient(Long patientId) {
        return patientAllergyRepository.findByPatient_PatientId(patientId).stream()
                .map(this::mapToPatientAllergyResponse)
                .collect(Collectors.toList());
    }

    public PatientAllergyResponse getAllergyById(Long allergyId) {
        PatientAllergy allergy = patientAllergyRepository.findById(allergyId)
                .orElseThrow(() -> new PatientNotFoundException("Allergy record not found with ID: " + allergyId));
        return mapToPatientAllergyResponse(allergy);
    }

    // ============================================
    // Mappers
    // ============================================

    private PatientResponse mapToPatientResponse(Patient patient) {
        Integer age = null;
        if (patient.getBirthday() != null) {
            age = Period.between(patient.getBirthday(), LocalDate.now()).getYears();
        }

        return PatientResponse.builder()
                .patientId(patient.getPatientId())
                .firstname(patient.getFirstname())
                .lastname(patient.getLastname())
                .address(patient.getAddress())
                .birthday(patient.getBirthday())
                .gender(patient.getGender())
                .phoneNumber(patient.getPhoneNumber())
                .emergencyContact(patient.getEmergencyContact())
                .createdDate(patient.getCreatedDate())
                .lastVisited(patient.getLastVisited())
                .keycloakUserId(patient.getKeycloakUserId())
                .hospitalId(patient.getHospitalId())
                .age(age)
                .build();
    }

    private MedicalHistoryResponse mapToMedicalHistoryResponse(MedicalHistory history) {
        return MedicalHistoryResponse.builder()
                .id(history.getId())
                .patientId(history.getPatient().getPatientId())
                .bloodPressure(history.getBloodPressure())
                .bloodSugar(history.getBloodSugar())
                .weight(history.getWeight())
                .height(history.getHeight())
                .temperature(history.getTemperature())
                .medicalPrescription(history.getMedicalPrescription())
                .creationDate(history.getCreationDate())
                .build();
    }

    private PatientAllergyResponse mapToPatientAllergyResponse(PatientAllergy allergy) {
        return PatientAllergyResponse.builder()
                .allergyId(allergy.getAllergyId())
                .patientId(allergy.getPatient().getPatientId())
                .allergen(allergy.getAllergen())
                .severity(allergy.getSeverity())
                .reaction(allergy.getReaction())
                .diagnosedDate(allergy.getDiagnosedDate())
                .createdAt(allergy.getCreatedAt())
                .build();
    }
}
