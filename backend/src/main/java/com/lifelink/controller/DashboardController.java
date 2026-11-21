package com.lifelink.controller;

import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** One URL, three different dashboards depending on the role. */
@Controller
public class DashboardController {

    private final DashboardService dashboard;
    private final ControllerSupport support;

    public DashboardController(DashboardService dashboard, ControllerSupport support) {
        this.dashboard = dashboard;
        this.support = support;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = support.user(auth);
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF) {
            model.addAttribute("d", dashboard.staffDashboard());
            return "dashboard/staff";
        }
        if (user.getRole() == Role.DONOR) {
            model.addAttribute("d", dashboard.donorDashboard(user));
            return "dashboard/donor";
        }
        model.addAttribute("d", dashboard.recipientDashboard(user));
        return "dashboard/recipient";
    }
}
