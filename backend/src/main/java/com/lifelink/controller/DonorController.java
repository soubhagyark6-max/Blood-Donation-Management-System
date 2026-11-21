package com.lifelink.controller;

import com.lifelink.dto.DonationForm;
import com.lifelink.dto.DonorProfileForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.BloodGroup;
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

/** Staff/admin donor management (contact details are visible here only - the whole /donors area needs ADMIN or STAFF). */
@Controller
@RequestMapping("/donors")
public class DonorController {

    private final DonorService donors;
    private final DonationService donations;
    private final ControllerSupport support;

    public DonorController(DonorService donors, DonationService donations, ControllerSupport support) {
        this.donors = donors;
        this.donations = donations;
        this.support = support;
    }

    @GetMapping
    public String list(@RequestParam(required = false) BloodGroup bloodGroup,
                       @RequestParam(required = false) String city,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Boolean availableOnly,
                       @RequestParam(required = false) Boolean verifiedOnly,
                       Model model) {
        model.addAttribute("donors", donors.search(bloodGroup, city, q, availableOnly, verifiedOnly));
        model.addAttribute("fBloodGroup", bloodGroup);
        model.addAttribute("fCity", city);
        model.addAttribute("fQ", q);
        model.addAttribute("fAvailable", availableOnly);
        model.addAttribute("fVerified", verifiedOnly);
        return "donors/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Donor d = donors.get(id);
        model.addAttribute("donor", d);
        model.addAttribute("history", donations.history(id));
        DonationForm f = new DonationForm();
        f.setDonationDate(LocalDate.now());
        model.addAttribute("donationForm", f);
        model.addAttribute("nextReminder", donors.nextReminderDate(d));
        model.addAttribute("intervalDays", donors.getDonationIntervalDays());
        return "donors/detail";
    }

    @PostMapping("/{id}/donation")
    public String recordDonation(@PathVariable Long id, @Valid @ModelAttribute("donationForm") DonationForm form,
                                 BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        Donor d = donors.get(id);
        if (!result.hasErrors()) {
            try {
                donations.recordByStaff(d, form, support.user(auth));
                ra.addFlashAttribute("success", "Donation recorded" + (form.isAddToInventory() ? " and stock added to inventory." : "."));
                return "redirect:/donors/" + id;
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("donor", d);
        model.addAttribute("history", donations.history(id));
        model.addAttribute("nextReminder", donors.nextReminderDate(d));
        model.addAttribute("intervalDays", donors.getDonationIntervalDays());
        return "donors/detail";
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Long id, @RequestParam boolean verified, Authentication auth, RedirectAttributes ra) {
        donors.setVerified(id, verified, auth.getName());
        ra.addFlashAttribute("success", verified ? "Donor marked as verified." : "Verification removed.");
        return "redirect:/donors/" + id;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Donor d = donors.get(id);
        model.addAttribute("donor", d);
        model.addAttribute("form", toForm(d));
        return "donors/edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") DonorProfileForm form, BindingResult result,
                       Authentication auth, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                donors.updateByStaff(id, form, auth.getName());
                ra.addFlashAttribute("success", "Donor updated.");
                return "redirect:/donors/" + id;
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("donor", donors.get(id));
        return "donors/edit";
    }

    static DonorProfileForm toForm(Donor d) {
        DonorProfileForm f = new DonorProfileForm();
        f.setFullName(d.getFullName());
        f.setDateOfBirth(d.getDateOfBirth());
        f.setGender(d.getGender());
        f.setBloodGroup(d.getBloodGroup());
        f.setPhone(d.getPhone());
        f.setCity(d.getCity());
        f.setAddress(d.getAddress());
        f.setLastDonationDate(d.getLastDonationDate());
        f.setAvailable(d.isAvailable());
        f.setContactPreference(d.getContactPreference());
        return f;
    }
}
