package com.lifelink.repository;

import com.lifelink.model.Appointment;
import com.lifelink.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDonorIdOrderByBookedAtDesc(Long donorId);

    List<Appointment> findByCampIdOrderByBookedAtAsc(Long campId);

    Optional<Appointment> findByCampIdAndDonorIdAndStatus(Long campId, Long donorId, AppointmentStatus status);

    long countByCampIdAndStatus(Long campId, AppointmentStatus status);

    long countByDonorIdAndStatus(Long donorId, AppointmentStatus status);
}
