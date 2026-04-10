package org.vgu.patientservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.patientservice.service.PatientService;

import java.util.Map;

/**
 * Policy Information Point (PIP) - Resource attributes for ABAC.
 * Returns resource (patient) attributes for authorization decisions.
 */
@RestController
@RequestMapping("/patients/pip")
@RequiredArgsConstructor
@Slf4j
public class PipController {

    private final PatientService patientService;

    /**
     * Get resource attributes for a patient (owner_id, hospital_id).
     * Used by authorization-service when object=patient or patient_record.
     */
    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable Long resourceId) {
        log.debug("PIP: get resource attributes for patient resourceId={}", resourceId);
        return patientService.getPipResourceAttributes(resourceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
