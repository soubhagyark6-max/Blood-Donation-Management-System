package com.lifelink.controller;

import com.lifelink.dto.BloodRequestForm;
import com.lifelink.dto.StatusUpdateForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.*;
import com.lifelink.service.DonorService;
import com.lifelink.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/requests")
public class RequestController {

    private final RequestService requests;
    private final DonorService donors;
    private final ControllerSupport support;

    public RequestController(RequestService requests, DonorService donors, ControllerSupport support) {
        this.requests = requests;
        this.donors = donors;
        this.support = support;
    }

    @GetMapping
    public String list(@RequestParam(required = false) RequestStatus status,
                       @RequestParam(required = false) BloodGroup bloodGroup,
                       @RequestParam(required = false) Urgency urgency,
                       @RequestParam(required = false) String q,
                       Authentication auth, Model model) {
        User user = support.user(auth);
        model.addAttribute("requestList", requests.search(user, status, bloodGroup, urgency, q));
        model.addAttribute("fStatus", status);
        model.addAttribute("fBloodGroup", bloodGroup);
        model.addAttribute("fUrgency", urgency);
        model.addAttribute("fQ", q);
        return "requests/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        BloodRequestForm f = new BloodRequestForm();
        f.setRequiredDate(LocalDate.now());
        model.addAttribute("form", f);
        model.addAttribute("editing", false);
        return "requests/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") BloodRequestForm form, BindingResult result,
                         Authentication auth, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            BloodRequest r = requests.create(form, support.user(auth));
            ra.addFlashAttribute("success", "Request #" + r.getId() + " submitted. Blood bank staff will review it.");
            return "redirect:/requests/" + r.getId();
        }
        model.addAttribute("editing", false);
        return "requests/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        User user = support.user(auth);
        BloodRequest r = requests.getForUser(id, user);
        model.addAttribute("req", r);
        model.addAttribute("history", requests.historyOf(id));
        if (!model.containsAttribute("statusForm")) {
            model.addAttribute("statusForm", new StatusUpdateForm());
        }
        return "requests/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        BloodRequest r = requests.getForUser(id, support.user(auth));
        if (r.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Only requests that are still Pending can be edited");
        }
        BloodRequestForm f = new BloodRequestForm();
        f.setPatientReference(r.getPatientReference());
        f.setBloodGroup(r.getBloodGroup());
        f.setComponent(r.getComponent());
        f.setUnits(r.getUnits());
        f.setHospital(r.getHospital());
        f.setCity(r.getCity());
        f.setContactPhone(r.getContactPhone());
        f.setRequiredDate(r.getRequiredDate());
        f.setUrgency(r.getUrgency());
        model.addAttribute("form", f);
        model.addAttribute("editing", true);
        model.addAttribute("requestId", id);
        return "requests/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") BloodRequestForm form, BindingResult result,
                       Authentication auth, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                requests.update(id, form, support.user(auth));
                ra.addFlashAttribute("success", "Request updated.");
                return "redirect:/requests/" + id;
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("editing", true);
        model.addAttribute("requestId", id);
        return "requests/form";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            requests.cancel(id, support.user(auth));
            ra.addFlashAttribute("success", "Request cancelled.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/requests/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @Valid @ModelAttribute("statusForm") StatusUpdateForm form,
                               BindingResult result, Authentication auth, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please choose a status and keep remarks under 500 characters.");
            return "redirect:/requests/" + id;
        }
        try {
            requests.changeStatus(id, form, support.user(auth));
            ra.addFlashAttribute("success", "Status updated to " + form.getStatus().getLabel() + ".");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/requests/" + id;
    }

    /** Preliminary donor matching - assistance only, never a medical decision. */
    @GetMapping("/{id}/matches")
    public String matches(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean verifiedOnly,
                          Authentication auth, Model model) {
        BloodRequest r = requests.getForUser(id, support.user(auth));
        model.addAttribute("req", r);
        model.addAttribute("result", donors.matchFor(r, verifiedOnly));
        model.addAttribute("verifiedOnly", verifiedOnly);
        model.addAttribute("intervalDays", donors.getDonationIntervalDays());
        return "requests/matches";
    }
}
