package com.lifelink.web;

import com.lifelink.TestSupport;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.DonorRepository;
import com.lifelink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthWebTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DonorRepository donors;
    @Autowired TestSupport support;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder donorRegistration(String email, String phone, String confirm) {
        return post("/register/donor").with(csrf())
                .param("fullName", "Asha Rao")
                .param("email", email)
                .param("password", TestSupport.PASSWORD)
                .param("confirmPassword", confirm)
                .param("dateOfBirth", "1995-05-10")
                .param("gender", "FEMALE")
                .param("bloodGroup", "O_POS")
                .param("phone", phone)
                .param("city", "Bengaluru")
                .param("contactPreference", "PHONE_AND_EMAIL")
                .param("available", "true");
    }

    @Test
    void publicPagesRender() throws Exception {
        for (String url : new String[]{"/", "/login", "/register", "/register/donor", "/register/recipient", "/faq"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void kannadaLanguageCanBeSelected() throws Exception {
        mvc.perform(get("/login").param("lang", "kn"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ಸೈನ್ ಇನ್")));
    }

    @Test
    void donorRegistrationCreatesAccountAndDonorProfile() throws Exception {
        String email = "reg-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        mvc.perform(donorRegistration(email, TestSupport.uniquePhone(), TestSupport.PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
        User u = users.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(Role.DONOR, u.getRole());
        assertTrue(donors.findByUserId(u.getId()).isPresent());
    }

    @Test
    void registrationWithMismatchedPasswordShowsErrorAndCreatesNothing() throws Exception {
        String email = "bad-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        mvc.perform(donorRegistration(email, TestSupport.uniquePhone(), "Other1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("register-donor"))
                .andExpect(model().attributeHasErrors("form"));
        assertTrue(users.findByEmailIgnoreCase(email).isEmpty());
    }

    @Test
    void registrationWithInvalidFieldsIsRejectedByServerValidation() throws Exception {
        mvc.perform(post("/register/donor").with(csrf())
                        .param("fullName", "").param("email", "nope").param("password", "123")
                        .param("confirmPassword", "123").param("phone", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("register-donor"))
                .andExpect(model().attributeHasFieldErrors("form", "fullName", "email", "password", "phone", "bloodGroup"));
    }

    @Test
    void recipientRegistrationWorksAndRejectsDuplicates() throws Exception {
        String email = "rec-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        var req = post("/register/recipient").with(csrf())
                .param("fullName", "Ravi Kumar").param("email", email)
                .param("password", TestSupport.PASSWORD).param("confirmPassword", TestSupport.PASSWORD);
        mvc.perform(req).andExpect(redirectedUrl("/login?registered"));
        mvc.perform(post("/register/recipient").with(csrf())
                        .param("fullName", "Ravi Kumar").param("email", email)
                        .param("password", TestSupport.PASSWORD).param("confirmPassword", TestSupport.PASSWORD))
                .andExpect(status().isOk()).andExpect(view().name("register-recipient"));
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/register/recipient")
                        .param("fullName", "No Csrf").param("email", "x@test.local")
                        .param("password", TestSupport.PASSWORD).param("confirmPassword", TestSupport.PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginSucceedsWithCorrectPassword() throws Exception {
        User u = support.user(Role.RECIPIENT);
        mvc.perform(formLogin("/login").user("username", u.getEmail()).password("password", TestSupport.PASSWORD))
                .andExpect(authenticated().withUsername(u.getEmail()))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void loginFailsWithWrongPasswordOrDisabledAccount() throws Exception {
        User u = support.user(Role.DONOR);
        mvc.perform(formLogin("/login").user("username", u.getEmail()).password("password", "WrongPass1"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        u.setEnabled(false);
        users.save(u);
        mvc.perform(formLogin("/login").user("username", u.getEmail()).password("password", TestSupport.PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unauthenticatedUsersAreSentToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/requests")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/inventory")).andExpect(status().is3xxRedirection());
    }
}
