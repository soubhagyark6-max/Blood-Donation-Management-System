package com.lifelink.service;

import com.lifelink.dto.DonorRegistrationForm;
import com.lifelink.dto.RecipientRegistrationForm;
import com.lifelink.dto.StaffForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.exception.NotFoundException;
import com.lifelink.model.Donor;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.DonorRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final DonorRepository donors;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, DonorRepository donors, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.donors = donors;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findAll();
    }

    @Transactional
    public User registerDonor(DonorRegistrationForm f) {
        String email = f.getEmail().trim().toLowerCase();
        if (!f.getPassword().equals(f.getConfirmPassword())) {
            throw new BusinessException("Password and confirm password do not match");
        }
        if (users.existsByEmailIgnoreCase(email) || donors.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with this email already exists");
        }
        if (donors.existsByPhone(f.getPhone())) {
            throw new BusinessException("A donor with this phone number is already registered");
        }
        if (Period.between(f.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
            throw new BusinessException("Donor accounts are for adults (18 years or older)");
        }
        if (f.getLastDonationDate() != null && f.getLastDonationDate().isBefore(f.getDateOfBirth())) {
            throw new BusinessException("Last donation date cannot be before the date of birth");
        }
        User u = newUser(f.getFullName(), email, f.getPassword(), Role.DONOR);
        users.save(u);

        Donor d = new Donor();
        d.setUser(u);
        d.setFullName(u.getFullName());
        d.setEmail(email);
        d.setPhone(f.getPhone());
        d.setDateOfBirth(f.getDateOfBirth());
        d.setGender(f.getGender());
        d.setBloodGroup(f.getBloodGroup());
        d.setCity(f.getCity().trim());
        d.setAddress(Texts.blankToNull(f.getAddress()));
        d.setLastDonationDate(f.getLastDonationDate());
        d.setAvailable(f.isAvailable());
        d.setContactPreference(f.getContactPreference());
        donors.save(d);
        audit.log(email, "REGISTER_DONOR", "New donor account");
        return u;
    }

    @Transactional
    public User registerRecipient(RecipientRegistrationForm f) {
        String email = f.getEmail().trim().toLowerCase();
        if (!f.getPassword().equals(f.getConfirmPassword())) {
            throw new BusinessException("Password and confirm password do not match");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with this email already exists");
        }
        User u = newUser(f.getFullName(), email, f.getPassword(), Role.RECIPIENT);
        users.save(u);
        audit.log(email, "REGISTER_RECIPIENT", "New recipient account");
        return u;
    }

    /** Admin creates a staff/admin account. */
    @Transactional
    public User createStaff(StaffForm f, String actor) {
        String email = f.getEmail().trim().toLowerCase();
        if (f.getRole() != Role.STAFF && f.getRole() != Role.ADMIN) {
            throw new BusinessException("Only Staff or Admin accounts can be created here");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with this email already exists");
        }
        User u = newUser(f.getFullName(), email, f.getPassword(), f.getRole());
        users.save(u);
        audit.log(actor, "CREATE_USER", "Created " + f.getRole() + " account " + email);
        return u;
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled, String actor) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (u.getEmail().equalsIgnoreCase(actor)) {
            throw new BusinessException("You cannot disable your own account");
        }
        u.setEnabled(enabled);
        users.save(u);
        audit.log(actor, enabled ? "ENABLE_USER" : "DISABLE_USER", u.getEmail());
    }

    private User newUser(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setFullName(name.trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        return u;
    }
}
