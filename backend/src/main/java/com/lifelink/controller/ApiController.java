package com.lifelink.controller;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.ComponentType;
import com.lifelink.repository.DonorRepository;
import com.lifelink.service.CampService;
import com.lifelink.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small read-only REST API (JSON). The main screens are server-rendered by Thymeleaf; this API is used by
 * frontend/js/live-stats.js on the home page and can be called by other clients.
 *   GET /api/public/stats     - open to everyone (counts only, no personal data)
 *   GET /api/inventory/stock  - ADMIN / STAFF only (usable units per group and component)
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final DonorRepository donors;
    private final InventoryService inventory;
    private final CampService camps;

    public ApiController(DonorRepository donors, InventoryService inventory, CampService camps) {
        this.donors = donors;
        this.inventory = inventory;
        this.camps = camps;
    }

    @GetMapping("/public/stats")
    public Map<String, Long> publicStats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("donors", donors.count());
        m.put("units", inventory.totalUsableUnits());
        m.put("camps", camps.upcomingCount());
        return m;
    }

    @GetMapping("/inventory/stock")
    public Map<String, Map<String, Long>> stock() {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        for (Map.Entry<BloodGroup, Map<ComponentType, Long>> g : inventory.stockMatrix().entrySet()) {
            Map<String, Long> row = new LinkedHashMap<>();
            g.getValue().forEach((c, v) -> row.put(c.getLabel(), v));
            out.put(g.getKey().getLabel(), row);
        }
        return out;
    }
}
