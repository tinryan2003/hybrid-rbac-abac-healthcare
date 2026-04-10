package org.vgu.billingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.vgu.billingservice.dto.InvoiceRequest;
import org.vgu.billingservice.dto.InvoiceResponse;
import org.vgu.billingservice.service.InvoiceService;

import java.util.List;

@RestController
@RequestMapping("/billing/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{invoiceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long invoiceId) {
        log.info("Fetching invoice with ID: {}", invoiceId);
        InvoiceResponse invoice = invoiceService.getInvoiceById(invoiceId);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> getInvoiceByInvoiceNumber(@PathVariable String invoiceNumber) {
        log.info("Fetching invoice with number: {}", invoiceNumber);
        InvoiceResponse invoice = invoiceService.getInvoiceByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByPatient(@PathVariable Long patientId) {
        log.info("Fetching invoices for patient ID: {}", patientId);
        List<InvoiceResponse> invoices = invoiceService.getInvoicesByPatient(patientId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable String status) {
        log.info("Fetching invoices with status: {}", status);
        List<InvoiceResponse> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByHospital(@PathVariable String hospitalId) {
        log.info("Fetching invoices for hospital: {}", hospitalId);
        List<InvoiceResponse> invoices = invoiceService.getInvoicesByHospital(hospitalId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        log.info("Fetching all invoices");
        List<InvoiceResponse> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceRequest request,
            Authentication authentication) {
        log.info("Creating invoice for patient ID: {}", request.getPatientId());
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakUserId = jwt.getSubject();
        InvoiceResponse created = invoiceService.createInvoice(request, keycloakUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{invoiceId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> updateInvoiceStatus(
            @PathVariable Long invoiceId,
            @RequestParam String status) {
        log.info("Updating invoice {} status to {}", invoiceId, status);
        InvoiceResponse updated = invoiceService.updateInvoiceStatus(invoiceId, status);
        return ResponseEntity.ok(updated);
    }
}
