package org.vgu.patientservice.repository;

import org.vgu.patientservice.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByKeycloakUserId(String keycloakUserId);
    List<Patient> findByHospitalId(String hospitalId);
    List<Patient> findByLastnameContainingIgnoreCaseOrFirstnameContainingIgnoreCase(String lastname, String firstname);
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    boolean existsByKeycloakUserId(String keycloakUserId);
}
