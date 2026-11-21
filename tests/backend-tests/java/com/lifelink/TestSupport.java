package com.lifelink;

import com.lifelink.dto.BloodRequestForm;
import com.lifelink.dto.DonorRegistrationForm;
import com.lifelink.model.*;
import com.lifelink.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/** Test helper: builds users and forms. Picked up by component scanning (test classpath only). */
@Component
public class TestSupport {

    public static final String PASSWORD = "Test@12345";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public TestSupport(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User user(Role role) {
        User u = new User();
        u.setFullName("Test " + role.getLabel());
        u.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local");
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setRole(role);
        return users.save(u);
    }

    public static String uniquePhone() {
        long n = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L);
        return "9" + String.format("%09d", n);
    }

    public static DonorRegistrationForm donorForm(BloodGroup bg, String city, LocalDate lastDonation) {
        DonorRegistrationForm f = new DonorRegistrationForm();
        f.setFullName("Test Donor");
        f.setEmail("donor-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local");
        f.setPassword(PASSWORD);
        f.setConfirmPassword(PASSWORD);
        f.setDateOfBirth(LocalDate.now().minusYears(30));
        f.setGender(Gender.FEMALE);
        f.setBloodGroup(bg);
        f.setPhone(uniquePhone());
        f.setCity(city);
        f.setLastDonationDate(lastDonation);
        f.setAvailable(true);
        f.setContactPreference(ContactPreference.PHONE_AND_EMAIL);
        return f;
    }

    public static BloodRequestForm requestForm(BloodGroup bg, ComponentType comp, int units, Urgency urgency) {
        BloodRequestForm f = new BloodRequestForm();
        f.setPatientReference("P-" + UUID.randomUUID().toString().substring(0, 4));
        f.setBloodGroup(bg);
        f.setComponent(comp);
        f.setUnits(units);
        f.setHospital("Test Hospital");
        f.setCity("Bengaluru");
        f.setContactPhone("9876543210");
        f.setRequiredDate(LocalDate.now().plusDays(1));
        f.setUrgency(urgency);
        return f;
    }
}
