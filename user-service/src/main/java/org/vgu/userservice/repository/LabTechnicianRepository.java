package org.vgu.userservice.repository;

import org.vgu.userservice.model.LabTechnician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTechnicianRepository extends JpaRepository<LabTechnician, Long> {
    
    Optional<LabTechnician> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<LabTechnician> findByUser_UserId(Long userId);
    List<LabTechnician> findByDepartment_DepartmentId(Long departmentId);
    List<LabTechnician> findByHospitalId(String hospitalId);
    List<LabTechnician> findByIsActiveTrue();
    
    @Query("SELECT lt FROM LabTechnician lt WHERE lt.department.departmentId = :departmentId AND lt.isActive = true")
    List<LabTechnician> findActiveLabTechniciansByDepartment(@Param("departmentId") Long departmentId);
}
