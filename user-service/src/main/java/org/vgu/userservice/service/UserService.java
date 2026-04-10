package org.vgu.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.userservice.dto.DoctorCreateRequest;
import org.vgu.userservice.dto.DoctorResponse;
import org.vgu.userservice.dto.EmployeeResponse;
import org.vgu.userservice.dto.GenericEmployeeCreateRequest;
import org.vgu.userservice.dto.NurseCreateRequest;
import org.vgu.userservice.dto.NurseResponse;
import org.vgu.userservice.dto.UserResponse;
import org.vgu.userservice.dto.WardSummaryDto;
import org.vgu.userservice.exception.DuplicateUserException;
import org.vgu.userservice.exception.KeycloakIntegrationException;
import org.vgu.userservice.exception.UserNotFoundException;
import org.vgu.userservice.model.*;
import org.vgu.userservice.repository.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final AdminRepository adminRepository;
    private final LabTechnicianRepository labTechnicianRepository;
    private final PharmacistRepository pharmacistRepository;
    private final ReceptionistRepository receptionistRepository;
    private final BillingClerkRepository billingClerkRepository;
    private final DepartmentRepository departmentRepository;
    private final KeycloakAdminService keycloakAdminService;

    // ============================================
    // User Operations
    // ============================================

    public UserResponse getUserByKeycloakId(String keycloakUserId) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with Keycloak ID: " + keycloakUserId));
        return mapToUserResponse(user);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return mapToUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // Doctor Operations
    // ============================================

    public DoctorResponse getDoctorByKeycloakId(String keycloakUserId) {
        Doctor doctor = doctorRepository.findByUser_KeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserNotFoundException("Doctor not found with Keycloak ID: " + keycloakUserId));
        return mapToDoctorResponse(doctor);
    }

    public DoctorResponse getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new UserNotFoundException("Doctor not found with ID: " + doctorId));
        return mapToDoctorResponse(doctor);
    }

    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToDoctorResponse)
                .collect(Collectors.toList());
    }

    public List<DoctorResponse> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartment_DepartmentId(departmentId).stream()
                .map(this::mapToDoctorResponse)
                .collect(Collectors.toList());
    }

    public List<DoctorResponse> getActiveDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findActiveDoctorsByDepartment(departmentId).stream()
                .map(this::mapToDoctorResponse)
                .collect(Collectors.toList());
    }

    public List<DoctorResponse> getDoctorsByHospital(String hospitalId) {
        return doctorRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToDoctorResponse)
                .collect(Collectors.toList());
    }

    public DoctorResponse createDoctor(DoctorCreateRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmailAddress()).isPresent()) {
            throw new DuplicateUserException("User with email already exists: " + request.getEmailAddress());
        }

        String keycloakUserId;
        try {
            // Keycloak user attributes: only username, email, firstName, lastName,
            // hospital_id
            Map<String, String> attributes = new HashMap<>();
            if (request.getHospitalId() != null) {
                attributes.put("hospital_id", request.getHospitalId());
            }

            // Create user in Keycloak (align with Keycloak given_name / family_name)
            keycloakUserId = keycloakAdminService.createUser(
                    request.getUsername(),
                    request.getEmailAddress(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName() != null ? request.getLastName() : "",
                    "DOCTOR",
                    attributes);

            log.info("Created Keycloak user for doctor: {} with ID: {}", request.getUsername(), keycloakUserId);
        } catch (KeycloakIntegrationException e) {
            log.error("Failed to create Keycloak user for doctor: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }

        // Create User entity
        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(request.getEmailAddress());
        user.setPhoneNumber(request.getPhoneNumber());
        user = userRepository.save(user);

        // Create Doctor entity
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName() != null ? request.getLastName() : "");
        doctor.setEmail(request.getEmailAddress());
        doctor.setPhoneNumber(request.getPhoneNumber());

        if (request.getGender() != null) {
            try {
                doctor.setGender(Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value: {}, setting to null", request.getGender());
            }
        }

        doctor.setField(request.getField());
        doctor.setBirthday(request.getBirthday());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Department not found with ID: " + request.getDepartmentId()));
            doctor.setDepartment(department);
        }

        doctor.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
        doctor.setPositionLevel(request.getPositionLevel() != null ? request.getPositionLevel() : 2);
        doctor.setIsActive(true);

        doctor = doctorRepository.save(doctor);

        log.info("Created doctor with ID: {} and Keycloak ID: {}", doctor.getDoctorId(), keycloakUserId);
        return mapToDoctorResponse(doctor);
    }

    @Transactional
    public DoctorResponse updateDoctor(Long doctorId, DoctorCreateRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new UserNotFoundException("Doctor not found with ID: " + doctorId));

        // Update basic fields
        if (request.getFirstName() != null)
            doctor.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            doctor.setLastName(request.getLastName());
        if (request.getEmailAddress() != null) {
            doctor.setEmail(request.getEmailAddress());
            doctor.getUser().setEmail(request.getEmailAddress());
        }
        if (request.getPhoneNumber() != null) {
            doctor.setPhoneNumber(request.getPhoneNumber());
            doctor.getUser().setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) {
            try {
                doctor.setGender(Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value: {}", request.getGender());
            }
        }
        if (request.getField() != null)
            doctor.setField(request.getField());
        if (request.getBirthday() != null)
            doctor.setBirthday(request.getBirthday());
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Department not found with ID: " + request.getDepartmentId()));
            doctor.setDepartment(department);
        }
        if (request.getHospitalId() != null)
            doctor.setHospitalId(request.getHospitalId());
        if (request.getPositionLevel() != null)
            doctor.setPositionLevel(request.getPositionLevel());

        doctor = doctorRepository.save(doctor);
        log.info("Updated doctor with ID: {}", doctorId);
        return mapToDoctorResponse(doctor);
    }

    @Transactional
    public void deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new UserNotFoundException("Doctor not found with ID: " + doctorId));

        String keycloakUserId = doctor.getUser().getKeycloakUserId();

        // Delete from Keycloak
        try {
            keycloakAdminService.deleteUser(keycloakUserId);
            log.info("Deleted doctor from Keycloak: {}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to delete doctor from Keycloak: {}", e.getMessage());
            // Continue with DB deletion even if Keycloak fails
        }

        // Delete from database
        doctorRepository.delete(doctor);
        userRepository.delete(doctor.getUser());
        log.info("Deleted doctor with ID: {}", doctorId);
    }

    // ============================================
    // Nurse Operations
    // ============================================

    public NurseResponse getNurseByKeycloakId(String keycloakUserId) {
        Nurse nurse = nurseRepository.findByUser_KeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserNotFoundException("Nurse not found with Keycloak ID: " + keycloakUserId));
        return mapToNurseResponse(nurse);
    }

    public NurseResponse getNurseById(Long nurseId) {
        Nurse nurse = nurseRepository.findById(nurseId)
                .orElseThrow(() -> new UserNotFoundException("Nurse not found with ID: " + nurseId));
        return mapToNurseResponse(nurse);
    }

    public List<NurseResponse> getAllNurses() {
        return nurseRepository.findAll().stream()
                .map(this::mapToNurseResponse)
                .collect(Collectors.toList());
    }

    public List<NurseResponse> getNursesByDepartment(Long departmentId) {
        return nurseRepository.findByDepartment_DepartmentId(departmentId).stream()
                .map(this::mapToNurseResponse)
                .collect(Collectors.toList());
    }

    public List<NurseResponse> getNursesByHospital(String hospitalId) {
        return nurseRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToNurseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all employees (doctors, nurses, admins, lab technicians, pharmacists,
     * receptionists, billing clerks).
     * Excludes patients.
     */
    public List<EmployeeResponse> getAllEmployees() {
        List<EmployeeResponse> list = new ArrayList<>();

        // Add doctors
        for (Doctor d : doctorRepository.findAll()) {
            list.add(mapDoctorToEmployeeResponse(d));
        }

        // Add nurses
        for (Nurse n : nurseRepository.findAll()) {
            list.add(mapNurseToEmployeeResponse(n));
        }

        // Add admins
        for (Admin a : adminRepository.findAll()) {
            list.add(mapAdminToEmployeeResponse(a));
        }

        // Add lab technicians
        for (LabTechnician lt : labTechnicianRepository.findAll()) {
            list.add(mapLabTechnicianToEmployeeResponse(lt));
        }

        // Add pharmacists
        for (Pharmacist p : pharmacistRepository.findAll()) {
            list.add(mapPharmacistToEmployeeResponse(p));
        }

        // Add receptionists
        for (Receptionist r : receptionistRepository.findAll()) {
            list.add(mapReceptionistToEmployeeResponse(r));
        }

        // Add billing clerks
        for (BillingClerk bc : billingClerkRepository.findAll()) {
            list.add(mapBillingClerkToEmployeeResponse(bc));
        }

        list.sort(Comparator.comparing(EmployeeResponse::getName, Comparator.nullsLast(String::compareTo)));
        return list;
    }

    /**
     * Create admin: Keycloak user + User + Admin entity.
     */
    @Transactional
    public EmployeeResponse createAdmin(GenericEmployeeCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateUserException("User with email already exists: " + request.getEmail());
        }
        String keycloakUserId;
        try {
            Map<String, String> attributes = new HashMap<>();
            if (request.getHospitalId() != null) {
                attributes.put("hospital_id", request.getHospitalId());
            }
            keycloakUserId = keycloakAdminService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName() != null ? request.getLastName() : "",
                    "ADMIN",
                    attributes);
            log.info("Created admin in Keycloak: {} ({})", request.getUsername(), keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to create admin in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakIntegrationException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user = userRepository.save(user);

        Admin admin = new Admin();
        admin.setUser(user);
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName() != null ? request.getLastName() : "");
        admin.setEmail(request.getEmail());
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setBirthday(request.getBirthday());
        admin.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : null);
        if (request.getGender() != null) {
            try {
                admin.setGender(Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (request.getAdminLevel() != null && !request.getAdminLevel().isEmpty()) {
            try {
                admin.setAdminLevel(parseAdminLevel(request.getAdminLevel()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        admin = adminRepository.save(admin);
        log.info("Created admin with ID: {} and Keycloak ID: {}", admin.getAdminId(), keycloakUserId);
        return mapAdminToEmployeeResponse(admin);
    }

    /**
     * Create generic employee (LAB_TECH, PHARMACIST, RECEPTIONIST, BILLING_CLERK)
     * Creates user in Keycloak and corresponding entity (LabTechnician, Pharmacist,
     * Receptionist, BillingClerk)
     */
    @Transactional
    public EmployeeResponse createGenericEmployee(GenericEmployeeCreateRequest request) {
        // Validate role
        Set<String> allowedRoles = Set.of("LAB_TECH", "PHARMACIST", "RECEPTIONIST", "BILLING_CLERK");
        if (!allowedRoles.contains(request.getRole())) {
            throw new IllegalArgumentException("Invalid role for generic employee: " + request.getRole() +
                    ". Allowed: " + allowedRoles);
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateUserException("User with email already exists: " + request.getEmail());
        }

        String keycloakUserId;
        try {
            // Keycloak user attributes: only username, email, firstName, lastName,
            // hospital_id
            Map<String, String> attributes = new HashMap<>();
            if (request.getHospitalId() != null) {
                attributes.put("hospital_id", request.getHospitalId());
            }

            // Create user in Keycloak (align with Keycloak given_name / family_name)
            keycloakUserId = keycloakAdminService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName() != null ? request.getLastName() : "",
                    request.getRole(),
                    attributes);

            log.info("Created generic employee in Keycloak: {} ({})", request.getUsername(), keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to create generic employee in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakIntegrationException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }

        // Create User entity
        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user = userRepository.save(user);

        // Create corresponding role entity
        String role = request.getRole();
        EmployeeResponse response;

        if ("LAB_TECH".equals(role)) {
            LabTechnician labTech = new LabTechnician();
            labTech.setUser(user);
            labTech.setFirstName(request.getFirstName());
            labTech.setLastName(request.getLastName() != null ? request.getLastName() : "");
            labTech.setEmail(request.getEmail());
            labTech.setPhoneNumber(request.getPhoneNumber());
            labTech.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    labTech.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid gender for LAB_TECH: {}", request.getGender());
                }
            }
            if (request.getPositionLevel() != null) {
                labTech.setPositionLevel(request.getPositionLevel());
            } else {
                labTech.setPositionLevel(1);
            }
            if (request.getDepartmentId() != null) {
                Department department = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new UserNotFoundException(
                                "Department not found with ID: " + request.getDepartmentId()));
                labTech.setDepartment(department);
            }
            labTech.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
            // Note: GenericEmployeeCreateRequest doesn't have 'field' - specialization can
            // be set later via update
            labTech.setIsActive(true);
            labTech = labTechnicianRepository.save(labTech);
            response = mapLabTechnicianToEmployeeResponse(labTech);

        } else if ("PHARMACIST".equals(role)) {
            Pharmacist pharmacist = new Pharmacist();
            pharmacist.setUser(user);
            pharmacist.setFirstName(request.getFirstName());
            pharmacist.setLastName(request.getLastName() != null ? request.getLastName() : "");
            pharmacist.setEmail(request.getEmail());
            pharmacist.setPhoneNumber(request.getPhoneNumber());
            pharmacist.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    pharmacist.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid gender for PHARMACIST: {}", request.getGender());
                }
            }
            if (request.getPositionLevel() != null) {
                pharmacist.setPositionLevel(request.getPositionLevel());
            } else {
                pharmacist.setPositionLevel(1);
            }
            pharmacist.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
            // Note: GenericEmployeeCreateRequest doesn't have 'field' - licenseNumber can
            // be set later via update
            pharmacist.setIsActive(true);
            pharmacist = pharmacistRepository.save(pharmacist);
            response = mapPharmacistToEmployeeResponse(pharmacist);

        } else if ("RECEPTIONIST".equals(role)) {
            Receptionist receptionist = new Receptionist();
            receptionist.setUser(user);
            receptionist.setFirstName(request.getFirstName());
            receptionist.setLastName(request.getLastName() != null ? request.getLastName() : "");
            receptionist.setEmail(request.getEmail());
            receptionist.setPhoneNumber(request.getPhoneNumber());
            receptionist.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    receptionist.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid gender for RECEPTIONIST: {}", request.getGender());
                }
            }
            if (request.getPositionLevel() != null) {
                receptionist.setPositionLevel(request.getPositionLevel());
            } else {
                receptionist.setPositionLevel(1);
            }
            receptionist.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
            receptionist.setIsActive(true);
            receptionist = receptionistRepository.save(receptionist);
            response = mapReceptionistToEmployeeResponse(receptionist);

        } else if ("BILLING_CLERK".equals(role)) {
            BillingClerk billingClerk = new BillingClerk();
            billingClerk.setUser(user);
            billingClerk.setFirstName(request.getFirstName());
            billingClerk.setLastName(request.getLastName() != null ? request.getLastName() : "");
            billingClerk.setEmail(request.getEmail());
            billingClerk.setPhoneNumber(request.getPhoneNumber());
            billingClerk.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    billingClerk.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid gender for BILLING_CLERK: {}", request.getGender());
                }
            }
            if (request.getPositionLevel() != null) {
                billingClerk.setPositionLevel(request.getPositionLevel());
            } else {
                billingClerk.setPositionLevel(1);
            }
            billingClerk.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
            billingClerk.setIsActive(true);
            billingClerk = billingClerkRepository.save(billingClerk);
            response = mapBillingClerkToEmployeeResponse(billingClerk);

        } else {
            throw new IllegalArgumentException("Unsupported role: " + role);
        }

        log.info("Created {} with entity ID: {} and Keycloak ID: {}", role, response.getEntityId(), keycloakUserId);
        return response;
    }

    /**
     * Update generic employee (Pharmacist, BillingClerk, Receptionist,
     * LabTechnician, Admin).
     * Authorization is handled by Spring Security + OPA policies.
     */
    @Transactional
    public EmployeeResponse updateGenericEmployee(String role, Long entityId, GenericEmployeeCreateRequest request) {
        EmployeeResponse response;

        if ("PHARMACIST".equals(role)) {
            Pharmacist pharmacist = pharmacistRepository.findById(entityId)
                    .orElseThrow(() -> new UserNotFoundException("Pharmacist not found with ID: " + entityId));

            if (request.getFirstName() != null)
                pharmacist.setFirstName(request.getFirstName());
            if (request.getLastName() != null)
                pharmacist.setLastName(request.getLastName());
            if (request.getEmail() != null) {
                pharmacist.setEmail(request.getEmail());
                pharmacist.getUser().setEmail(request.getEmail());
            }
            if (request.getPhoneNumber() != null) {
                pharmacist.setPhoneNumber(request.getPhoneNumber());
                pharmacist.getUser().setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getBirthday() != null)
                pharmacist.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    pharmacist.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender for PHARMACIST update: {}", request.getGender());
                }
            }
            if (request.getHospitalId() != null)
                pharmacist.setHospitalId(request.getHospitalId());
            if (request.getPositionLevel() != null)
                pharmacist.setPositionLevel(request.getPositionLevel());

            pharmacist = pharmacistRepository.save(pharmacist);
            response = mapPharmacistToEmployeeResponse(pharmacist);

        } else if ("BILLING_CLERK".equals(role)) {
            BillingClerk billingClerk = billingClerkRepository.findById(entityId)
                    .orElseThrow(() -> new UserNotFoundException("Billing Clerk not found with ID: " + entityId));

            if (request.getFirstName() != null)
                billingClerk.setFirstName(request.getFirstName());
            if (request.getLastName() != null)
                billingClerk.setLastName(request.getLastName());
            if (request.getEmail() != null) {
                billingClerk.setEmail(request.getEmail());
                billingClerk.getUser().setEmail(request.getEmail());
            }
            if (request.getPhoneNumber() != null) {
                billingClerk.setPhoneNumber(request.getPhoneNumber());
                billingClerk.getUser().setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getBirthday() != null)
                billingClerk.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    billingClerk.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender for BILLING_CLERK update: {}", request.getGender());
                }
            }
            if (request.getHospitalId() != null)
                billingClerk.setHospitalId(request.getHospitalId());
            if (request.getPositionLevel() != null)
                billingClerk.setPositionLevel(request.getPositionLevel());

            billingClerk = billingClerkRepository.save(billingClerk);
            response = mapBillingClerkToEmployeeResponse(billingClerk);

        } else if ("RECEPTIONIST".equals(role)) {
            Receptionist receptionist = receptionistRepository.findById(entityId)
                    .orElseThrow(() -> new UserNotFoundException("Receptionist not found with ID: " + entityId));

            if (request.getFirstName() != null)
                receptionist.setFirstName(request.getFirstName());
            if (request.getLastName() != null)
                receptionist.setLastName(request.getLastName());
            if (request.getEmail() != null) {
                receptionist.setEmail(request.getEmail());
                receptionist.getUser().setEmail(request.getEmail());
            }
            if (request.getPhoneNumber() != null) {
                receptionist.setPhoneNumber(request.getPhoneNumber());
                receptionist.getUser().setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getBirthday() != null)
                receptionist.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    receptionist.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender for RECEPTIONIST update: {}", request.getGender());
                }
            }
            if (request.getHospitalId() != null)
                receptionist.setHospitalId(request.getHospitalId());
            if (request.getPositionLevel() != null)
                receptionist.setPositionLevel(request.getPositionLevel());

            receptionist = receptionistRepository.save(receptionist);
            response = mapReceptionistToEmployeeResponse(receptionist);

        } else if ("LAB_TECH".equals(role)) {
            LabTechnician labTech = labTechnicianRepository.findById(entityId)
                    .orElseThrow(() -> new UserNotFoundException("Lab Technician not found with ID: " + entityId));

            if (request.getFirstName() != null)
                labTech.setFirstName(request.getFirstName());
            if (request.getLastName() != null)
                labTech.setLastName(request.getLastName());
            if (request.getEmail() != null) {
                labTech.setEmail(request.getEmail());
                labTech.getUser().setEmail(request.getEmail());
            }
            if (request.getPhoneNumber() != null) {
                labTech.setPhoneNumber(request.getPhoneNumber());
                labTech.getUser().setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getDepartmentId() != null) {
                Department department = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new UserNotFoundException(
                                "Department not found with ID: " + request.getDepartmentId()));
                labTech.setDepartment(department);
            }
            if (request.getBirthday() != null)
                labTech.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    labTech.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender for LAB_TECH update: {}", request.getGender());
                }
            }
            if (request.getHospitalId() != null)
                labTech.setHospitalId(request.getHospitalId());
            if (request.getPositionLevel() != null)
                labTech.setPositionLevel(request.getPositionLevel());

            labTech = labTechnicianRepository.save(labTech);
            response = mapLabTechnicianToEmployeeResponse(labTech);

        } else if ("ADMIN".equals(role)) {
            Admin admin = adminRepository.findById(entityId)
                    .orElseThrow(() -> new UserNotFoundException("Admin not found with ID: " + entityId));

            if (request.getFirstName() != null)
                admin.setFirstName(request.getFirstName());
            if (request.getLastName() != null)
                admin.setLastName(request.getLastName());
            if (request.getEmail() != null) {
                admin.setEmail(request.getEmail());
                admin.getUser().setEmail(request.getEmail());
            }
            if (request.getPhoneNumber() != null) {
                admin.setPhoneNumber(request.getPhoneNumber());
                admin.getUser().setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getBirthday() != null)
                admin.setBirthday(request.getBirthday());
            if (request.getGender() != null) {
                try {
                    admin.setGender(Gender.valueOf(request.getGender()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender for ADMIN update: {}", request.getGender());
                }
            }
            if (request.getHospitalId() != null)
                admin.setHospitalId(request.getHospitalId());
            if (request.getAdminLevel() != null) {
                try {
                    admin.setAdminLevel(parseAdminLevel(request.getAdminLevel()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid admin level: {}", request.getAdminLevel());
                }
            }

            admin = adminRepository.save(admin);
            response = mapAdminToEmployeeResponse(admin);

        } else {
            throw new IllegalArgumentException("Unsupported role for update: " + role);
        }

        log.info("Updated {} with ID: {}", role, entityId);
        return response;
    }

    /**
     * Returns distinct wards (from doctors and nurses). Ward column removed from
     * DB; returns empty list.
     */
    public List<WardSummaryDto> getWards(String hospitalId) {
        return Collections.emptyList();
    }

    public NurseResponse createNurse(NurseCreateRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateUserException("User with email already exists: " + request.getEmail());
        }

        String keycloakUserId;
        try {
            // Keycloak user attributes: only username, email, firstName, lastName,
            // hospital_id
            Map<String, String> attributes = new HashMap<>();
            if (request.getHospitalId() != null) {
                attributes.put("hospital_id", request.getHospitalId());
            }

            // Create user in Keycloak (align with Keycloak given_name / family_name)
            keycloakUserId = keycloakAdminService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName() != null ? request.getLastName() : "",
                    "NURSE",
                    attributes);

            log.info("Created Keycloak user for nurse: {} with ID: {}", request.getUsername(), keycloakUserId);
        } catch (KeycloakIntegrationException e) {
            log.error("Failed to create Keycloak user for nurse: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }

        // Create User entity
        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user = userRepository.save(user);

        // Create Nurse entity
        Nurse nurse = new Nurse();
        nurse.setUser(user);
        nurse.setFirstName(request.getFirstName());
        nurse.setLastName(request.getLastName() != null ? request.getLastName() : "");
        nurse.setEmail(request.getEmail());
        nurse.setPhoneNumber(request.getPhoneNumber());

        if (request.getGender() != null) {
            try {
                nurse.setGender(Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value: {}, setting to null", request.getGender());
            }
        }

        nurse.setBirthday(request.getBirthday());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Department not found with ID: " + request.getDepartmentId()));
            nurse.setDepartment(department);
        }

        nurse.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
        nurse.setPositionLevel(request.getPositionLevel() != null ? request.getPositionLevel() : 1);
        nurse.setIsActive(true);

        nurse = nurseRepository.save(nurse);

        log.info("Created nurse with ID: {} and Keycloak ID: {}", nurse.getNurseId(), keycloakUserId);
        return mapToNurseResponse(nurse);
    }

    @Transactional
    public NurseResponse updateNurse(Long nurseId, NurseCreateRequest request) {
        Nurse nurse = nurseRepository.findById(nurseId)
                .orElseThrow(() -> new UserNotFoundException("Nurse not found with ID: " + nurseId));

        // Update basic fields
        if (request.getFirstName() != null)
            nurse.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            nurse.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            nurse.setEmail(request.getEmail());
            nurse.getUser().setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            nurse.setPhoneNumber(request.getPhoneNumber());
            nurse.getUser().setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) {
            try {
                nurse.setGender(Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value: {}", request.getGender());
            }
        }
        if (request.getBirthday() != null)
            nurse.setBirthday(request.getBirthday());
        if (request.getFirstName() != null)
            nurse.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            nurse.setLastName(request.getLastName());
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Department not found with ID: " + request.getDepartmentId()));
            nurse.setDepartment(department);
        }
        if (request.getHospitalId() != null)
            nurse.setHospitalId(request.getHospitalId());
        if (request.getPositionLevel() != null)
            nurse.setPositionLevel(request.getPositionLevel());

        nurse = nurseRepository.save(nurse);
        log.info("Updated nurse with ID: {}", nurseId);
        return mapToNurseResponse(nurse);
    }

    @Transactional
    public void deleteNurse(Long nurseId) {
        Nurse nurse = nurseRepository.findById(nurseId)
                .orElseThrow(() -> new UserNotFoundException("Nurse not found with ID: " + nurseId));

        String keycloakUserId = nurse.getUser().getKeycloakUserId();

        // Delete from Keycloak
        try {
            keycloakAdminService.deleteUser(keycloakUserId);
            log.info("Deleted nurse from Keycloak: {}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to delete nurse from Keycloak: {}", e.getMessage());
            // Continue with DB deletion even if Keycloak fails
        }

        // Delete from database
        nurseRepository.delete(nurse);
        userRepository.delete(nurse.getUser());
        log.info("Deleted nurse with ID: {}", nurseId);
    }

    // ============================================
    // Mappers
    // ============================================

    private UserResponse mapToUserResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .userId(user.getUserId())
                .keycloakUserId(user.getKeycloakUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt());

        Long userId = user.getUserId();

        // Try to enrich with staff profile information (jobTitle, hospitalId,
        // positionLevel) for display and ABAC visibility.
        doctorRepository.findByUser_UserId(userId).ifPresent(d -> {
            builder.jobTitle("DOCTOR");
            if (d.getHospitalId() != null) {
                builder.hospitalId(d.getHospitalId());
            }
            if (d.getPositionLevel() != null) {
                builder.positionLevel(d.getPositionLevel());
            }
        });

        nurseRepository.findByUser_UserId(userId).ifPresent(n -> {
            builder.jobTitle("NURSE");
            if (n.getHospitalId() != null) {
                builder.hospitalId(n.getHospitalId());
            }
            if (n.getPositionLevel() != null) {
                builder.positionLevel(n.getPositionLevel());
            }
        });

        adminRepository.findByUser_UserId(userId).ifPresent(a -> {
            builder.jobTitle("ADMIN");
            if (a.getHospitalId() != null) {
                builder.hospitalId(a.getHospitalId());
            }
            if (a.getAdminLevel() != null) {
                int level = a.getAdminLevel() == Admin.AdminLevel.SYSTEM ? 3
                        : (a.getAdminLevel() == Admin.AdminLevel.HOSPITAL ? 2 : 1);
                builder.positionLevel(level);
            }
        });

        labTechnicianRepository.findByUser_UserId(userId).ifPresent(lt -> {
            builder.jobTitle("LAB_TECH");
            if (lt.getHospitalId() != null) {
                builder.hospitalId(lt.getHospitalId());
            }
            // Lab techs currently have a flat seniority model → position_level=1.
            builder.positionLevel(1);
        });

        pharmacistRepository.findByUser_UserId(userId).ifPresent(p -> {
            builder.jobTitle("PHARMACIST");
            if (p.getHospitalId() != null) {
                builder.hospitalId(p.getHospitalId());
            }
            builder.positionLevel(1);
        });

        receptionistRepository.findByUser_UserId(userId).ifPresent(r -> {
            builder.jobTitle("RECEPTIONIST");
            if (r.getHospitalId() != null) {
                builder.hospitalId(r.getHospitalId());
            }
            builder.positionLevel(1);
        });

        billingClerkRepository.findByUser_UserId(userId).ifPresent(bc -> {
            builder.jobTitle("BILLING_CLERK");
            if (bc.getHospitalId() != null) {
                builder.hospitalId(bc.getHospitalId());
            }
            builder.positionLevel(1);
        });

        return builder.build();
    }

    private static String fullName(String firstName, String lastName) {
        if (firstName == null && lastName == null)
            return null;
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    private DoctorResponse mapToDoctorResponse(Doctor doctor) {
        String fn = doctor.getFirstName();
        String ln = doctor.getLastName();
        return DoctorResponse.builder()
                .doctorId(doctor.getDoctorId())
                .userId(doctor.getUser().getUserId())
                .keycloakUserId(doctor.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .gender(doctor.getGender() != null ? doctor.getGender().name() : null)
                .field(doctor.getField())
                .birthday(doctor.getBirthday())
                .emailAddress(doctor.getEmail())
                .phoneNumber(doctor.getPhoneNumber())
                .departmentId(doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentId() : null)
                .departmentName(doctor.getDepartment() != null ? doctor.getDepartment().getName() : null)
                .hospitalId(doctor.getHospitalId())
                .positionLevel(doctor.getPositionLevel())
                .isActive(doctor.getIsActive())
                .createdAt(doctor.getCreatedAt())
                .build();
    }

    private NurseResponse mapToNurseResponse(Nurse nurse) {
        String fn = nurse.getFirstName();
        String ln = nurse.getLastName();
        return NurseResponse.builder()
                .nurseId(nurse.getNurseId())
                .userId(nurse.getUser().getUserId())
                .keycloakUserId(nurse.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .gender(nurse.getGender() != null ? nurse.getGender().name() : null)
                .birthday(nurse.getBirthday())
                .phoneNumber(nurse.getPhoneNumber())
                .email(nurse.getEmail())
                .departmentId(nurse.getDepartment() != null ? nurse.getDepartment().getDepartmentId() : null)
                .departmentName(nurse.getDepartment() != null ? nurse.getDepartment().getName() : null)
                .hospitalId(nurse.getHospitalId())
                .positionLevel(nurse.getPositionLevel())
                .isActive(nurse.getIsActive())
                .createdAt(nurse.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapDoctorToEmployeeResponse(Doctor d) {
        String fn = d.getFirstName();
        String ln = d.getLastName();
        return EmployeeResponse.builder()
                .role("DOCTOR")
                .entityId(d.getDoctorId())
                .userId(d.getUser().getUserId())
                .keycloakUserId(d.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(d.getEmail())
                .phoneNumber(d.getPhoneNumber())
                .gender(d.getGender() != null ? d.getGender().name() : null)
                .birthday(d.getBirthday())
                .departmentId(d.getDepartment() != null ? d.getDepartment().getDepartmentId() : null)
                .departmentName(d.getDepartment() != null ? d.getDepartment().getName() : null)
                .hospitalId(d.getHospitalId())
                .positionLevel(d.getPositionLevel())
                .field(d.getField())
                .adminLevel(null)
                .isActive(d.getIsActive())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapNurseToEmployeeResponse(Nurse n) {
        String fn = n.getFirstName();
        String ln = n.getLastName();
        return EmployeeResponse.builder()
                .role("NURSE")
                .entityId(n.getNurseId())
                .userId(n.getUser().getUserId())
                .keycloakUserId(n.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(n.getEmail())
                .phoneNumber(n.getPhoneNumber())
                .gender(n.getGender() != null ? n.getGender().name() : null)
                .birthday(n.getBirthday())
                .departmentId(n.getDepartment() != null ? n.getDepartment().getDepartmentId() : null)
                .departmentName(n.getDepartment() != null ? n.getDepartment().getName() : null)
                .hospitalId(n.getHospitalId())
                .positionLevel(n.getPositionLevel())
                .field(null)
                .adminLevel(null)
                .isActive(n.getIsActive())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapAdminToEmployeeResponse(Admin a) {
        String fn = a.getFirstName();
        String ln = a.getLastName();
        return EmployeeResponse.builder()
                .role("ADMIN")
                .entityId(a.getAdminId())
                .userId(a.getUser().getUserId())
                .keycloakUserId(a.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(a.getEmail())
                .phoneNumber(a.getPhoneNumber())
                .gender(a.getGender() != null ? a.getGender().name() : null)
                .birthday(a.getBirthday())
                .departmentId(null)
                .departmentName(null)
                .hospitalId(a.getHospitalId())
                .positionLevel(null)
                .field(null)
                .adminLevel(a.getAdminLevel() != null ? a.getAdminLevel().name() : null)
                .isActive(true)
                .createdAt(a.getCreatedAt())
                .build();
    }

    private Admin.AdminLevel parseAdminLevel(String raw) {
        if (raw == null)
            return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty())
            return null;

        // Support INT-backed admin_level (e.g., 3=SYSTEM, 2=HOSPITAL, 1=DEPARTMENT)
        try {
            return Admin.AdminLevel.fromCode(Integer.parseInt(trimmed));
        } catch (NumberFormatException ignored) {
            // Fall back to enum name
        }

        return Admin.AdminLevel.valueOf(trimmed.toUpperCase());
    }

    private EmployeeResponse mapLabTechnicianToEmployeeResponse(LabTechnician lt) {
        String fn = lt.getFirstName();
        String ln = lt.getLastName();
        return EmployeeResponse.builder()
                .role("LAB_TECH")
                .entityId(lt.getLabTechId())
                .userId(lt.getUser().getUserId())
                .keycloakUserId(lt.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(lt.getEmail())
                .phoneNumber(lt.getPhoneNumber())
                .gender(null)
                .birthday(null)
                .departmentId(lt.getDepartment() != null ? lt.getDepartment().getDepartmentId() : null)
                .departmentName(lt.getDepartment() != null ? lt.getDepartment().getName() : null)
                .hospitalId(lt.getHospitalId())
                .positionLevel(null)
                .field(lt.getSpecialization())
                .adminLevel(null)
                .isActive(lt.getIsActive())
                .createdAt(lt.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapPharmacistToEmployeeResponse(Pharmacist p) {
        String fn = p.getFirstName();
        String ln = p.getLastName();
        return EmployeeResponse.builder()
                .role("PHARMACIST")
                .entityId(p.getPharmacistId())
                .userId(p.getUser().getUserId())
                .keycloakUserId(p.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(p.getEmail())
                .phoneNumber(p.getPhoneNumber())
                .gender(null)
                .birthday(null)
                .departmentId(null)
                .departmentName(null)
                .hospitalId(p.getHospitalId())
                .positionLevel(null)
                .field(p.getLicenseNumber())
                .adminLevel(null)
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapReceptionistToEmployeeResponse(Receptionist r) {
        String fn = r.getFirstName();
        String ln = r.getLastName();
        return EmployeeResponse.builder()
                .role("RECEPTIONIST")
                .entityId(r.getReceptionistId())
                .userId(r.getUser().getUserId())
                .keycloakUserId(r.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(r.getEmail())
                .phoneNumber(r.getPhoneNumber())
                .gender(null)
                .birthday(null)
                .departmentId(null)
                .departmentName(null)
                .hospitalId(r.getHospitalId())
                .positionLevel(null)
                .field(null)
                .adminLevel(null)
                .isActive(r.getIsActive())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapBillingClerkToEmployeeResponse(BillingClerk bc) {
        String fn = bc.getFirstName();
        String ln = bc.getLastName();
        return EmployeeResponse.builder()
                .role("BILLING_CLERK")
                .entityId(bc.getBillingClerkId())
                .userId(bc.getUser().getUserId())
                .keycloakUserId(bc.getUser().getKeycloakUserId())
                .firstName(fn)
                .lastName(ln)
                .name(fullName(fn, ln))
                .email(bc.getEmail())
                .phoneNumber(bc.getPhoneNumber())
                .gender(null)
                .birthday(null)
                .departmentId(null)
                .departmentName(null)
                .hospitalId(bc.getHospitalId())
                .positionLevel(null)
                .field(null)
                .adminLevel(null)
                .isActive(bc.getIsActive())
                .createdAt(bc.getCreatedAt())
                .build();
    }
}
