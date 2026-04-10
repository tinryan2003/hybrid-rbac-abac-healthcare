package org.vgu.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.pharmacyservice.dto.PrescriptionItemResponse;
import org.vgu.pharmacyservice.dto.PrescriptionResponse;
import org.vgu.pharmacyservice.exception.PharmacyNotFoundException;
import org.vgu.pharmacyservice.model.Prescription;
import org.vgu.pharmacyservice.model.PrescriptionItem;
import org.vgu.pharmacyservice.repository.PrescriptionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionResponse getPrescriptionById(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new PharmacyNotFoundException("Prescription not found with ID: " + prescriptionId));
        return mapToPrescriptionResponse(prescription);
    }

    /** PIP: Resource attributes for ABAC. */
    public Optional<Map<String, Object>> getPipResourceAttributes(Long resourceId) {
        return prescriptionRepository.findById(resourceId).map(p -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("owner_id", String.valueOf(p.getPatientId()));
            if (p.getCreatedByKeycloakId() != null && !p.getCreatedByKeycloakId().isBlank())
                attrs.put("created_by", p.getCreatedByKeycloakId());
            if (p.getHospitalId() != null) attrs.put("hospital_id", p.getHospitalId());
            if (p.getStatus() != null) attrs.put("status", p.getStatus());
            if (p.getSensitivityLevel() != null) attrs.put("sensitivity_level", p.getSensitivityLevel());
            return attrs;
        });
    }

    public List<PrescriptionResponse> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId).stream()
                .map(this::mapToPrescriptionResponse)
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> getPrescriptionsByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToPrescriptionResponse)
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> getPrescriptionsByHospital(String hospitalId) {
        return prescriptionRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToPrescriptionResponse)
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> getPrescriptionsByStatus(String status) {
        return prescriptionRepository.findByStatus(status).stream()
                .map(this::mapToPrescriptionResponse)
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> getAllPrescriptions() {
        return prescriptionRepository.findAll().stream()
                .map(this::mapToPrescriptionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetch prescriptions scoped by hospital and optional status.
     *
     * @param hospitalId Hospital identifier (can be null for all)
     * @param status     Optional status filter (PENDING, APPROVED, etc.)
     */
    public List<PrescriptionResponse> getPrescriptionsForHospital(String hospitalId, String status) {
        List<Prescription> base = (hospitalId != null && !hospitalId.isBlank())
                ? prescriptionRepository.findByHospitalId(hospitalId)
                : prescriptionRepository.findAll();

        return base.stream()
                .filter(p -> status == null || status.isBlank()
                        || (p.getStatus() != null && p.getStatus().equalsIgnoreCase(status)))
                .map(this::mapToPrescriptionResponse)
                .collect(Collectors.toList());
    }

    private PrescriptionResponse mapToPrescriptionResponse(Prescription p) {
        List<PrescriptionItemResponse> items = p.getItems() != null
                ? p.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList())
                : List.of();

        return PrescriptionResponse.builder()
                .prescriptionId(p.getPrescriptionId())
                .doctorId(p.getDoctorId())
                .patientId(p.getPatientId())
                .appointmentId(p.getAppointmentId())
                .prescriptionDate(p.getPrescriptionDate())
                .diagnosis(p.getDiagnosis())
                .notes(p.getNotes())
                .status(p.getStatus())
                .dispensedByPharmacistId(p.getDispensedByPharmacistId())
                .dispensedAt(p.getDispensedAt())
                .hospitalId(p.getHospitalId())
                .sensitivityLevel(p.getSensitivityLevel())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .items(items)
                .build();
    }

    private PrescriptionItemResponse mapToItemResponse(PrescriptionItem item) {
        String medicineName = item.getMedicine() != null ? item.getMedicine().getName() : null;
        return PrescriptionItemResponse.builder()
                .itemId(item.getItemId())
                .prescriptionId(item.getPrescription().getPrescriptionId())
                .medicineId(item.getMedicine().getMedicineId())
                .medicineName(medicineName)
                .dosage(item.getDosage())
                .frequency(item.getFrequency())
                .durationDays(item.getDurationDays())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .quantity(item.getQuantity())
                .quantityDispensed(item.getQuantityDispensed())
                .instructions(item.getInstructions())
                .beforeAfterMeal(item.getBeforeAfterMeal())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
