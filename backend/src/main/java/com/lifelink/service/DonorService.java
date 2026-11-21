package com.lifelink.service;

import com.lifelink.dto.DonorProfileForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.exception.NotFoundException;
import com.lifelink.model.*;
import com.lifelink.repository.DonorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DonorService {

    private final DonorRepository donors;
    private final AuditService audit;

    @Value("${lifelink.donation-interval-days:90}")
    private int donationIntervalDays;

    public DonorService(DonorRepository donors, AuditService audit) {
        this.donors = donors;
        this.audit = audit;
    }

    public int getDonationIntervalDays() {
        return donationIntervalDays;
    }

    @Transactional(readOnly = true)
    public Donor get(Long id) {
        return donors.findById(id).orElseThrow(() -> new NotFoundException("Donor not found"));
    }

    @Transactional(readOnly = true)
    public Donor getByUser(User user) {
        return donors.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Donor profile not found"));
    }

    @Transactional(readOnly = true)
    public List<Donor> search(BloodGroup bg, String city, String q, Boolean availableOnly, Boolean verifiedOnly) {
        Specification<Donor> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (bg != null) {
                p.add(cb.equal(root.get("bloodGroup"), bg));
            }
            if (Texts.hasText(city)) {
                p.add(cb.like(cb.lower(root.get("city")), Texts.like(city), '\\'));
            }
            if (Texts.hasText(q)) {
                String like = Texts.like(q);
                p.add(cb.or(cb.like(cb.lower(root.get("fullName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(root.get("phone"), like, '\\')));
            }
            if (Boolean.TRUE.equals(availableOnly)) {
                p.add(cb.isTrue(root.get("available")));
            }
            if (Boolean.TRUE.equals(verifiedOnly)) {
                p.add(cb.isTrue(root.get("verified")));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return donors.findAll(spec, Sort.by("fullName"));
    }

    @Transactional
    public void updateProfile(User user, DonorProfileForm f) {
        Donor d = getByUser(user);
        applyForm(d, f);
        audit.log(user.getEmail(), "UPDATE_DONOR_PROFILE", "Donor #" + d.getId());
    }

    /** Staff/admin edit of any donor. */
    @Transactional
    public void updateByStaff(Long donorId, DonorProfileForm f, String actor) {
        Donor d = get(donorId);
        applyForm(d, f);
        audit.log(actor, "STAFF_UPDATE_DONOR", "Donor #" + d.getId());
    }

    private void applyForm(Donor d, DonorProfileForm f) {
        if (donors.existsByPhoneAndIdNot(f.getPhone(), d.getId())) {
            throw new BusinessException("Another donor already uses this phone number");
        }
        if (f.getLastDonationDate() != null && f.getLastDonationDate().isBefore(f.getDateOfBirth())) {
            throw new BusinessException("Last donation date cannot be before the date of birth");
        }
        if (java.time.Period.between(f.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
            throw new BusinessException("Donor accounts are for adults (18 years or older)");
        }
        d.setFullName(f.getFullName().trim());
        d.setDateOfBirth(f.getDateOfBirth());
        d.setGender(f.getGender());
        d.setBloodGroup(f.getBloodGroup());
        d.setPhone(f.getPhone());
        d.setCity(f.getCity().trim());
        d.setAddress(Texts.blankToNull(f.getAddress()));
        d.setLastDonationDate(f.getLastDonationDate());
        d.setAvailable(f.isAvailable());
        d.setContactPreference(f.getContactPreference());
        if (d.getUser() != null) {
            d.getUser().setFullName(d.getFullName());
        }
        donors.save(d);
    }

    /** Staff marks that the donor's details were checked. This is NOT a medical clearance. */
    @Transactional
    public void setVerified(Long donorId, boolean verified, String actor) {
        Donor d = get(donorId);
        d.setVerified(verified);
        donors.save(d);
        audit.log(actor, verified ? "VERIFY_DONOR" : "UNVERIFY_DONOR", "Donor #" + donorId);
    }

    // ---------------------------------------------------------------- smart matching (assistance only)

    public record Match(Donor donor, List<String> reasons, boolean contactViaBloodBankOnly) {
    }

    public record MatchResult(List<Match> matches, int excludedRecentDonation, int excludedUnavailable) {
    }

    /**
     * Preliminary shortlist for a blood request: same blood group, same city, marked available, not donated within
     * the configured reminder interval (when a last-donation date is known). Verified donors are listed first.
     * It does NOT decide medical eligibility or blood compatibility - qualified blood bank professionals do.
     */
    @Transactional(readOnly = true)
    public MatchResult matchFor(BloodRequest request, boolean verifiedOnly) {
        List<Donor> candidates = search(request.getBloodGroup(), request.getCity(), null, null, verifiedOnly);
        List<Match> matches = new ArrayList<>();
        int recent = 0;
        int unavailable = 0;
        LocalDate today = LocalDate.now();
        for (Donor d : candidates) {
            if (!d.isAvailable()) {
                unavailable++;
                continue;
            }
            Long days = d.getLastDonationDate() == null ? null : ChronoUnit.DAYS.between(d.getLastDonationDate(), today);
            if (days != null && days < donationIntervalDays) {
                recent++;
                continue;
            }
            List<String> reasons = new ArrayList<>();
            reasons.add("Same blood group as requested (" + d.getBloodGroup().getLabel() + ")");
            reasons.add("Lives in " + d.getCity() + " (request city: " + request.getCity() + ")");
            reasons.add("Marked as available to donate");
            reasons.add(days == null ? "Last donation date not provided" : "Last donation " + days + " days ago");
            reasons.add(d.isVerified() ? "Details verified by staff" : "Details not yet verified by staff");
            matches.add(new Match(d, reasons, d.getContactPreference() == ContactPreference.NONE));
        }
        matches.sort((a, b) -> Boolean.compare(b.donor().isVerified(), a.donor().isVerified()));
        return new MatchResult(matches, recent, unavailable);
    }

    /** Date after which the reminder interval has passed (guidance only). */
    public LocalDate nextReminderDate(Donor d) {
        return d.getLastDonationDate() == null ? null : d.getLastDonationDate().plusDays(donationIntervalDays);
    }
}
