package org.vgu.pharmacyservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.vgu.pharmacyservice.dto.PrescriptionResponse;
import org.vgu.pharmacyservice.service.PrescriptionService;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/pharmacy/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/{prescriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable Long prescriptionId) {
        log.info("Fetching prescription with ID: {}", prescriptionId);
        PrescriptionResponse prescription = prescriptionService.getPrescriptionById(prescriptionId);
        return ResponseEntity.ok(prescription);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByPatient(@PathVariable Long patientId) {
        log.info("Fetching prescriptions for patient ID: {}", patientId);
        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByDoctor(@PathVariable Long doctorId) {
        log.info("Fetching prescriptions for doctor ID: {}", doctorId);
        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByDoctor(doctorId);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByHospital(@PathVariable String hospitalId) {
        log.info("Fetching prescriptions for hospital: {}", hospitalId);
        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByHospital(hospitalId);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByStatus(@PathVariable String status) {
        log.info("Fetching prescriptions with status: {}", status);
        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByStatus(status);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionResponse>> getAllPrescriptions(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam(required = false) String status
    ) {
        String hospitalId = principal != null ? principal.getClaimAsString("hospital_id") : null;
        log.info("Fetching prescriptions for hospital: {} with status: {}", hospitalId, status);
        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsForHospital(hospitalId, status);
        return ResponseEntity.ok(prescriptions);
    }
}
