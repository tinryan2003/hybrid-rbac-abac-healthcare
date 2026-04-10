package org.vgu.labservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.labservice.service.LabOrderService;

import java.util.Map;

/**
 * Policy Information Point (PIP) - Resource attributes for ABAC (lab_order).
 */
@RestController
@RequestMapping("/lab/orders/pip")
@RequiredArgsConstructor
@Slf4j
public class PipController {

    private final LabOrderService labOrderService;

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable Long resourceId) {
        log.debug("PIP: get resource attributes for lab_order resourceId={}", resourceId);
        return labOrderService.getPipResourceAttributes(resourceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
