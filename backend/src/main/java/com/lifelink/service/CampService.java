package com.lifelink.service;

import com.lifelink.dto.CampForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.exception.NotFoundException;
import com.lifelink.model.DonationCamp;
import com.lifelink.model.NotificationType;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.DonationCampRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CampService {

    private final DonationCampRepository camps;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;

    public CampService(DonationCampRepository camps, UserRepository users, NotificationService notifications,
                       AuditService audit) {
        this.camps = camps;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DonationCamp> upcoming() {
        return camps.findByActiveTrueAndCampDateGreaterThanEqualOrderByCampDateAscStartTimeAsc(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<DonationCamp> all() {
        return camps.findAllByOrderByCampDateDesc();
    }

    @Transactional(readOnly = true)
    public long upcomingCount() {
        return camps.countByActiveTrueAndCampDateGreaterThanEqual(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public DonationCamp get(Long id) {
        return camps.findById(id).orElseThrow(() -> new NotFoundException("Camp not found"));
    }

    @Transactional
    public DonationCamp create(CampForm f, String actor) {
        validateTimes(f);
        DonationCamp c = new DonationCamp();
        apply(c, f);
        camps.save(c);
        // Tell donors about the new camp (in-app only).
        for (User u : users.findByRoleIn(List.of(Role.DONOR))) {
            if (u.isEnabled()) {
                notifications.notify(u, NotificationType.CAMP,
                        "New donation camp: " + c.getName() + " on " + c.getCampDate() + " at " + c.getVenue()
                                + ", " + c.getCity(), "/camps");
            }
        }
        audit.log(actor, "CREATE_CAMP", c.getName() + " on " + c.getCampDate());
        return c;
    }

    @Transactional
    public DonationCamp update(Long id, CampForm f, String actor) {
        validateTimes(f);
        DonationCamp c = get(id);
        apply(c, f);
        camps.save(c);
        audit.log(actor, "UPDATE_CAMP", "Camp #" + id);
        return c;
    }

    @Transactional
    public void setActive(Long id, boolean active, String actor) {
        DonationCamp c = get(id);
        c.setActive(active);
        camps.save(c);
        audit.log(actor, active ? "ACTIVATE_CAMP" : "CANCEL_CAMP", "Camp #" + id);
    }

    private void validateTimes(CampForm f) {
        if (!f.getEndTime().isAfter(f.getStartTime())) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private void apply(DonationCamp c, CampForm f) {
        c.setName(f.getName().trim());
        c.setCampDate(f.getCampDate());
        c.setStartTime(f.getStartTime());
        c.setEndTime(f.getEndTime());
        c.setVenue(f.getVenue().trim());
        c.setCity(f.getCity().trim());
        c.setContactName(f.getContactName().trim());
        c.setContactPhone(f.getContactPhone());
        c.setCapacity(f.getCapacity());
        c.setDescription(Texts.blankToNull(f.getDescription()));
    }
}
