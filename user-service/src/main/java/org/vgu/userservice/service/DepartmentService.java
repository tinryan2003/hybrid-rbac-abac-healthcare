package org.vgu.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.userservice.dto.DepartmentRequest;
import org.vgu.userservice.dto.DepartmentResponse;
import org.vgu.userservice.exception.UserNotFoundException;
import org.vgu.userservice.model.Department;
import org.vgu.userservice.repository.DepartmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentResponse getDepartmentById(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new UserNotFoundException("Department not found with ID: " + departmentId));
        return mapToDepartmentResponse(department);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDepartmentResponse)
                .collect(Collectors.toList());
    }

    public List<DepartmentResponse> getDepartmentsByHospital(String hospitalId) {
        return departmentRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToDepartmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.getName());
        department.setLocation(request.getLocation());
        department.setHospitalId(request.getHospitalId());
        department.setDescription(request.getDescription());
        department = departmentRepository.save(department);
        return mapToDepartmentResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, DepartmentRequest request) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new UserNotFoundException("Department not found with ID: " + departmentId));
        department.setName(request.getName());
        department.setLocation(request.getLocation());
        department.setHospitalId(request.getHospitalId());
        department.setDescription(request.getDescription());
        department = departmentRepository.save(department);
        return mapToDepartmentResponse(department);
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new UserNotFoundException("Department not found with ID: " + departmentId);
        }
        departmentRepository.deleteById(departmentId);
    }

    private DepartmentResponse mapToDepartmentResponse(Department department) {
        return DepartmentResponse.builder()
                .departmentId(department.getDepartmentId())
                .name(department.getName())
                .location(department.getLocation())
                .hospitalId(department.getHospitalId())
                .description(department.getDescription())
                .creationDate(department.getCreationDate())
                .build();
    }
}
