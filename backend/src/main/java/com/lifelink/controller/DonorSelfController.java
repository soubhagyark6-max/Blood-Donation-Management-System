package com.lifelink.controller;

import com.lifelink.dto.DonationForm;
import com.lifelink.dto.DonorProfileForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.Donor;
import com.lifelink.model.User;
import com.lifelink.service.DonationService;
import com.lifelink.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** A donor's own profile and donation history (URL /donor/** is reserved for the DONOR role). */
@Controller
@RequestMapping("/donor")
public class DonorSelfController {

    private final DonorService donors;
    private final DonationService donations;
    private final ControllerSupport support;

    public DonorSelfController(DonorService donors, DonationService donations, ControllerSupport support) {
        this.donors = donors;
        this.donations = donations;
        this.support = support;
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        Donor d = donors.getByUser(support.user(auth));
        model.addAttribute("donor", d);
        model.addAttribute("form", DonorController.toForm(d));
        return "donors/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@Valid @ModelAttribute("form") DonorProfileForm form, BindingResult result,
                              Authentication auth, Model model, RedirectAttributes ra) {
        User user = support.user(auth);
        if (!result.hasErrors()) {
            try {
                donors.updateProfile(user, form);
                ra.addFlashAttribute("success", "Your profile has been updated.");
                return "redirect:/donor/profile";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("donor", donors.getByUser(user));
        return "donors/profile";
    }

    @GetMapping("/history")
    public String history(Authentication auth, Model model) {
        Donor d = donors.getByUser(support.user(auth));
        DonationForm f = new DonationForm();
        f.setDonationDate(LocalDate.now());
        fillHistory(model, d, f);
        return "donors/history";
    }

    @PostMapping("/history")
    public String addHistory(@Valid @ModelAttribute("donationForm") DonationForm form, BindingResult result,
                             Authentication auth, Model model, RedirectAttributes ra) {
        User user = support.user(auth);
        Donor d = donors.getByUser(user);
        if (!result.hasErrors()) {
            try {
                donations.recordBySelf(d, form, user);
                ra.addFlashAttribute("success", "Donation added to your history (shown as self-reported until staff verify it).");
                return "redirect:/donor/history";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        fillHistory(model, d, form);
        return "donors/history";
    }

    private void fillHistory(Model model, Donor d, DonationForm form) {
        model.addAttribute("donor", d);
        model.addAttribute("history", donations.history(d.getId()));
        model.addAttribute("donationForm", form);
        model.addAttribute("nextReminder", donors.nextReminderDate(d));
        model.addAttribute("intervalDays", donors.getDonationIntervalDays());
    }
}
