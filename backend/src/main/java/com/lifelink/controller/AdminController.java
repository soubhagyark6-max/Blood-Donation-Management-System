package com.lifelink.controller;

import com.lifelink.dto.StaffForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.Role;
import com.lifelink.service.AuditService;
import com.lifelink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administrator-only area: user/staff management and the audit log. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService users;
    private final AuditService audit;

    public AdminController(UserService users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("users", users.all());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new StaffForm());
        }
        return "admin/users";
    }

    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("form") StaffForm form, BindingResult result, Authentication auth,
                         Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                users.createStaff(form, auth.getName());
                ra.addFlashAttribute("success", "Account created for " + form.getEmail() + ".");
                return "redirect:/admin/users";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("users", users.all());
        model.addAttribute("openModal", "staffModal");
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean enabled, Authentication auth, RedirectAttributes ra) {
        try {
            users.setEnabled(id, enabled, auth.getName());
            ra.addFlashAttribute("success", enabled ? "Account enabled." : "Account disabled.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("logs", audit.recent());
        return "admin/audit";
    }

    @ModelAttribute("staffRoles")
    public Role[] staffRoles() {
        return new Role[]{Role.STAFF, Role.ADMIN};
    }
}
