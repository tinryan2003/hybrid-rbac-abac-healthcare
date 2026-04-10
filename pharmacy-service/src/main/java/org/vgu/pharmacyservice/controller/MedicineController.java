package org.vgu.pharmacyservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.vgu.pharmacyservice.dto.MedicineInventoryTransactionResponse;
import org.vgu.pharmacyservice.dto.MedicineResponse;
import org.vgu.pharmacyservice.service.MedicineService;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pharmacy/medicines")
@RequiredArgsConstructor
@Slf4j
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> getMedicineById(@PathVariable Long medicineId) {
        log.info("Fetching medicine with ID: {}", medicineId);
        MedicineResponse medicine = medicineService.getMedicineById(medicineId);
        return ResponseEntity.ok(medicine);
    }

    @GetMapping
    public ResponseEntity<List<MedicineResponse>> getAllMedicines(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStock
    ) {
        String hospitalId = principal != null ? principal.getClaimAsString("hospital_id") : null;
        log.info("Fetching medicines for hospital: {} (active={}, category={}, lowStock={})",
                hospitalId, active, category, lowStock);
        List<MedicineResponse> medicines = medicineService.getMedicinesFiltered(
                hospitalId,
                active,
                category,
                lowStock
        );
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<MedicineResponse>> getMedicinesByHospital(@PathVariable String hospitalId) {
        log.info("Fetching medicines for hospital: {}", hospitalId);
        List<MedicineResponse> medicines = medicineService.getMedicinesByHospital(hospitalId);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/active")
    public ResponseEntity<List<MedicineResponse>> getActiveMedicines() {
        log.info("Fetching active medicines");
        List<MedicineResponse> medicines = medicineService.getActiveMedicines();
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineResponse>> searchMedicines(@RequestParam String query) {
        log.info("Searching medicines with query: {}", query);
        List<MedicineResponse> medicines = medicineService.searchMedicines(query);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<MedicineResponse>> getMedicinesByCategory(@PathVariable String category) {
        log.info("Fetching medicines by category: {}", category);
        List<MedicineResponse> medicines = medicineService.getMedicinesByCategory(category);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<MedicineResponse>> getLowStockMedicines(@AuthenticationPrincipal Jwt principal) {
        String hospitalId = principal != null ? principal.getClaimAsString("hospital_id") : null;
        log.info("Fetching low stock medicines for hospital: {}", hospitalId);
        List<MedicineResponse> medicines = medicineService.getMedicinesFiltered(
                hospitalId,
                null,
                null,
                true
        );
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/{medicineId}/inventory-transactions")
    public ResponseEntity<List<MedicineInventoryTransactionResponse>> getInventoryTransactions(@PathVariable Long medicineId) {
        log.info("Fetching inventory transactions for medicine ID: {}", medicineId);
        List<MedicineInventoryTransactionResponse> transactions = medicineService.getInventoryTransactionsByMedicine(medicineId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping
    public ResponseEntity<MedicineResponse> createMedicine(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody MedicineResponse medicineRequest
    ) {
        String hospitalId = principal != null ? principal.getClaimAsString("hospital_id") : null;
        medicineRequest.setHospitalId(hospitalId);
        log.info("Creating new medicine: {} for hospital {}", medicineRequest.getName(), hospitalId);
        MedicineResponse created = medicineService.createMedicine(medicineRequest);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> updateMedicine(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable Long medicineId,
            @RequestBody MedicineResponse medicineRequest
    ) {
        String hospitalId = principal != null ? principal.getClaimAsString("hospital_id") : null;
        medicineRequest.setHospitalId(hospitalId);
        log.info("Updating medicine ID: {} for hospital {}", medicineId, hospitalId);
        MedicineResponse updated = medicineService.updateMedicine(medicineId, medicineRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{medicineId}")
    public ResponseEntity<Map<String, Object>> deleteMedicine(@PathVariable Long medicineId) {
        log.info("Deleting medicine ID: {}", medicineId);
        medicineService.deleteMedicine(medicineId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Medicine deleted successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "pharmacy-service"));
    }
}
