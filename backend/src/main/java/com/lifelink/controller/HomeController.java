package com.lifelink.controller;

import com.lifelink.dto.DonorRegistrationForm;
import com.lifelink.dto.RecipientRegistrationForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.repository.DonorRepository;
import com.lifelink.service.CampService;
import com.lifelink.service.InventoryService;
import com.lifelink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Public pages: landing page, login, registration, FAQ. */
@Controller
public class HomeController {

    private final DonorRepository donors;
    private final InventoryService inventory;
    private final CampService camps;
    private final UserService userService;

    public HomeController(DonorRepository donors, InventoryService inventory, CampService camps, UserService userService) {
        this.donors = donors;
        this.inventory = inventory;
        this.camps = camps;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Live figures from the database (not hard-coded).
        model.addAttribute("donorCount", donors.count());
        model.addAttribute("unitCount", inventory.totalUsableUnits());
        model.addAttribute("campCount", camps.upcomingCount());
        model.addAttribute("upcomingCamps", camps.upcoming().stream().limit(3).toList());
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String chooseRegistration() {
        return "register-choice";
    }

    @GetMapping("/register/donor")
    public String donorForm(Model model) {
        model.addAttribute("form", new DonorRegistrationForm());
        return "register-donor";
    }

    @PostMapping("/register/donor")
    public String registerDonor(@Valid @ModelAttribute("form") DonorRegistrationForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "register-donor";
        }
        try {
            userService.registerDonor(form);
        } catch (BusinessException ex) {
            result.reject("business", ex.getMessage());
            return "register-donor";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/register/recipient")
    public String recipientForm(Model model) {
        model.addAttribute("form", new RecipientRegistrationForm());
        return "register-recipient";
    }

    @PostMapping("/register/recipient")
    public String registerRecipient(@Valid @ModelAttribute("form") RecipientRegistrationForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "register-recipient";
        }
        try {
            userService.registerRecipient(form);
        } catch (BusinessException ex) {
            result.reject("business", ex.getMessage());
            return "register-recipient";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }
}
