package org.vgu.labservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.vgu.labservice.dto.LabOrderRequest;
import org.vgu.labservice.dto.LabOrderResponse;
import org.vgu.labservice.service.LabOrderService;

import java.util.List;

@RestController
@RequestMapping("/lab/orders")
@RequiredArgsConstructor
@Slf4j
public class LabOrderController {

    private final LabOrderService labOrderService;

    @GetMapping("/{labOrderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabOrderResponse> getLabOrderById(@PathVariable Long labOrderId) {
        log.info("Fetching lab order with ID: {}", labOrderId);
        LabOrderResponse order = labOrderService.getLabOrderById(labOrderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabOrderResponse>> getLabOrdersByPatient(@PathVariable Long patientId) {
        log.info("Fetching lab orders for patient ID: {}", patientId);
        List<LabOrderResponse> orders = labOrderService.getLabOrdersByPatient(patientId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabOrderResponse>> getLabOrdersByDoctor(@PathVariable Long doctorId) {
        log.info("Fetching lab orders for doctor ID: {}", doctorId);
        List<LabOrderResponse> orders = labOrderService.getLabOrdersByDoctor(doctorId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabOrderResponse>> getLabOrdersByStatus(@PathVariable String status) {
        log.info("Fetching lab orders with status: {}", status);
        List<LabOrderResponse> orders = labOrderService.getLabOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabOrderResponse>> getLabOrdersByHospital(@PathVariable String hospitalId) {
        log.info("Fetching lab orders for hospital: {}", hospitalId);
        List<LabOrderResponse> orders = labOrderService.getLabOrdersByHospital(hospitalId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabOrderResponse>> getAllLabOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        log.info("Fetching lab orders - status={}, patientId={}, doctorId={}", status, patientId, doctorId);
        List<LabOrderResponse> orders = labOrderService.getLabOrders(status, patientId, doctorId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabOrderResponse> createLabOrder(@Valid @RequestBody LabOrderRequest request,
            Authentication authentication) {
        log.info("Creating lab order for patient ID: {}", request.getPatientId());
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String creatorKeycloakUserId = jwt.getSubject();
        LabOrderResponse created = labOrderService.createLabOrder(request, creatorKeycloakUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{labOrderId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabOrderResponse> updateLabOrderStatus(
            @PathVariable Long labOrderId,
            @RequestParam String status,
            Authentication authentication) {
        log.info("Updating lab order {} status to {}", labOrderId, status);
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakUserId = jwt.getSubject();
        LabOrderResponse updated = labOrderService.updateLabOrderStatus(labOrderId, status, keycloakUserId);
        return ResponseEntity.ok(updated);
    }
}
