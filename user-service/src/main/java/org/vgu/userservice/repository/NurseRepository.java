package org.vgu.userservice.repository;

import org.vgu.userservice.model.Nurse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NurseRepository extends JpaRepository<Nurse, Long> {
    Optional<Nurse> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<Nurse> findByUser_UserId(Long userId);
    List<Nurse> findByDepartment_DepartmentId(Long departmentId);
    List<Nurse> findByHospitalId(String hospitalId);
    List<Nurse> findByIsActiveTrue();
    
    @Query("SELECT n FROM Nurse n WHERE n.department.departmentId = :departmentId AND n.isActive = true")
    List<Nurse> findActiveNursesByDepartment(@Param("departmentId") Long departmentId);
}
