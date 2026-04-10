package org.vgu.appointmentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.appointmentservice.model.AppointmentHistory;

import java.util.List;

@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, Long> {
    List<AppointmentHistory> findByAppointmentId(Long appointmentId);
    List<AppointmentHistory> findByAction(String action);
}
