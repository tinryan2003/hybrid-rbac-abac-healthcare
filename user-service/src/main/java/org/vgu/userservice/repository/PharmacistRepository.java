package org.vgu.userservice.repository;

import org.vgu.userservice.model.Pharmacist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacistRepository extends JpaRepository<Pharmacist, Long> {
    
    Optional<Pharmacist> findByUser_KeycloakUserId(String keycloakUserId);
    Optional<Pharmacist> findByUser_UserId(Long userId);
    List<Pharmacist> findByHospitalId(String hospitalId);
    List<Pharmacist> findByIsActiveTrue();
    Optional<Pharmacist> findByLicenseNumber(String licenseNumber);
}
