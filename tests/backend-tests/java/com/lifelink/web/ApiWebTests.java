package com.lifelink.web;

import com.lifelink.TestSupport;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** REST API: public stats are open, stock data is staff/admin only. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiWebTests {

    @Autowired MockMvc mvc;
    @Autowired TestSupport support;

    @Test
    void publicStatsAreOpenAndReturnJson() throws Exception {
        mvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.donors").exists())
                .andExpect(jsonPath("$.units").exists())
                .andExpect(jsonPath("$.camps").exists());
    }

    @Test
    void stockApiIsStaffOnly() throws Exception {
        User staff = support.user(Role.STAFF);
        User donor = support.user(Role.DONOR);
        mvc.perform(get("/api/inventory/stock").with(user(donor.getEmail()).roles("DONOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventory/stock").with(user(staff.getEmail()).roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['O+']['Whole Blood']").exists());
        mvc.perform(get("/api/inventory/stock")).andExpect(status().is3xxRedirection());
    }
}
