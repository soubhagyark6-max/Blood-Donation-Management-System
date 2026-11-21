package com.lifelink.controller;

import com.lifelink.dto.CampForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.DonationCamp;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.service.AppointmentService;
import com.lifelink.service.CampService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/camps")
public class CampController {

    private final CampService camps;
    private final AppointmentService appointments;
    private final ControllerSupport support;

    public CampController(CampService camps, AppointmentService appointments, ControllerSupport support) {
        this.camps = camps;
        this.appointments = appointments;
        this.support = support;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        User user = support.user(auth);
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        List<DonationCamp> list = staff ? camps.all() : camps.upcoming();
        Map<Long, Long> booked = new LinkedHashMap<>();
        for (DonationCamp c : list) {
            booked.put(c.getId(), appointments.bookedCount(c.getId()));
        }
        model.addAttribute("camps", list);
        model.addAttribute("booked", booked);
        model.addAttribute("isStaff", staff);
        return "camps/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CampForm());
        model.addAttribute("editing", false);
        return "camps/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CampForm form, BindingResult result, Authentication auth,
                         Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                camps.create(form, auth.getName());
                ra.addFlashAttribute("success", "Donation camp created.");
                return "redirect:/camps";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("editing", false);
        return "camps/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        DonationCamp c = camps.get(id);
        CampForm f = new CampForm();
        f.setName(c.getName());
        f.setCampDate(c.getCampDate());
        f.setStartTime(c.getStartTime());
        f.setEndTime(c.getEndTime());
        f.setVenue(c.getVenue());
        f.setCity(c.getCity());
        f.setContactName(c.getContactName());
        f.setContactPhone(c.getContactPhone());
        f.setCapacity(c.getCapacity());
        f.setDescription(c.getDescription());
        model.addAttribute("form", f);
        model.addAttribute("editing", true);
        model.addAttribute("campId", id);
        return "camps/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") CampForm form, BindingResult result,
                       Authentication auth, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                camps.update(id, form, auth.getName());
                ra.addFlashAttribute("success", "Camp updated.");
                return "redirect:/camps";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("editing", true);
        model.addAttribute("campId", id);
        return "camps/form";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean active, Authentication auth, RedirectAttributes ra) {
        camps.setActive(id, active, auth.getName());
        ra.addFlashAttribute("success", active ? "Camp re-activated." : "Camp cancelled.");
        return "redirect:/camps";
    }

    @GetMapping("/{id}/registrations")
    public String registrations(@PathVariable Long id, Model model) {
        model.addAttribute("camp", camps.get(id));
        model.addAttribute("appointments", appointments.forCamp(id));
        return "camps/registrations";
    }

    @PostMapping("/{id}/book")
    public String book(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            appointments.book(id, support.user(auth));
            ra.addFlashAttribute("success", "Appointment booked! Show your QR code at the camp.");
            return "redirect:/appointments";
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/camps";
        }
    }
}
