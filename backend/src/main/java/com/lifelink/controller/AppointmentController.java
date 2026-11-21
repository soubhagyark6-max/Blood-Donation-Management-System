package com.lifelink.controller;

import com.lifelink.exception.BusinessException;
import com.lifelink.model.Appointment;
import com.lifelink.model.AppointmentStatus;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.service.AppointmentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointments;
    private final ControllerSupport support;

    public AppointmentController(AppointmentService appointments, ControllerSupport support) {
        this.appointments = appointments;
        this.support = support;
    }

    @GetMapping
    public String mine(Authentication auth, Model model) {
        model.addAttribute("appointments", appointments.forDonor(support.user(auth)));
        return "appointments/list";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = support.user(auth);
        Appointment a = appointments.getOwned(id, user);
        try {
            appointments.cancel(id, user);
            ra.addFlashAttribute("success", "Appointment cancelled.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        return staff ? "redirect:/camps/" + a.getCamp().getId() + "/registrations" : "redirect:/appointments";
    }

    @PostMapping("/{id}/mark")
    public String mark(@PathVariable Long id, @RequestParam AppointmentStatus status, Authentication auth,
                       RedirectAttributes ra) {
        User staff = support.user(auth);
        Appointment a = appointments.getOwned(id, staff);
        try {
            appointments.mark(id, status, staff);
            ra.addFlashAttribute("success", "Appointment marked as " + status.getLabel() + ".");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/camps/" + a.getCamp().getId() + "/registrations";
    }
}
