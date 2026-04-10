package org.vgu.patientservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.vgu.patientservice.dto.*;
import org.vgu.patientservice.service.PatientService;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patients") // Gateway strips /api, so requests arrive as /patients
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    // ============================================
    // Patient Endpoints
    // ============================================

    // PUT/DELETE must come BEFORE GET with same path pattern to ensure method-specific matching
    @PutMapping(value = "/{patientId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long patientId,
            @Valid @RequestBody PatientUpdateRequest request) {
        log.info("Updating patient with ID: {}", patientId);
        PatientResponse updated = patientService.updatePatient(patientId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePatient(@PathVariable Long patientId) {
        log.info("Deleting patient with ID: {}", patientId);
        patientService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long patientId) {
        log.info("Fetching patient with ID: {}", patientId);
        PatientResponse patient = patientService.getPatientById(patientId);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/keycloak/{keycloakUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> getPatientByKeycloakId(@PathVariable String keycloakUserId) {
        log.info("Fetching patient with Keycloak ID: {}", keycloakUserId);
        PatientResponse patient = patientService.getPatientByKeycloakId(keycloakUserId);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> getCurrentPatient(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakUserId = jwt.getSubject();
        log.info("Fetching current patient with Keycloak ID: {}", keycloakUserId);
        PatientResponse patient = patientService.getPatientByKeycloakId(keycloakUserId);
        return ResponseEntity.ok(patient);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        log.info("Fetching all patients");
        List<PatientResponse> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> createPatientJson(@Valid @RequestBody PatientCreateRequest request,
            Authentication authentication) {
        log.info("Creating patient (JSON): {} {}", request.getFirstname(), request.getLastname());
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String creatorKeycloakUserId = jwt.getSubject();
        PatientResponse created = patientService.createPatient(request, creatorKeycloakUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> createPatientForm(
            @RequestParam String firstname,
            @RequestParam String lastname,
            @RequestParam String birthday,
            @RequestParam String gender,
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String emergencyContact,
            @RequestParam(required = false) String keycloakUserId,
            @RequestParam(required = false) String hospitalId,
            Authentication authentication) {
        log.info("Creating patient (form): {} {}", firstname, lastname);
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String creatorKeycloakUserId = jwt.getSubject();
        PatientCreateRequest request = PatientCreateRequest.builder()
                .firstname(firstname)
                .lastname(lastname)
                .birthday(LocalDate.parse(birthday))
                .gender(gender)
                .phoneNumber(phoneNumber)
                .address(address)
                .emergencyContact(emergencyContact)
                .keycloakUserId(keycloakUserId)
                .hospitalId(hospitalId)
                .build();
        PatientResponse created = patientService.createPatient(request, creatorKeycloakUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientResponse>> getPatientsByHospital(@PathVariable String hospitalId) {
        log.info("Fetching patients for hospital: {}", hospitalId);
        List<PatientResponse> patients = patientService.getPatientsByHospital(hospitalId);
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientResponse>> searchPatients(@RequestParam String query) {
        log.info("Searching patients with query: {}", query);
        List<PatientResponse> patients = patientService.searchPatients(query);
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/{patientId}/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientDetailResponse> getPatientDetail(@PathVariable Long patientId) {
        log.info("Fetching patient detail for ID: {}", patientId);
        PatientDetailResponse detail = patientService.getPatientDetail(patientId);
        return ResponseEntity.ok(detail);
    }

    // ============================================
    // Medical History Endpoints
    // ============================================

    @GetMapping("/{patientId}/medical-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicalHistoryResponse>> getMedicalHistory(@PathVariable Long patientId) {
        log.info("Fetching medical history for patient ID: {}", patientId);
        List<MedicalHistoryResponse> history = patientService.getMedicalHistoryByPatient(patientId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{patientId}/medical-history/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicalHistoryResponse>> getRecentMedicalHistory(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching recent {} medical history records for patient ID: {}", limit, patientId);
        List<MedicalHistoryResponse> history = patientService.getRecentMedicalHistory(patientId, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/medical-history/{historyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicalHistoryResponse> getMedicalHistoryById(@PathVariable Long historyId) {
        log.info("Fetching medical history with ID: {}", historyId);
        MedicalHistoryResponse history = patientService.getMedicalHistoryById(historyId);
        return ResponseEntity.ok(history);
    }

    // ============================================
    // Patient Allergy Endpoints
    // ============================================

    @GetMapping("/{patientId}/allergies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientAllergyResponse>> getAllergies(@PathVariable Long patientId) {
        log.info("Fetching allergies for patient ID: {}", patientId);
        List<PatientAllergyResponse> allergies = patientService.getAllergiesByPatient(patientId);
        return ResponseEntity.ok(allergies);
    }

    @GetMapping("/allergies/{allergyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientAllergyResponse> getAllergyById(@PathVariable Long allergyId) {
        log.info("Fetching allergy with ID: {}", allergyId);
        PatientAllergyResponse allergy = patientService.getAllergyById(allergyId);
        return ResponseEntity.ok(allergy);
    }

    // ============================================
    // Health Check
    // ============================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "patient-service"));
    }
}
