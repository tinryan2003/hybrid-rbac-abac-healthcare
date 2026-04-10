package org.vgu.pharmacyservice.repository;

import org.vgu.pharmacyservice.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientId(Long patientId);
    List<Prescription> findByDoctorId(Long doctorId);
    List<Prescription> findByHospitalId(String hospitalId);
    List<Prescription> findByStatus(String status);
    List<Prescription> findByPrescriptionDateBetween(LocalDate start, LocalDate end);
    List<Prescription> findByPatientIdOrderByPrescriptionDateDesc(Long patientId);
}
