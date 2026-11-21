package com.lifelink.service;

import com.lifelink.TestSupport;
import com.lifelink.dto.BloodRequestForm;
import com.lifelink.dto.DonorRegistrationForm;
import com.lifelink.dto.StockForm;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.ComponentType;
import com.lifelink.model.Urgency;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Bean-validation rules (server-side), tested without starting Spring. */
class FormValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean hasViolationOn(Object form, String field) {
        return validator.validate(form).stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    void validRequestFormPasses() {
        assertTrue(validator.validate(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 2, Urgency.NORMAL)).isEmpty());
    }

    @Test
    void requestFormRejectsBadUnitsPhoneAndPastDate() {
        BloodRequestForm f = TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 0, Urgency.NORMAL);
        f.setContactPhone("12345");
        f.setRequiredDate(LocalDate.now().minusDays(1));
        f.setHospital(" ");
        assertTrue(hasViolationOn(f, "units"));
        assertTrue(hasViolationOn(f, "contactPhone"));
        assertTrue(hasViolationOn(f, "requiredDate"));
        assertTrue(hasViolationOn(f, "hospital"));
        f.setUnits(21);
        assertTrue(hasViolationOn(f, "units"));
    }

    @Test
    void donorFormRejectsWeakPasswordBadEmailAndFutureDates() {
        DonorRegistrationForm f = TestSupport.donorForm(BloodGroup.A_POS, "Mysuru", LocalDate.now().plusDays(3));
        f.setEmail("not-an-email");
        f.setPassword("short");
        f.setPhone("0000000000");
        assertTrue(hasViolationOn(f, "email"));
        assertTrue(hasViolationOn(f, "password"));
        assertTrue(hasViolationOn(f, "phone"));
        assertTrue(hasViolationOn(f, "lastDonationDate"));
        f.setPassword("onlyletters");
        assertTrue(hasViolationOn(f, "password")); // needs a digit
    }

    @Test
    void stockFormRejectsNonPositiveUnitsAndMissingGroup() {
        StockForm f = new StockForm();
        f.setUnits(0);
        assertTrue(hasViolationOn(f, "units"));
        assertTrue(hasViolationOn(f, "bloodGroup"));
    }
}
