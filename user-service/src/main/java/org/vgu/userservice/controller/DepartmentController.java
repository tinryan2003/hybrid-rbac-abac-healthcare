package org.vgu.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.vgu.userservice.dto.DepartmentRequest;
import org.vgu.userservice.dto.DepartmentResponse;
import org.vgu.userservice.service.DepartmentService;

import java.util.List;

@RestController
@RequestMapping("/users/departments")   // Gateway strips /api
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long departmentId) {
        DepartmentResponse department = departmentService.getDepartmentById(departmentId);
        return ResponseEntity.ok(department);
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByHospital(@PathVariable String hospitalId) {
        List<DepartmentResponse> departments = departmentService.getDepartmentsByHospital(hospitalId);
        return ResponseEntity.ok(departments);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentRequest request) {
        DepartmentResponse created = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long departmentId,
            @RequestBody DepartmentRequest request
    ) {
        DepartmentResponse updated = departmentService.updateDepartment(departmentId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }
}
