package org.vgu.appointmentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.appointmentservice.repository.AppointmentRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Policy Information Point (PIP) - Resource attributes for ABAC.
 * Returns appointment resource attributes for authorization decisions.
 */
@RestController
@RequestMapping("/api/appointments/pip")
@RequiredArgsConstructor
@Slf4j
public class PipController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable Long resourceId) {
        log.debug("PIP: get resource attributes for appointment resourceId={}", resourceId);
        return appointmentRepository.findById(resourceId)
                .map(a -> {
                    Map<String, Object> attrs = new HashMap<>();
                    attrs.put("owner_id", String.valueOf(a.getPatientId()));
                    if (a.getDepartmentId() != null) attrs.put("department_id", String.valueOf(a.getDepartmentId()));
                    if (a.getHospitalId() != null) attrs.put("hospital_id", a.getHospitalId());
                    if (a.getStatus() != null) attrs.put("status", a.getStatus());
                    if (a.getCreatedByKeycloakId() != null) attrs.put("created_by", a.getCreatedByKeycloakId());
                    // Appointments default to NORMAL sensitivity
                    attrs.put("sensitivity_level", "NORMAL");
                    return ResponseEntity.<Map<String, Object>>ok(attrs);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
