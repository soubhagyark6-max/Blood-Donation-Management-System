package com.lifelink.service;

import com.lifelink.exception.BusinessException;
import com.lifelink.exception.NotFoundException;
import com.lifelink.model.*;
import com.lifelink.repository.AppointmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointments;
    private final CampService camps;
    private final DonorService donors;
    private final NotificationService notifications;
    private final AuditService audit;

    public AppointmentService(AppointmentRepository appointments, CampService camps, DonorService donors,
                              NotificationService notifications, AuditService audit) {
        this.appointments = appointments;
        this.camps = camps;
        this.donors = donors;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public Appointment book(Long campId, User user) {
        Donor donor = donors.getByUser(user);
        DonationCamp camp = camps.get(campId);
        if (!camp.isActive()) {
            throw new BusinessException("This camp has been cancelled");
        }
        if (camp.getCampDate().isBefore(LocalDate.now())) {
            throw new BusinessException("This camp date has already passed");
        }
        if (appointments.findByCampIdAndDonorIdAndStatus(campId, donor.getId(), AppointmentStatus.BOOKED).isPresent()) {
            throw new BusinessException("You already have a booking for this camp");
        }
        long booked = appointments.countByCampIdAndStatus(campId, AppointmentStatus.BOOKED);
        if (booked >= camp.getCapacity()) {
            throw new BusinessException("This camp is fully booked");
        }
        Appointment a = new Appointment();
        a.setDonor(donor);
        a.setCamp(camp);
        a.setReference("LL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        appointments.save(a);
        notifications.notify(user, NotificationType.APPOINTMENT,
                "Appointment booked for " + camp.getName() + " on " + camp.getCampDate() + " at " + camp.getStartTime()
                        + ". Reference: " + a.getReference(), "/appointments");
        audit.log(user.getEmail(), "BOOK_APPOINTMENT", a.getReference());
        return a;
    }

    @Transactional
    public void cancel(Long appointmentId, User user) {
        Appointment a = appointments.findById(appointmentId).orElseThrow(() -> new NotFoundException("Appointment not found"));
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        boolean owner = a.getDonor().getUser() != null && a.getDonor().getUser().getId().equals(user.getId());
        if (!staff && !owner) {
            throw new AccessDeniedException("You can only cancel your own appointments");
        }
        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessException("Only booked appointments can be cancelled");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        appointments.save(a);
        audit.log(user.getEmail(), "CANCEL_APPOINTMENT", a.getReference());
    }

    /** Staff sets COMPLETED or NO_SHOW after the camp. */
    @Transactional
    public void mark(Long appointmentId, AppointmentStatus status, User staff) {
        if (status != AppointmentStatus.COMPLETED && status != AppointmentStatus.NO_SHOW) {
            throw new BusinessException("Invalid status");
        }
        Appointment a = appointments.findById(appointmentId).orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessException("Only booked appointments can be updated");
        }
        a.setStatus(status);
        appointments.save(a);
        audit.log(staff.getEmail(), "APPOINTMENT_" + status, a.getReference());
    }

    @Transactional(readOnly = true)
    public List<Appointment> forDonor(User user) {
        return appointments.findByDonorIdOrderByBookedAtDesc(donors.getByUser(user).getId());
    }

    @Transactional(readOnly = true)
    public List<Appointment> forCamp(Long campId) {
        return appointments.findByCampIdOrderByBookedAtAsc(campId);
    }

    @Transactional(readOnly = true)
    public long bookedCount(Long campId) {
        return appointments.countByCampIdAndStatus(campId, AppointmentStatus.BOOKED);
    }

    @Transactional(readOnly = true)
    public Appointment getOwned(Long id, User user) {
        Appointment a = appointments.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found"));
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        boolean owner = a.getDonor().getUser() != null && a.getDonor().getUser().getId().equals(user.getId());
        if (!staff && !owner) {
            throw new AccessDeniedException("Not your appointment");
        }
        return a;
    }
}
