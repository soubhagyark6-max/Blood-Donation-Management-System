package com.lifelink.service;

import com.lifelink.TestSupport;
import com.lifelink.dto.DonorRegistrationForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.*;
import com.lifelink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DonorServiceTests {

    @Autowired UserService userService;
    @Autowired DonorService donorService;
    @Autowired RequestService requestService;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired TestSupport support;

    @Test
    void registrationCreatesUserAndDonorWithHashedPassword() {
        DonorRegistrationForm f = TestSupport.donorForm(BloodGroup.O_POS, "Mysuru", null);
        User u = userService.registerDonor(f);
        assertEquals(Role.DONOR, u.getRole());
        assertNotEquals(TestSupport.PASSWORD, u.getPasswordHash());
        assertTrue(encoder.matches(TestSupport.PASSWORD, u.getPasswordHash()));
        assertTrue(u.getPasswordHash().startsWith("$2")); // BCrypt
        assertEquals(BloodGroup.O_POS, donorService.getByUser(u).getBloodGroup());
    }

    @Test
    void duplicateEmailIsRejected() {
        DonorRegistrationForm a = TestSupport.donorForm(BloodGroup.A_POS, "Mysuru", null);
        userService.registerDonor(a);
        DonorRegistrationForm b = TestSupport.donorForm(BloodGroup.B_POS, "Mysuru", null);
        b.setEmail(a.getEmail().toUpperCase());
        assertThrows(BusinessException.class, () -> userService.registerDonor(b));
    }

    @Test
    void duplicatePhoneIsRejected() {
        DonorRegistrationForm a = TestSupport.donorForm(BloodGroup.A_POS, "Mysuru", null);
        userService.registerDonor(a);
        DonorRegistrationForm b = TestSupport.donorForm(BloodGroup.B_POS, "Mysuru", null);
        b.setPhone(a.getPhone());
        assertThrows(BusinessException.class, () -> userService.registerDonor(b));
    }

    @Test
    void passwordMismatchAndUnderageAreRejected() {
        DonorRegistrationForm a = TestSupport.donorForm(BloodGroup.A_POS, "Mysuru", null);
        a.setConfirmPassword("Different1");
        assertThrows(BusinessException.class, () -> userService.registerDonor(a));

        DonorRegistrationForm b = TestSupport.donorForm(BloodGroup.A_POS, "Mysuru", null);
        b.setDateOfBirth(LocalDate.now().minusYears(16));
        assertThrows(BusinessException.class, () -> userService.registerDonor(b));
    }

    @Test
    void searchFiltersByGroupAndCity() {
        userService.registerDonor(TestSupport.donorForm(BloodGroup.AB_POS, "Hubballi", null));
        userService.registerDonor(TestSupport.donorForm(BloodGroup.AB_POS, "Belagavi", null));
        userService.registerDonor(TestSupport.donorForm(BloodGroup.O_NEG, "Hubballi", null));

        assertEquals(2, donorService.search(BloodGroup.AB_POS, null, null, null, null).size());
        assertEquals(1, donorService.search(BloodGroup.AB_POS, "hubb", null, null, null).size());
        assertEquals(2, donorService.search(null, "Hubballi", null, null, null).size());
        assertEquals(0, donorService.search(BloodGroup.A_NEG, "Hubballi", null, null, null).size());
    }

    @Test
    void searchTreatsPercentSignAsLiteralText() {
        userService.registerDonor(TestSupport.donorForm(BloodGroup.A_POS, "Mangaluru", null));
        assertEquals(0, donorService.search(null, "%", null, null, null).size());
    }

    @Test
    void verifiedFlagCanBeSetByStaff() {
        User u = userService.registerDonor(TestSupport.donorForm(BloodGroup.A_POS, "Mangaluru", null));
        Donor d = donorService.getByUser(u);
        assertFalse(d.isVerified());
        donorService.setVerified(d.getId(), true, "staff@test.local");
        assertTrue(donorService.get(d.getId()).isVerified());
    }

    @Test
    void matchingUsesGroupCityAvailabilityAndLastDonation() {
        User recipient = support.user(Role.RECIPIENT);
        // eligible-looking candidates (preliminary only)
        Donor ok = donorService.getByUser(userService.registerDonor(TestSupport.donorForm(BloodGroup.B_NEG, "Bengaluru", LocalDate.now().minusDays(200))));
        Donor noDate = donorService.getByUser(userService.registerDonor(TestSupport.donorForm(BloodGroup.B_NEG, "Bengaluru", null)));
        // excluded candidates
        Donor recent = donorService.getByUser(userService.registerDonor(TestSupport.donorForm(BloodGroup.B_NEG, "Bengaluru", LocalDate.now().minusDays(10))));
        Donor away = donorService.getByUser(userService.registerDonor(TestSupport.donorForm(BloodGroup.B_NEG, "Bengaluru", null)));
        away.setAvailable(false);
        userService.registerDonor(TestSupport.donorForm(BloodGroup.B_NEG, "Mysuru", null));        // other city
        userService.registerDonor(TestSupport.donorForm(BloodGroup.A_POS, "Bengaluru", null));     // other group
        donorService.setVerified(ok.getId(), true, "staff");

        BloodRequest req = requestService.create(TestSupport.requestForm(BloodGroup.B_NEG, ComponentType.WHOLE_BLOOD, 1, Urgency.URGENT), recipient);
        DonorService.MatchResult result = donorService.matchFor(req, false);

        List<Long> ids = result.matches().stream().map(m -> m.donor().getId()).toList();
        assertTrue(ids.contains(ok.getId()));
        assertTrue(ids.contains(noDate.getId()));
        assertFalse(ids.contains(recent.getId()));
        assertFalse(ids.contains(away.getId()));
        assertEquals(2, ids.size());
        assertEquals(1, result.excludedRecentDonation());
        assertEquals(1, result.excludedUnavailable());
        // verified donors come first and every match explains why
        assertEquals(ok.getId(), result.matches().get(0).donor().getId());
        assertFalse(result.matches().get(0).reasons().isEmpty());

        assertEquals(1, donorService.matchFor(req, true).matches().size());
    }
}
