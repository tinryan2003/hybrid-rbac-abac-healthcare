package org.vgu.billingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.billingservice.service.InvoiceService;

import java.util.Map;

/**
 * Policy Information Point (PIP) - Resource attributes for ABAC (invoice).
 */
@RestController
@RequestMapping("/billing/invoices/pip")
@RequiredArgsConstructor
@Slf4j
public class PipController {

    private final InvoiceService invoiceService;

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable Long resourceId) {
        log.debug("PIP: get resource attributes for invoice resourceId={}", resourceId);
        return invoiceService.getPipResourceAttributes(resourceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
