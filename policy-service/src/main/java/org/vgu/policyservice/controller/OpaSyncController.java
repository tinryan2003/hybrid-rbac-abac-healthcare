package org.vgu.policyservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.policyservice.service.OpaSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OPA Sync Management Controller
 * Provides admin endpoints for managing policy synchronization with OPA
 */
@RestController
@RequestMapping("/policies/opa-sync")
@RequiredArgsConstructor
@Slf4j
public class OpaSyncController {

    private final OpaSyncService opaSyncService;

    /**
     * Get OPA sync status and statistics
     * GET /policies/opa-sync/status
     */
    @GetMapping("/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = opaSyncService.getSyncStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Check if OPA server is healthy
     * GET /policies/opa-sync/health
     */
    @GetMapping("/health")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkOpaHealth() {
        boolean healthy = opaSyncService.isOpaHealthy();
        return ResponseEntity.ok(Map.of(
                "opa_healthy", healthy,
                "message", healthy ? "OPA is reachable" : "OPA is not reachable"));
    }

    /**
     * Manually trigger full resync of all enabled policies
     * POST /policies/opa-sync/resync
     */
    @PostMapping("/resync")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerResync() {
        log.info("[OPA] Manual OPA resync triggered via API");

        try {
            opaSyncService.forceResync();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Full resync completed successfully"));
        } catch (Exception e) {
            log.error("[OPA] Manual resync failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Resync failed: " + e.getMessage()));
        }
    }
}
