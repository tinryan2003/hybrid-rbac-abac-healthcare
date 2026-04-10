package org.vgu.pharmacyservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.pharmacyservice.repository.MedicineRepository;
import org.vgu.pharmacyservice.service.PrescriptionService;

import java.util.HashMap;
import java.util.Map;

/**
 * Policy Information Point (PIP) - Resource attributes for ABAC.
 * Handles both prescription and medicine resources.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PipController {

    private final PrescriptionService prescriptionService;
    private final MedicineRepository medicineRepository;

    /**
     * PIP for prescription resource: owner_id, hospital_id, status,
     * sensitivity_level.
     */
    @GetMapping("/pharmacy/prescriptions/pip/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> getPrescriptionAttributes(@PathVariable Long resourceId) {
        log.debug("PIP: get resource attributes for prescription resourceId={}", resourceId);
        return prescriptionService.getPipResourceAttributes(resourceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PIP for medicine resource: hospital_id, controlled_substance,
     * sensitivity_level.
     * Controlled substances are CRITICAL sensitivity, prescription-only are HIGH,
     * others NORMAL.
     */
    @GetMapping("/pharmacy/medicines/pip/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> getMedicineAttributes(@PathVariable Long resourceId) {
        log.debug("PIP: get resource attributes for medicine resourceId={}", resourceId);
        return medicineRepository.findById(resourceId)
                .map(m -> {
                    Map<String, Object> attrs = new HashMap<>();
                    if (m.getHospitalId() != null)
                        attrs.put("hospital_id", m.getHospitalId());
                    attrs.put("requires_prescription", String.valueOf(m.getRequiresPrescription()));
                    attrs.put("controlled_substance", String.valueOf(m.getControlledSubstance()));
                    // Sensitivity derived from medicine type
                    String sensitivity;
                    if (Boolean.TRUE.equals(m.getControlledSubstance())) {
                        sensitivity = "CRITICAL";
                    } else if (Boolean.TRUE.equals(m.getRequiresPrescription())) {
                        sensitivity = "HIGH";
                    } else {
                        sensitivity = "NORMAL";
                    }
                    attrs.put("sensitivity_level", sensitivity);
                    return ResponseEntity.<Map<String, Object>>ok(attrs);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
