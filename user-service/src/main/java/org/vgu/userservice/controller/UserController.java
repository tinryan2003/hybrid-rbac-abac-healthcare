package org.vgu.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.vgu.userservice.dto.DoctorCreateRequest;
import org.vgu.userservice.dto.DoctorResponse;
import org.vgu.userservice.dto.EmployeeResponse;
import org.vgu.userservice.dto.GenericEmployeeCreateRequest;
import org.vgu.userservice.dto.NurseCreateRequest;
import org.vgu.userservice.dto.NurseResponse;
import org.vgu.userservice.dto.UserResponse;
import org.vgu.userservice.dto.WardSummaryDto;
import org.vgu.userservice.service.UserService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users") // Gateway strips /api, so requests arrive as /users
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ============================================
    // User Endpoints
    // ============================================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakUserId = jwt.getSubject();
        UserResponse user = userService.getUserByKeycloakId(keycloakUserId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/keycloak/{keycloakUserId}")
    // No @PreAuthorize - this endpoint is for inter-service calls (audit-service,
    // authorization-service)
    // Configured as permitAll() in SecurityConfig
    public ResponseEntity<UserResponse> getUserByKeycloakId(@PathVariable String keycloakUserId) {
        UserResponse user = userService.getUserByKeycloakId(keycloakUserId);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // ============================================
    // Doctor Endpoints
    // ============================================

    @GetMapping("/doctors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> doctors = userService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/doctors/{doctorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long doctorId) {
        DoctorResponse doctor = userService.getDoctorById(doctorId);
        return ResponseEntity.ok(doctor);
    }

    @GetMapping("/doctors/keycloak/{keycloakUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorResponse> getDoctorByKeycloakId(@PathVariable String keycloakUserId) {
        DoctorResponse doctor = userService.getDoctorByKeycloakId(keycloakUserId);
        return ResponseEntity.ok(doctor);
    }

    @GetMapping("/doctors/department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByDepartment(@PathVariable Long departmentId) {
        List<DoctorResponse> doctors = userService.getDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/doctors/department/{departmentId}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorResponse>> getActiveDoctorsByDepartment(@PathVariable Long departmentId) {
        List<DoctorResponse> doctors = userService.getActiveDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/doctors/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByHospital(@PathVariable String hospitalId) {
        List<DoctorResponse> doctors = userService.getDoctorsByHospital(hospitalId);
        return ResponseEntity.ok(doctors);
    }

    @PostMapping("/doctors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        log.info("Creating doctor: {} {} ({})", request.getFirstName(), request.getLastName(),
                request.getEmailAddress());
        DoctorResponse created = userService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/doctors/{doctorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorResponse> updateDoctor(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorCreateRequest request) {
        log.info("Updating doctor ID: {}", doctorId);
        DoctorResponse updated = userService.updateDoctor(doctorId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/doctors/{doctorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long doctorId) {
        log.info("Deleting doctor ID: {}", doctorId);
        userService.deleteDoctor(doctorId);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // Nurse Endpoints
    // ============================================

    @GetMapping("/nurses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NurseResponse>> getAllNurses() {
        List<NurseResponse> nurses = userService.getAllNurses();
        return ResponseEntity.ok(nurses);
    }

    @GetMapping("/nurses/{nurseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NurseResponse> getNurseById(@PathVariable Long nurseId) {
        NurseResponse nurse = userService.getNurseById(nurseId);
        return ResponseEntity.ok(nurse);
    }

    @GetMapping("/nurses/keycloak/{keycloakUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NurseResponse> getNurseByKeycloakId(@PathVariable String keycloakUserId) {
        NurseResponse nurse = userService.getNurseByKeycloakId(keycloakUserId);
        return ResponseEntity.ok(nurse);
    }

    @GetMapping("/nurses/department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NurseResponse>> getNursesByDepartment(@PathVariable Long departmentId) {
        List<NurseResponse> nurses = userService.getNursesByDepartment(departmentId);
        return ResponseEntity.ok(nurses);
    }

    @GetMapping("/nurses/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NurseResponse>> getNursesByHospital(@PathVariable String hospitalId) {
        List<NurseResponse> nurses = userService.getNursesByHospital(hospitalId);
        return ResponseEntity.ok(nurses);
    }

    @PostMapping("/nurses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NurseResponse> createNurse(@Valid @RequestBody NurseCreateRequest request) {
        log.info("Creating nurse: {} {} ({})", request.getFirstName(), request.getLastName(), request.getEmail());
        NurseResponse created = userService.createNurse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/nurses/{nurseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NurseResponse> updateNurse(
            @PathVariable Long nurseId,
            @Valid @RequestBody NurseCreateRequest request) {
        log.info("Updating nurse ID: {}", nurseId);
        NurseResponse updated = userService.updateNurse(nurseId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/nurses/{nurseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteNurse(@PathVariable Long nurseId) {
        log.info("Deleting nurse ID: {}", nurseId);
        userService.deleteNurse(nurseId);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // Employees (all roles except patient: doctors, nurses, admins)
    // ============================================

    @GetMapping("/employees")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        List<EmployeeResponse> employees = userService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @PostMapping("/employees")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody GenericEmployeeCreateRequest request) {
        log.info("Creating employee: {} {} role={} ({})", request.getFirstName(), request.getLastName(),
                request.getRole(), request.getEmail());

        // Route to appropriate service method based on role
        EmployeeResponse created;
        if ("DOCTOR".equals(request.getRole())) {
            // Convert to DoctorCreateRequest and use createDoctor
            DoctorCreateRequest doctorReq = DoctorCreateRequest.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .emailAddress(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .gender(request.getGender())
                    .birthday(request.getBirthday())
                    .departmentId(request.getDepartmentId())
                    .hospitalId(request.getHospitalId())
                    .positionLevel(request.getPositionLevel())
                    .field(request.getField())
                    .build();
            DoctorResponse doctor = userService.createDoctor(doctorReq);
            created = mapDoctorToEmployeeResponse(doctor);
        } else if ("NURSE".equals(request.getRole())) {
            // Convert to NurseCreateRequest and use createNurse
            NurseCreateRequest nurseReq = NurseCreateRequest.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .gender(request.getGender())
                    .birthday(request.getBirthday())
                    .departmentId(request.getDepartmentId())
                    .hospitalId(request.getHospitalId())
                    .positionLevel(request.getPositionLevel())
                    .build();
            NurseResponse nurse = userService.createNurse(nurseReq);
            created = mapNurseToEmployeeResponse(nurse);
        } else if ("ADMIN".equals(request.getRole())) {
            created = userService.createAdmin(request);
        } else {
            // Generic employee (RECEPTIONIST, LAB_TECH, PHARMACIST, BILLING_CLERK, MANAGER)
            created = userService.createGenericEmployee(request);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/employees/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long entityId,
            @RequestParam String role,
            @Valid @RequestBody GenericEmployeeCreateRequest request) {
        log.info("Updating employee ID: {} role: {}", entityId, role);

        EmployeeResponse updated;
        if ("DOCTOR".equals(role)) {
            DoctorCreateRequest doctorReq = DoctorCreateRequest.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .emailAddress(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .gender(request.getGender())
                    .birthday(request.getBirthday())
                    .departmentId(request.getDepartmentId())
                    .hospitalId(request.getHospitalId())
                    .positionLevel(request.getPositionLevel())
                    .field(request.getField())
                    .build();
            DoctorResponse doctor = userService.updateDoctor(entityId, doctorReq);
            updated = mapDoctorToEmployeeResponse(doctor);
        } else if ("NURSE".equals(role)) {
            NurseCreateRequest nurseReq = NurseCreateRequest.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .gender(request.getGender())
                    .birthday(request.getBirthday())
                    .departmentId(request.getDepartmentId())
                    .hospitalId(request.getHospitalId())
                    .positionLevel(request.getPositionLevel())
                    .build();
            NurseResponse nurse = userService.updateNurse(entityId, nurseReq);
            updated = mapNurseToEmployeeResponse(nurse);
        } else {
            // Generic employee (PHARMACIST, BILLING_CLERK, RECEPTIONIST, LAB_TECH, ADMIN)
            updated = userService.updateGenericEmployee(role, entityId, request);
        }

        return ResponseEntity.ok(updated);
    }

    private EmployeeResponse mapDoctorToEmployeeResponse(DoctorResponse doctor) {
        return EmployeeResponse.builder()
                .role("DOCTOR")
                .entityId(doctor.getDoctorId())
                .userId(doctor.getUserId())
                .keycloakUserId(doctor.getKeycloakUserId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .name(doctor.getName())
                .email(doctor.getEmailAddress())
                .phoneNumber(doctor.getPhoneNumber())
                .gender(doctor.getGender())
                .birthday(doctor.getBirthday())
                .departmentId(doctor.getDepartmentId())
                .departmentName(doctor.getDepartmentName())
                .hospitalId(doctor.getHospitalId())
                .positionLevel(doctor.getPositionLevel())
                .field(doctor.getField())
                .adminLevel(null)
                .isActive(doctor.getIsActive())
                .createdAt(doctor.getCreatedAt())
                .build();
    }

    private EmployeeResponse mapNurseToEmployeeResponse(NurseResponse nurse) {
        return EmployeeResponse.builder()
                .role("NURSE")
                .entityId(nurse.getNurseId())
                .userId(nurse.getUserId())
                .keycloakUserId(nurse.getKeycloakUserId())
                .firstName(nurse.getFirstName())
                .lastName(nurse.getLastName())
                .name(nurse.getName())
                .email(nurse.getEmail())
                .phoneNumber(nurse.getPhoneNumber())
                .gender(nurse.getGender())
                .birthday(nurse.getBirthday())
                .departmentId(nurse.getDepartmentId())
                .departmentName(nurse.getDepartmentName())
                .hospitalId(nurse.getHospitalId())
                .positionLevel(nurse.getPositionLevel())
                .field(null)
                .adminLevel(null)
                .isActive(nurse.getIsActive())
                .createdAt(nurse.getCreatedAt())
                .build();
    }

    // ============================================
    // Wards (distinct from doctors/nurses)
    // ============================================

    @GetMapping("/wards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WardSummaryDto>> getWards(
            @RequestParam(required = false) String hospitalId) {
        List<WardSummaryDto> wards = userService.getWards(hospitalId);
        return ResponseEntity.ok(wards);
    }

    // ============================================
    // Health Check
    // ============================================

    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "UP", "service", "user-service"));
    }
}
