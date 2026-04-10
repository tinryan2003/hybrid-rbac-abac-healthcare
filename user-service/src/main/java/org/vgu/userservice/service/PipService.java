package org.vgu.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.userservice.model.*;
import org.vgu.userservice.repository.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Policy Information Point (PIP) - provides subject attributes for ABAC.
 * Resolves user by Keycloak ID to any role (Doctor, Nurse, Admin, LabTechnician, Pharmacist, Receptionist, BillingClerk)
 * and returns ABAC attributes (hospital_id, department_id, position_level, job_title, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PipService {

    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final AdminRepository adminRepository;
    private final LabTechnicianRepository labTechnicianRepository;
    private final PharmacistRepository pharmacistRepository;
    private final ReceptionistRepository receptionistRepository;
    private final BillingClerkRepository billingClerkRepository;

    /**
     * Get subject attributes for authorization (department_id, hospital_id, position_level, job_title).
     * Tries all role repositories in order: Doctor, Nurse, Admin, LabTechnician, Pharmacist, Receptionist, BillingClerk.
     */
    public Optional<Map<String, Object>> getSubjectAttributes(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return Optional.empty();
        }

        // Try Doctor
        Optional<Doctor> doctor = doctorRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (doctor.isPresent()) {
            return Optional.of(mapDoctorAttributes(doctor.get()));
        }

        // Try Nurse
        Optional<Nurse> nurse = nurseRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (nurse.isPresent()) {
            return Optional.of(mapNurseAttributes(nurse.get()));
        }

        // Try Admin
        Optional<Admin> admin = adminRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (admin.isPresent()) {
            return Optional.of(mapAdminAttributes(admin.get()));
        }

        // Try LabTechnician
        Optional<LabTechnician> labTech = labTechnicianRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (labTech.isPresent()) {
            return Optional.of(mapLabTechnicianAttributes(labTech.get()));
        }

        // Try Pharmacist
        Optional<Pharmacist> pharmacist = pharmacistRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (pharmacist.isPresent()) {
            return Optional.of(mapPharmacistAttributes(pharmacist.get()));
        }

        // Try Receptionist
        Optional<Receptionist> receptionist = receptionistRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (receptionist.isPresent()) {
            return Optional.of(mapReceptionistAttributes(receptionist.get()));
        }

        // Try BillingClerk
        Optional<BillingClerk> billingClerk = billingClerkRepository.findByUser_KeycloakUserId(keycloakUserId);
        if (billingClerk.isPresent()) {
            return Optional.of(mapBillingClerkAttributes(billingClerk.get()));
        }

        log.debug("PIP: no subject profile found for keycloakUserId={}", keycloakUserId);
        return Optional.empty();
    }

    /**
     * Get resource attributes for a staff user record (when user profile is the TARGET resource).
     * Used by PIP when object="user" or "staff_record".
     * Returns department_id, hospital_id, job_title of the target user, plus sensitivity_level.
     */
    public Optional<Map<String, Object>> getResourceAttributes(String keycloakUserId) {
        return getSubjectAttributes(keycloakUserId).map(attrs -> {
            // User records are NORMAL sensitivity by default
            attrs.put("sensitivity_level", "NORMAL");
            // owner_id = the user themselves (for OWNER_IS_USER constraint)
            attrs.put("owner_id", keycloakUserId);
            return attrs;
        });
    }

    private Map<String, Object> mapDoctorAttributes(Doctor d) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "DOCTOR");
        if (d.getDepartment() != null) {
            attrs.put("department_id", String.valueOf(d.getDepartment().getDepartmentId()));
        }
        if (d.getHospitalId() != null) {
            attrs.put("hospital_id", d.getHospitalId());
        }
        if (d.getPositionLevel() != null) {
            attrs.put("position_level", d.getPositionLevel());
        }
        return attrs;
    }

    private Map<String, Object> mapNurseAttributes(Nurse n) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "NURSE");
        if (n.getDepartment() != null) {
            attrs.put("department_id", String.valueOf(n.getDepartment().getDepartmentId()));
        }
        if (n.getHospitalId() != null) {
            attrs.put("hospital_id", n.getHospitalId());
        }
        if (n.getPositionLevel() != null) {
            attrs.put("position_level", n.getPositionLevel());
        }
        return attrs;
    }

    private Map<String, Object> mapAdminAttributes(Admin a) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "ADMIN");
        if (a.getHospitalId() != null) {
            attrs.put("hospital_id", a.getHospitalId());
        }
        if (a.getAdminLevel() != null) {
            attrs.put("position_level", a.getAdminLevel() == Admin.AdminLevel.SYSTEM ? 3 : (a.getAdminLevel() == Admin.AdminLevel.HOSPITAL ? 2 : 1));
        }
        return attrs;
    }

    private Map<String, Object> mapLabTechnicianAttributes(LabTechnician lt) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "LAB_TECH");
        if (lt.getDepartment() != null) {
            attrs.put("department_id", String.valueOf(lt.getDepartment().getDepartmentId()));
        }
        if (lt.getHospitalId() != null) {
            attrs.put("hospital_id", lt.getHospitalId());
        }
        if (lt.getSpecialization() != null) {
            attrs.put("specialization", lt.getSpecialization());
        }
        attrs.put("position_level", 1); // default: no seniority levels for lab techs
        return attrs;
    }

    private Map<String, Object> mapPharmacistAttributes(Pharmacist p) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "PHARMACIST");
        if (p.getHospitalId() != null) {
            attrs.put("hospital_id", p.getHospitalId());
        }
        if (p.getLicenseNumber() != null) {
            attrs.put("license_number", p.getLicenseNumber());
        }
        attrs.put("position_level", 1); // default: no seniority levels for pharmacists
        return attrs;
    }

    private Map<String, Object> mapReceptionistAttributes(Receptionist r) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "RECEPTIONIST");
        if (r.getHospitalId() != null) {
            attrs.put("hospital_id", r.getHospitalId());
        }
        attrs.put("position_level", 1); // default: no seniority levels for receptionists
        return attrs;
    }

    private Map<String, Object> mapBillingClerkAttributes(BillingClerk bc) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("job_title", "BILLING_CLERK");
        if (bc.getHospitalId() != null) {
            attrs.put("hospital_id", bc.getHospitalId());
        }
        attrs.put("position_level", 1); // default: no seniority levels for billing clerks
        return attrs;
    }
}
