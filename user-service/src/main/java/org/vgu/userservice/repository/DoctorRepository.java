package org.vgu.userservice.repository;

import org.vgu.userservice.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<Doctor> findByUser_UserId(Long userId);
    List<Doctor> findByDepartment_DepartmentId(Long departmentId);
    List<Doctor> findByHospitalId(String hospitalId);
    List<Doctor> findByIsActiveTrue();
    
    @Query("SELECT d FROM Doctor d WHERE d.department.departmentId = :departmentId AND d.isActive = true")
    List<Doctor> findActiveDoctorsByDepartment(@Param("departmentId") Long departmentId);
}
