package org.vgu.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.pharmacyservice.dto.MedicineInventoryTransactionResponse;
import org.vgu.pharmacyservice.dto.MedicineResponse;
import org.vgu.pharmacyservice.exception.PharmacyNotFoundException;
import org.vgu.pharmacyservice.model.Medicine;
import org.vgu.pharmacyservice.model.MedicineInventoryTransaction;
import org.vgu.pharmacyservice.model.PrescriptionItem;
import org.vgu.pharmacyservice.repository.MedicineInventoryTransactionRepository;
import org.vgu.pharmacyservice.repository.MedicineRepository;
import org.vgu.pharmacyservice.repository.PrescriptionItemRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineInventoryTransactionRepository inventoryTransactionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;

    public MedicineResponse getMedicineById(Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new PharmacyNotFoundException("Medicine not found with ID: " + medicineId));
        return mapToMedicineResponse(medicine);
    }

    public List<MedicineResponse> getAllMedicines() {
        // Fallback: no filters, no hospital scoping
        return getMedicinesFiltered(null, null, null, null);
    }

    /**
     * Fetch medicines filtered by current hospital and optional flags.
     *
     * @param hospitalId Hospital identifier (can be null for all)
     * @param active     If non-null, filter by isActive flag
     * @param category   If non-null/non-blank, filter by category (case-insensitive)
     * @param lowStock   If true, only include medicines where stockQuantity <= reorderLevel
     */
    public List<MedicineResponse> getMedicinesFiltered(
            String hospitalId,
            Boolean active,
            String category,
            Boolean lowStock
    ) {
        List<Medicine> base = (hospitalId != null && !hospitalId.isBlank())
                ? medicineRepository.findByHospitalId(hospitalId)
                : medicineRepository.findAll();

        return base.stream()
                .filter(m -> active == null || Boolean.TRUE.equals(m.getIsActive()) == active)
                .filter(m -> category == null || category.isBlank()
                        || (m.getCategory() != null && m.getCategory().equalsIgnoreCase(category)))
                .filter(m -> !Boolean.TRUE.equals(lowStock)
                        || (m.getStockQuantity() != null && m.getReorderLevel() != null
                        && m.getStockQuantity() <= m.getReorderLevel()))
                .map(this::mapToMedicineResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineResponse> getMedicinesByHospital(String hospitalId) {
        return medicineRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToMedicineResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineResponse> getActiveMedicines() {
        return medicineRepository.findByIsActiveTrue().stream()
                .map(this::mapToMedicineResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineResponse> searchMedicines(String query) {
        return medicineRepository.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(query, query).stream()
                .map(this::mapToMedicineResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineResponse> getMedicinesByCategory(String category) {
        return medicineRepository.findByCategory(category).stream()
                .map(this::mapToMedicineResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineResponse> getLowStockMedicines() {
        return medicineRepository.findAll().stream()
                .filter(m -> m.getStockQuantity() != null && m.getReorderLevel() != null
                        && m.getStockQuantity() <= m.getReorderLevel())
                .map(this::mapToMedicineResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineInventoryTransactionResponse> getInventoryTransactionsByMedicine(Long medicineId) {
        return inventoryTransactionRepository.findByMedicine_MedicineIdOrderByTransactionDateDesc(medicineId).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicineResponse createMedicine(MedicineResponse request) {
        Medicine medicine = new Medicine();
        medicine.setName(request.getName());
        medicine.setGenericName(request.getGenericName());
        medicine.setBrandName(request.getBrandName());
        medicine.setDescription(request.getDescription());
        medicine.setSideEffect(request.getSideEffect());
        medicine.setCategory(request.getCategory());
        medicine.setDosageForm(request.getDosageForm());
        medicine.setStrength(request.getStrength());
        medicine.setUnit(request.getUnit());
        medicine.setUnitPrice(request.getUnitPrice());
        medicine.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        medicine.setReorderLevel(request.getReorderLevel() != null ? request.getReorderLevel() : 10);
        medicine.setRequiresPrescription(request.getRequiresPrescription() != null ? request.getRequiresPrescription() : true);
        medicine.setControlledSubstance(request.getControlledSubstance() != null ? request.getControlledSubstance() : false);
        medicine.setHospitalId(request.getHospitalId());
        medicine.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        Medicine saved = medicineRepository.save(medicine);
        log.info("Created medicine: {} (ID: {})", saved.getName(), saved.getMedicineId());
        return mapToMedicineResponse(saved);
    }

    @Transactional
    public MedicineResponse updateMedicine(Long medicineId, MedicineResponse request) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new PharmacyNotFoundException("Medicine not found with ID: " + medicineId));
        
        if (request.getName() != null) medicine.setName(request.getName());
        if (request.getGenericName() != null) medicine.setGenericName(request.getGenericName());
        if (request.getBrandName() != null) medicine.setBrandName(request.getBrandName());
        if (request.getDescription() != null) medicine.setDescription(request.getDescription());
        if (request.getSideEffect() != null) medicine.setSideEffect(request.getSideEffect());
        if (request.getCategory() != null) medicine.setCategory(request.getCategory());
        if (request.getDosageForm() != null) medicine.setDosageForm(request.getDosageForm());
        if (request.getStrength() != null) medicine.setStrength(request.getStrength());
        if (request.getUnit() != null) medicine.setUnit(request.getUnit());
        if (request.getUnitPrice() != null) medicine.setUnitPrice(request.getUnitPrice());
        if (request.getStockQuantity() != null) medicine.setStockQuantity(request.getStockQuantity());
        if (request.getReorderLevel() != null) medicine.setReorderLevel(request.getReorderLevel());
        if (request.getRequiresPrescription() != null) medicine.setRequiresPrescription(request.getRequiresPrescription());
        if (request.getControlledSubstance() != null) medicine.setControlledSubstance(request.getControlledSubstance());
        if (request.getHospitalId() != null) medicine.setHospitalId(request.getHospitalId());
        if (request.getIsActive() != null) medicine.setIsActive(request.getIsActive());
        
        Medicine updated = medicineRepository.save(medicine);
        log.info("Updated medicine ID: {}", medicineId);
        return mapToMedicineResponse(updated);
    }

    @Transactional
    public void deleteMedicine(Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new PharmacyNotFoundException("Medicine not found with ID: " + medicineId));

        // Delete inventory transactions referencing this medicine
        List<MedicineInventoryTransaction> transactions =
                inventoryTransactionRepository.findByMedicine_MedicineIdOrderByTransactionDateDesc(medicineId);
        if (!transactions.isEmpty()) {
            inventoryTransactionRepository.deleteAll(transactions);
        }

        // Delete prescription items referencing this medicine
        List<PrescriptionItem> items = prescriptionItemRepository.findByMedicine_MedicineId(medicineId);
        if (!items.isEmpty()) {
            prescriptionItemRepository.deleteAll(items);
        }

        // Hard delete medicine record
        medicineRepository.delete(medicine);
        log.info("Hard deleted medicine ID: {}", medicineId);
    }

    private MedicineResponse mapToMedicineResponse(Medicine medicine) {
        boolean lowStock = medicine.getReorderLevel() != null && medicine.getStockQuantity() != null
                && medicine.getStockQuantity() <= medicine.getReorderLevel();

        return MedicineResponse.builder()
                .medicineId(medicine.getMedicineId())
                .name(medicine.getName())
                .genericName(medicine.getGenericName())
                .brandName(medicine.getBrandName())
                .description(medicine.getDescription())
                .sideEffect(medicine.getSideEffect())
                .category(medicine.getCategory())
                .dosageForm(medicine.getDosageForm())
                .strength(medicine.getStrength())
                .unit(medicine.getUnit())
                .unitPrice(medicine.getUnitPrice())
                .stockQuantity(medicine.getStockQuantity())
                .reorderLevel(medicine.getReorderLevel())
                .requiresPrescription(medicine.getRequiresPrescription())
                .controlledSubstance(medicine.getControlledSubstance())
                .hospitalId(medicine.getHospitalId())
                .isActive(medicine.getIsActive())
                .createdAt(medicine.getCreatedAt())
                .updatedAt(medicine.getUpdatedAt())
                .lowStock(lowStock)
                .build();
    }

    private MedicineInventoryTransactionResponse mapToTransactionResponse(MedicineInventoryTransaction t) {
        String medicineName = t.getMedicine() != null ? t.getMedicine().getName() : null;
        return MedicineInventoryTransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .medicineId(t.getMedicine().getMedicineId())
                .medicineName(medicineName)
                .transactionType(t.getTransactionType())
                .quantity(t.getQuantity())
                .referenceId(t.getReferenceId())
                .referenceType(t.getReferenceType())
                .performedByKeycloakId(t.getPerformedByKeycloakId())
                .hospitalId(t.getHospitalId())
                .notes(t.getNotes())
                .transactionDate(t.getTransactionDate())
                .build();
    }
}
