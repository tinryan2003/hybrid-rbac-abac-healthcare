package org.vgu.labservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.vgu.labservice.model.LabTestCatalog;
import org.vgu.labservice.repository.LabTestCatalogRepository;

import java.util.List;

/**
 * Exposes the lab test catalog so the frontend can present test choices
 * when doctors create lab orders.
 */
@RestController
@RequestMapping("/lab/catalog")
@RequiredArgsConstructor
@Slf4j
public class LabCatalogController {

    private final LabTestCatalogRepository labTestCatalogRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabTestCatalog>> getAllActiveTests() {
        log.info("Fetching all active lab tests from catalog");
        return ResponseEntity.ok(labTestCatalogRepository.findByIsActiveTrue());
    }

    @GetMapping("/{testId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabTestCatalog> getTestById(@PathVariable Long testId) {
        log.info("Fetching lab test catalog entry with ID: {}", testId);
        return labTestCatalogRepository.findById(testId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LabTestCatalog>> getTestsByCategory(@PathVariable String category) {
        log.info("Fetching lab tests by category: {}", category);
        return ResponseEntity.ok(labTestCatalogRepository.findByTestCategoryAndIsActiveTrue(category));
    }
}
