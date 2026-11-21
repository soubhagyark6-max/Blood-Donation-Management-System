package com.lifelink.service;

import com.lifelink.dto.DonationForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.*;
import com.lifelink.repository.DonationRepository;
import com.lifelink.repository.DonorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DonationService {

    private final DonationRepository donations;
    private final DonorRepository donors;
    private final InventoryService inventory;
    private final NotificationService notifications;
    private final AuditService audit;

    public DonationService(DonationRepository donations, DonorRepository donors, InventoryService inventory,
                           NotificationService notifications, AuditService audit) {
        this.donations = donations;
        this.donors = donors;
        this.inventory = inventory;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Donation> history(Long donorId) {
        return donations.findByDonorIdOrderByDonationDateDesc(donorId);
    }

    /** A donor adds an entry to their own history. It is marked as self-reported (not staff verified). */
    @Transactional
    public Donation recordBySelf(Donor donor, DonationForm f, User actor) {
        validate(donor, f);
        Donation d = build(donor, f, actor, false);
        donations.save(d);
        updateLastDonation(donor, f);
        audit.log(actor.getEmail(), "DONOR_SELF_DONATION", "Donor #" + donor.getId());
        return d;
    }

    /** Staff records a donation. Optionally the units are added to inventory in the same transaction. */
    @Transactional
    public Donation recordByStaff(Donor donor, DonationForm f, User actor) {
        validate(donor, f);
        Donation d = build(donor, f, actor, true);
        donations.save(d);
        updateLastDonation(donor, f);
        if (f.isAddToInventory()) {
            inventory.receive(donor.getBloodGroup(), f.getComponent(), f.getUnits(), f.getDonationDate(),
                    f.getExpiryDate(), "From donation #" + d.getId() + " (donor #" + donor.getId() + ")",
                    actor.getEmail(), false);
        }
        if (donor.getUser() != null) {
            notifications.notify(donor.getUser(), NotificationType.SYSTEM,
                    "Thank you! Your donation on " + f.getDonationDate() + " was recorded by the blood bank.", "/donor/history");
        }
        audit.log(actor.getEmail(), "RECORD_DONATION", "Donor #" + donor.getId() + ", donation #" + d.getId());
        return d;
    }

    private void validate(Donor donor, DonationForm f) {
        if (f.getDonationDate().isBefore(donor.getDateOfBirth())) {
            throw new BusinessException("Donation date cannot be before the date of birth");
        }
    }

    private Donation build(Donor donor, DonationForm f, User actor, boolean verified) {
        Donation d = new Donation();
        d.setDonor(donor);
        d.setDonationDate(f.getDonationDate());
        d.setComponent(f.getComponent());
        d.setUnits(f.getUnits());
        d.setLocation(Texts.blankToNull(f.getLocation()));
        d.setNotes(Texts.blankToNull(f.getNotes()));
        d.setStaffVerified(verified);
        d.setRecordedBy(actor);
        return d;
    }

    private void updateLastDonation(Donor donor, DonationForm f) {
        if (donor.getLastDonationDate() == null || f.getDonationDate().isAfter(donor.getLastDonationDate())) {
            donor.setLastDonationDate(f.getDonationDate());
            donors.save(donor);
        }
    }
}
