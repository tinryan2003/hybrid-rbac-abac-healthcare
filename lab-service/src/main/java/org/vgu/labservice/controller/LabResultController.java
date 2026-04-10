package org.vgu.labservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.vgu.labservice.dto.LabResultRequest;
import org.vgu.labservice.dto.LabResultResponse;
import org.vgu.labservice.service.LabResultService;

import java.util.List;

@RestController
@RequestMapping("/lab/results")
@RequiredArgsConstructor
@Slf4j
public class LabResultController {

    private final LabResultService labResultService;

    @GetMapping("/{resultId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabResultResponse> getLabResultById(@PathVariable Long resultId) {
        log.info("Fetching lab result with ID: {}", resultId);
        LabResultResponse result = labResultService.getLabResultById(resultId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/order/{labOrderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabResultResponse>> getLabResultsByOrderId(@PathVariable Long labOrderId) {
        log.info("Fetching lab results for order ID: {}", labOrderId);
        List<LabResultResponse> results = labResultService.getLabResultsByOrderId(labOrderId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabResultResponse>> getLabResultsByPatientId(@PathVariable Long patientId) {
        log.info("Fetching lab results for patient ID: {}", patientId);
        List<LabResultResponse> results = labResultService.getLabResultsByPatientId(patientId);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabResultResponse>> getAllLabResults() {
        log.info("Fetching all lab results");
        List<LabResultResponse> results = labResultService.getAllLabResults();
        return ResponseEntity.ok(results);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabResultResponse> createLabResult(@Valid @RequestBody LabResultRequest request) {
        log.info("Creating lab result for order ID: {}", request.getLabOrderId());
        LabResultResponse created = labResultService.createLabResult(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{resultId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabResultResponse> updateLabResult(
            @PathVariable Long resultId,
            @Valid @RequestBody LabResultRequest request) {
        log.info("Updating lab result with ID: {}", resultId);
        LabResultResponse updated = labResultService.updateLabResult(resultId, request);
        return ResponseEntity.ok(updated);
    }
}
