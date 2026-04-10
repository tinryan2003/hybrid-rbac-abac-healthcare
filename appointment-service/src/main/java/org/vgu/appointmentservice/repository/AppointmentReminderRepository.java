package org.vgu.appointmentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.appointmentservice.model.AppointmentReminder;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {
    List<AppointmentReminder> findByAppointmentId(Long appointmentId);
    List<AppointmentReminder> findBySent(Boolean sent);
    List<AppointmentReminder> findByReminderTimeBefore(LocalDateTime time);
}
