package com.lifelink.web;

import com.lifelink.TestSupport;
import com.lifelink.dto.StockForm;
import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.service.InventoryService;
import com.lifelink.service.RequestService;
import com.lifelink.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Role-based access control and page rendering for every role (real database users, H2 in tests). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleAccessWebTests {

    @Autowired MockMvc mvc;
    @Autowired TestSupport support;
    @Autowired UserService userService;
    @Autowired RequestService requestService;
    @Autowired InventoryService inventory;
    @Autowired BloodRequestRepository requestRepo;

    User admin, staff, donorUser, recipient;
    RequestPostProcessor asAdmin, asStaff, asDonor, asRecipient;

    @BeforeEach
    void setUp() {
        admin = support.user(Role.ADMIN);
        staff = support.user(Role.STAFF);
        recipient = support.user(Role.RECIPIENT);
        donorUser = userService.registerDonor(TestSupport.donorForm(BloodGroup.O_POS, "Bengaluru", null));
        asAdmin = user(admin.getEmail()).roles("ADMIN");
        asStaff = user(staff.getEmail()).roles("STAFF");
        asDonor = user(donorUser.getEmail()).roles("DONOR");
        asRecipient = user(recipient.getEmail()).roles("RECIPIENT");
    }

    // ------------------------------------------------------------ access matrix
    @Test
    void donorAndRecipientCannotOpenStaffAreas() throws Exception {
        for (String url : new String[]{"/inventory", "/donors", "/reports", "/admin/users", "/admin/audit"}) {
            mvc.perform(get(url).with(asDonor)).andExpect(status().isForbidden());
            mvc.perform(get(url).with(asRecipient)).andExpect(status().isForbidden());
        }
    }

    @Test
    void staffCannotOpenAdminAreas() throws Exception {
        mvc.perform(get("/admin/users").with(asStaff)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/audit").with(asStaff)).andExpect(status().isForbidden());
    }

    @Test
    void donorOnlyAndRecipientOnlyAreasAreProtected() throws Exception {
        mvc.perform(get("/donor/profile").with(asRecipient)).andExpect(status().isForbidden());
        mvc.perform(get("/appointments").with(asStaff)).andExpect(status().isForbidden());
        mvc.perform(get("/requests/new").with(asDonor)).andExpect(status().isForbidden());
        mvc.perform(get("/requests/new").with(asStaff)).andExpect(status().isForbidden());
    }

    @Test
    void onlyStaffCanChangeRequestStatus() throws Exception {
        BloodRequest r = requestService.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        mvc.perform(post("/requests/{id}/status", r.getId()).with(asRecipient).with(csrf()).param("status", "APPROVED"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/requests/{id}/status", r.getId()).with(asDonor).with(csrf()).param("status", "APPROVED"))
                .andExpect(status().isForbidden());
        assertEquals(RequestStatus.PENDING, requestRepo.findById(r.getId()).orElseThrow().getStatus());

        mvc.perform(post("/requests/{id}/status", r.getId()).with(asStaff).with(csrf()).param("status", "APPROVED").param("remarks", "ok"))
                .andExpect(status().is3xxRedirection());
        assertEquals(RequestStatus.APPROVED, requestRepo.findById(r.getId()).orElseThrow().getStatus());
    }

    @Test
    void recipientCannotOpenAnotherRecipientsRequest() throws Exception {
        User other = support.user(Role.RECIPIENT);
        BloodRequest r = requestService.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), other);
        mvc.perform(get("/requests/{id}", r.getId()).with(asRecipient)).andExpect(status().isForbidden());
        mvc.perform(get("/requests/{id}", r.getId()).with(asStaff)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------ pages render (also checks Thymeleaf syntax)
    @Test
    void staffAndAdminPagesRender() throws Exception {
        inventory.receive(BloodGroup.O_POS, ComponentType.WHOLE_BLOOD, 3, LocalDate.now(), null, null, "tester", false);
        requestService.create(TestSupport.requestForm(BloodGroup.O_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.EMERGENCY), recipient);
        for (String url : new String[]{"/dashboard", "/donors", "/requests", "/inventory", "/inventory/history", "/camps",
                "/camps/new", "/reports", "/notifications"}) {
            mvc.perform(get(url).with(asStaff)).andExpect(status().isOk());
        }
        for (String url : new String[]{"/admin/users", "/admin/audit", "/dashboard"}) {
            mvc.perform(get(url).with(asAdmin)).andExpect(status().isOk());
        }
        mvc.perform(get("/dashboard").with(asStaff)).andExpect(content().string(containsString("Registered donors")));
    }

    @Test
    void donorPagesRender() throws Exception {
        for (String url : new String[]{"/dashboard", "/donor/profile", "/donor/history", "/camps", "/appointments", "/notifications", "/faq"}) {
            mvc.perform(get(url).with(asDonor)).andExpect(status().isOk());
        }
    }

    @Test
    void recipientPagesAndRequestFlowRender() throws Exception {
        BloodRequest r = requestService.create(TestSupport.requestForm(BloodGroup.B_POS, ComponentType.PLASMA, 2, Urgency.URGENT), recipient);
        for (String url : new String[]{"/dashboard", "/requests", "/requests/new", "/notifications"}) {
            mvc.perform(get(url).with(asRecipient)).andExpect(status().isOk());
        }
        mvc.perform(get("/requests/{id}", r.getId()).with(asRecipient)).andExpect(status().isOk());
        mvc.perform(get("/requests/{id}/edit", r.getId()).with(asRecipient)).andExpect(status().isOk());
        mvc.perform(get("/requests/{id}/matches", r.getId()).with(asStaff)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Preliminary assistance only")));
    }

    @Test
    void recipientCanCreateRequestThroughForm() throws Exception {
        mvc.perform(post("/requests/new").with(asRecipient).with(csrf())
                        .param("patientReference", "P-777").param("bloodGroup", "A_NEG").param("component", "PLATELETS")
                        .param("units", "2").param("hospital", "City Hospital").param("city", "Mysuru")
                        .param("contactPhone", "9876543210").param("requiredDate", LocalDate.now().plusDays(1).toString())
                        .param("urgency", "EMERGENCY"))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, requestRepo.countByRequesterId(recipient.getId()));
    }

    @Test
    void invalidRequestFormIsRejectedWithErrors() throws Exception {
        mvc.perform(post("/requests/new").with(asRecipient).with(csrf())
                        .param("patientReference", "").param("units", "0").param("contactPhone", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("requests/form"))
                .andExpect(model().attributeHasFieldErrors("form", "patientReference", "units", "contactPhone", "bloodGroup", "hospital"));
        assertEquals(0, requestRepo.countByRequesterId(recipient.getId()));
    }

    @Test
    void staffCanReceiveAndIssueStockThroughForms() throws Exception {
        mvc.perform(post("/inventory/receive").with(asStaff).with(csrf())
                        .param("bloodGroup", "AB_POS").param("component", "PLASMA").param("units", "5"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/inventory/issue").with(asStaff).with(csrf())
                        .param("bloodGroup", "AB_POS").param("component", "PLASMA").param("units", "2"))
                .andExpect(status().is3xxRedirection());
        assertEquals(3, inventory.stockMatrix().get(BloodGroup.AB_POS).get(ComponentType.PLASMA));

        // over-issue shows an error on the page and does not change stock
        mvc.perform(post("/inventory/issue").with(asStaff).with(csrf())
                        .param("bloodGroup", "AB_POS").param("component", "PLASMA").param("units", "50"))
                .andExpect(status().isOk()).andExpect(view().name("inventory/index"))
                .andExpect(content().string(containsString("Not enough usable stock")));
        assertEquals(3, inventory.stockMatrix().get(BloodGroup.AB_POS).get(ComponentType.PLASMA));
    }

    @Test
    void csvExportDoesNotContainDonorContactDetails() throws Exception {
        mvc.perform(get("/reports/donors.csv").with(asStaff))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("lifelink-donors.csv")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("@test.local"))));
        mvc.perform(get("/reports/donors.csv").with(asDonor)).andExpect(status().isForbidden());
    }

    @Test
    void donorCanBookAndCancelCampAppointment() throws Exception {
        var camp = new com.lifelink.model.DonationCamp();
        camp.setName("Test camp");
        camp.setCampDate(LocalDate.now().plusDays(3));
        camp.setStartTime(java.time.LocalTime.of(9, 0));
        camp.setEndTime(java.time.LocalTime.of(14, 0));
        camp.setVenue("Hall");
        camp.setCity("Bengaluru");
        camp.setContactName("Organiser");
        camp.setContactPhone("9876543210");
        camp.setCapacity(1);
        camp = campRepo.save(camp);

        mvc.perform(post("/camps/{id}/book", camp.getId()).with(asDonor).with(csrf()))
                .andExpect(redirectedUrl("/appointments"));
        // second booking of the same camp is refused with a message
        mvc.perform(post("/camps/{id}/book", camp.getId()).with(asDonor).with(csrf()))
                .andExpect(redirectedUrl("/camps")).andExpect(flash().attributeExists("error"));
        mvc.perform(get("/appointments").with(asDonor)).andExpect(status().isOk());
    }

    @Autowired com.lifelink.repository.DonationCampRepository campRepo;
}
