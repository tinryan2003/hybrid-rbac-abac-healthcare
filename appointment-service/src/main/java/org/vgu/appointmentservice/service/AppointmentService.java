package org.vgu.appointmentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.appointmentservice.dto.*;
import org.vgu.appointmentservice.model.Appointment;
import org.vgu.appointmentservice.model.AppointmentHistory;
import org.vgu.appointmentservice.repository.AppointmentHistoryRepository;
import org.vgu.appointmentservice.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository historyRepository;

    // ─────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────

    public List<AppointmentResponse> getAll(String status, Long patientId, Long doctorId, String date) {
        List<Appointment> results;
        if (patientId != null) {
            results = appointmentRepository.findByPatientId(patientId);
            if (status != null) results = results.stream().filter(a -> status.equals(a.getStatus())).collect(Collectors.toList());
        } else if (doctorId != null) {
            results = appointmentRepository.findByDoctorId(doctorId);
            if (status != null) results = results.stream().filter(a -> status.equals(a.getStatus())).collect(Collectors.toList());
        } else if (status != null) {
            results = appointmentRepository.findByStatus(status);
        } else if (date != null) {
            results = appointmentRepository.findByAppointmentDate(LocalDate.parse(date));
        } else {
            results = appointmentRepository.findAll();
        }
        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getByCreatedBy(String keycloakId) {
        return appointmentRepository.findByCreatedByKeycloakId(keycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AppointmentResponse getById(Long id) {
        return toResponse(find(id));
    }

    public List<AppointmentHistoryResponse> getHistory(Long appointmentId) {
        return historyRepository.findByAppointmentId(appointmentId)
                .stream().map(this::toHistoryResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────
    // Mutations
    // ─────────────────────────────────────────

    @Transactional
    public AppointmentResponse book(AppointmentRequest req, String keycloakUserId) {
        log.info("Booking appointment for patientId={}, doctorId={}", req.getPatientId(), req.getDoctorId());
        Appointment a = new Appointment();
        a.setDoctorId(req.getDoctorId());
        a.setPatientId(req.getPatientId());
        a.setDoctorSpecialization(req.getDoctorSpecialization());
        a.setAppointmentDate(req.getAppointmentDate());
        a.setAppointmentTime(req.getAppointmentTime());
        a.setDurationMinutes(req.getDurationMinutes() != null ? req.getDurationMinutes() : 30);
        a.setReason(req.getReason());
        a.setNotes(req.getNotes());
        a.setStatus("PENDING");
        a.setHospitalId(req.getHospitalId() != null ? req.getHospitalId() : "HOSPITAL_A");
        a.setDepartmentId(req.getDepartmentId());
        a.setCreatedByKeycloakId(keycloakUserId);
        Appointment saved = appointmentRepository.save(a);
        recordHistory(saved.getAppointmentId(), keycloakUserId, "CREATED", null, "PENDING",
                null, null, saved.getAppointmentDate(), saved.getAppointmentTime(), req.getReason(), null);
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse confirm(Long id, String keycloakUserId) {
        Appointment a = find(id);
        String prev = a.getStatus();
        a.setStatus("CONFIRMED");
        a.setApproveDate(LocalDateTime.now());
        Appointment saved = appointmentRepository.save(a);
        recordHistory(id, keycloakUserId, "CONFIRMED", prev, "CONFIRMED", null, null, null, null, null, null);
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse reject(Long id, String keycloakUserId, String reason) {
        Appointment a = find(id);
        String prev = a.getStatus();
        a.setStatus("CANCELLED");
        a.setCancelDate(LocalDateTime.now());
        Appointment saved = appointmentRepository.save(a);
        recordHistory(id, keycloakUserId, "CANCELLED", prev, "CANCELLED", null, null, null, null, reason, "Rejected by staff");
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, String keycloakUserId, String reason) {
        Appointment a = find(id);
        String prev = a.getStatus();
        a.setStatus("CANCELLED");
        a.setCancelDate(LocalDateTime.now());
        Appointment saved = appointmentRepository.save(a);
        recordHistory(id, keycloakUserId, "CANCELLED", prev, "CANCELLED", null, null, null, null, reason, null);
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, AppointmentRescheduleRequest req, String keycloakUserId) {
        Appointment a = find(id);
        String prev = a.getStatus();
        LocalDate prevDate = a.getAppointmentDate();
        java.time.LocalTime prevTime = a.getAppointmentTime();
        a.setAppointmentDate(req.getNewDate());
        a.setAppointmentTime(req.getNewTime());
        a.setStatus("PENDING");
        Appointment saved = appointmentRepository.save(a);
        recordHistory(id, keycloakUserId, "RESCHEDULED", prev, "PENDING",
                prevDate, prevTime, req.getNewDate(), req.getNewTime(), req.getReason(), null);
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse complete(Long id, String keycloakUserId) {
        Appointment a = find(id);
        String prev = a.getStatus();
        a.setStatus("COMPLETED");
        a.setCompletedDate(LocalDateTime.now());
        Appointment saved = appointmentRepository.save(a);
        recordHistory(id, keycloakUserId, "COMPLETED", prev, "COMPLETED", null, null, null, null, null, null);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private Appointment find(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
    }

    private void recordHistory(Long appointmentId, String keycloakId, String action,
                               String prevStatus, String newStatus,
                               LocalDate prevDate, java.time.LocalTime prevTime,
                               LocalDate newDate, java.time.LocalTime newTime,
                               String reason, String notes) {
        AppointmentHistory h = new AppointmentHistory();
        h.setAppointmentId(appointmentId);
        h.setChangedByKeycloakId(keycloakId);
        h.setAction(action);
        h.setPreviousStatus(prevStatus);
        h.setNewStatus(newStatus);
        h.setPreviousDate(prevDate);
        h.setPreviousTime(prevTime);
        h.setNewDate(newDate);
        h.setNewTime(newTime);
        h.setReason(reason);
        h.setNotes(notes);
        historyRepository.save(h);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .appointmentId(a.getAppointmentId())
                .doctorId(a.getDoctorId())
                .patientId(a.getPatientId())
                .doctorSpecialization(a.getDoctorSpecialization())
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .durationMinutes(a.getDurationMinutes())
                .reason(a.getReason())
                .notes(a.getNotes())
                .status(a.getStatus())
                .hospitalId(a.getHospitalId())
                .departmentId(a.getDepartmentId())
                .createDate(a.getCreateDate())
                .approveDate(a.getApproveDate())
                .cancelDate(a.getCancelDate())
                .completedDate(a.getCompletedDate())
                .updatedAt(a.getUpdatedAt())
                .createdByKeycloakId(a.getCreatedByKeycloakId())
                .build();
    }

    private AppointmentHistoryResponse toHistoryResponse(AppointmentHistory h) {
        return AppointmentHistoryResponse.builder()
                .historyId(h.getHistoryId())
                .appointmentId(h.getAppointmentId())
                .changedByKeycloakId(h.getChangedByKeycloakId())
                .action(h.getAction())
                .previousStatus(h.getPreviousStatus())
                .newStatus(h.getNewStatus())
                .previousDate(h.getPreviousDate())
                .previousTime(h.getPreviousTime())
                .newDate(h.getNewDate())
                .newTime(h.getNewTime())
                .reason(h.getReason())
                .notes(h.getNotes())
                .changedAt(h.getChangedAt())
                .build();
    }
}
