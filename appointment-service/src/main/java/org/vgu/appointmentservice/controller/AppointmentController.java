package org.vgu.appointmentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.vgu.appointmentservice.dto.*;
import org.vgu.appointmentservice.service.AppointmentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    /** List all appointments with optional filters: status, patientId, doctorId, date */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AppointmentResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String date) {
        log.info("GET /appointments status={} patientId={} doctorId={} date={}", status, patientId, doctorId, date);
        return ResponseEntity.ok(appointmentService.getAll(status, patientId, doctorId, date));
    }

    /** Get appointments created by the currently authenticated user (patient self-booking) */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        log.info("GET /appointments/me keycloakId={}", keycloakId);
        return ResponseEntity.ok(appointmentService.getByCreatedBy(keycloakId));
    }

    /** Get appointment by ID */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    /** Get appointment change history */
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AppointmentHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getHistory(id));
    }

    /** Book a new appointment — Patient, Receptionist, Billing Clerk */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> book(
            @Valid @RequestBody AppointmentRequest request,
            Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        log.info("POST /appointments patientId={} doctorId={}", request.getPatientId(), request.getDoctorId());
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.book(request, keycloakId));
    }

    /** Confirm appointment — Doctor, Nurse */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable Long id, Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        log.info("PUT /appointments/{}/confirm", id);
        return ResponseEntity.ok(appointmentService.confirm(id, keycloakId));
    }

    /** Reject appointment — Doctor, Nurse */
    @PutMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        String reason = body != null ? body.get("reason") : null;
        log.info("PUT /appointments/{}/reject reason={}", id, reason);
        return ResponseEntity.ok(appointmentService.reject(id, keycloakId, reason));
    }

    /** Cancel appointment — Patient, Receptionist, Doctor */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        String reason = body != null ? body.get("reason") : null;
        log.info("PUT /appointments/{}/cancel reason={}", id, reason);
        return ResponseEntity.ok(appointmentService.cancel(id, keycloakId, reason));
    }

    /** Reschedule appointment — Patient, Receptionist, Doctor, Nurse */
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRescheduleRequest request,
            Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        log.info("PUT /appointments/{}/reschedule newDate={}", id, request.getNewDate());
        return ResponseEntity.ok(appointmentService.reschedule(id, request, keycloakId));
    }

    /** Complete appointment — Doctor */
    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable Long id, Authentication authentication) {
        String keycloakId = ((Jwt) authentication.getPrincipal()).getSubject();
        log.info("PUT /appointments/{}/complete", id);
        return ResponseEntity.ok(appointmentService.complete(id, keycloakId));
    }
}
