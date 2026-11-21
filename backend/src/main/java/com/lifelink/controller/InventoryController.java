package com.lifelink.controller;

import com.lifelink.dto.StockForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.ComponentType;
import com.lifelink.model.TxType;
import com.lifelink.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Inventory module - URL /inventory/** is restricted to ADMIN and STAFF. */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public String page(@RequestParam(required = false) BloodGroup bloodGroup,
                       @RequestParam(required = false) ComponentType component,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String sort,
                       Model model) {
        fill(model, bloodGroup, component, status, sort);
        if (!model.containsAttribute("receiveForm")) {
            StockForm f = new StockForm();
            f.setCollectionDate(LocalDate.now());
            model.addAttribute("receiveForm", f);
        }
        if (!model.containsAttribute("issueForm")) {
            model.addAttribute("issueForm", new StockForm());
        }
        return "inventory/index";
    }

    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute("receiveForm") StockForm form, BindingResult result,
                          Authentication auth, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                inventory.receive(form, auth.getName(), null);
                ra.addFlashAttribute("success", form.getUnits() + " unit(s) of " + form.getBloodGroup().getLabel() + " "
                        + form.getComponent().getLabel() + " added to stock.");
                inventory.refreshAlerts();
                return "redirect:/inventory";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        fill(model, null, null, null, null);
        model.addAttribute("issueForm", new StockForm());
        model.addAttribute("openModal", "receiveModal");
        return "inventory/index";
    }

    @PostMapping("/issue")
    public String issue(@Valid @ModelAttribute("issueForm") StockForm form, BindingResult result,
                        Authentication auth, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                inventory.issue(form, auth.getName());
                ra.addFlashAttribute("success", form.getUnits() + " unit(s) of " + form.getBloodGroup().getLabel() + " "
                        + form.getComponent().getLabel() + " issued.");
                return "redirect:/inventory";
            } catch (BusinessException ex) {
                result.reject("business", ex.getMessage());
            }
        }
        fill(model, null, null, null, null);
        StockForm r = new StockForm();
        r.setCollectionDate(LocalDate.now());
        model.addAttribute("receiveForm", r);
        model.addAttribute("openModal", "issueModal");
        return "inventory/index";
    }

    @PostMapping("/discard")
    public String discard(Authentication auth, RedirectAttributes ra) {
        int n = inventory.discardExpired(auth.getName());
        ra.addFlashAttribute("success", n == 0 ? "No expired stock to discard." : n + " expired unit(s) written off.");
        return "redirect:/inventory";
    }

    @GetMapping("/history")
    public String history(@RequestParam(required = false) BloodGroup bloodGroup,
                          @RequestParam(required = false) ComponentType component,
                          @RequestParam(required = false) TxType type, Model model) {
        model.addAttribute("transactions", inventory.history(bloodGroup, component, type));
        model.addAttribute("txTypes", TxType.values());
        model.addAttribute("fBloodGroup", bloodGroup);
        model.addAttribute("fComponent", component);
        model.addAttribute("fType", type);
        return "inventory/history";
    }

    private void fill(Model model, BloodGroup bg, ComponentType comp, String status, String sort) {
        model.addAttribute("matrix", inventory.stockMatrix());
        model.addAttribute("batches", inventory.searchBatches(bg, comp, status, sort));
        model.addAttribute("lowStock", inventory.lowStock());
        model.addAttribute("expiring", inventory.expiringSoon());
        model.addAttribute("expiredWithStock", inventory.expiredWithStock());
        model.addAttribute("threshold", inventory.getLowStockThreshold());
        model.addAttribute("expiryDays", inventory.getExpiryAlertDays());
        model.addAttribute("fBloodGroup", bg);
        model.addAttribute("fComponent", comp);
        model.addAttribute("fStatus", status);
        model.addAttribute("fSort", sort);
    }
}
