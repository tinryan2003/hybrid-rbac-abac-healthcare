package org.vgu.patientservice.repository;

import org.vgu.patientservice.model.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatient_PatientIdOrderByCreationDateDesc(Long patientId);
    List<MedicalHistory> findTop10ByPatient_PatientIdOrderByCreationDateDesc(Long patientId);
}
