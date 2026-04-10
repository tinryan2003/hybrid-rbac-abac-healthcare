package org.vgu.labservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.labservice.model.LabOrder;

import java.util.List;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {
    List<LabOrder> findByPatientId(Long patientId);
    List<LabOrder> findByDoctorId(Long doctorId);
    List<LabOrder> findByStatus(String status);
    List<LabOrder> findByHospitalId(String hospitalId);
}
