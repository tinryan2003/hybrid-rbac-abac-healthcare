package org.vgu.patientservice.repository;

import org.vgu.patientservice.model.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, Long> {
    List<PatientAllergy> findByPatient_PatientId(Long patientId);
    List<PatientAllergy> findByAllergenContainingIgnoreCase(String allergen);
}
