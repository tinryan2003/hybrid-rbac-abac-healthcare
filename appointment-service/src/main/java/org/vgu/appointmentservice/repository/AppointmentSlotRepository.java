package org.vgu.appointmentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.appointmentservice.model.AppointmentSlot;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    List<AppointmentSlot> findByDoctorIdAndSlotDate(Long doctorId, LocalDate slotDate);
    List<AppointmentSlot> findByDoctorIdAndSlotDateAndIsAvailable(Long doctorId, LocalDate slotDate, Boolean isAvailable);
    List<AppointmentSlot> findByIsAvailable(Boolean isAvailable);
}
