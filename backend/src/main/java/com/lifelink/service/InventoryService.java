package com.lifelink.service;

import com.lifelink.dto.StockForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.*;
import com.lifelink.repository.BloodInventoryRepository;
import com.lifelink.repository.InventoryTransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Stock handling. Every change runs in a database transaction; batches are locked while stock is
 * issued so that two staff members can never overdraw the same units.
 * Components (whole blood, red cells, platelets, plasma) are separate stocks and never substitute for each other.
 */
@Service
public class InventoryService {

    /** Default shelf life (days) pre-filled when staff leave the expiry date empty. Staff can always override it. */
    public static final Map<ComponentType, Integer> DEFAULT_SHELF_LIFE_DAYS = Map.of(
            ComponentType.WHOLE_BLOOD, 35,
            ComponentType.RED_CELLS, 42,
            ComponentType.PLATELETS, 5,
            ComponentType.PLASMA, 365);

    private final BloodInventoryRepository repo;
    private final InventoryTransactionRepository txRepo;
    private final NotificationService notifications;
    private final AuditService audit;

    @Value("${lifelink.low-stock-threshold:5}")
    private int lowStockThreshold;

    @Value("${lifelink.expiry-alert-days:7}")
    private int expiryAlertDays;

    public InventoryService(BloodInventoryRepository repo, InventoryTransactionRepository txRepo,
                            NotificationService notifications, AuditService audit) {
        this.repo = repo;
        this.txRepo = txRepo;
        this.notifications = notifications;
        this.audit = audit;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public int getExpiryAlertDays() {
        return expiryAlertDays;
    }

    // ---------------------------------------------------------------- receive
    @Transactional
    public BloodInventory receive(StockForm f, String actor, String note) {
        return receive(f.getBloodGroup(), f.getComponent(), f.getUnits(), f.getCollectionDate(), f.getExpiryDate(),
                Texts.hasText(f.getNote()) ? f.getNote() : note, actor, false);
    }

    @Transactional
    public BloodInventory receive(BloodGroup bg, ComponentType comp, int units, LocalDate collected, LocalDate expiry,
                                  String note, String actor, boolean demo) {
        if (units < 1) {
            throw new BusinessException("Units must be at least 1");
        }
        LocalDate collection = collected != null ? collected : LocalDate.now();
        if (collection.isAfter(LocalDate.now())) {
            throw new BusinessException("Collection date cannot be in the future");
        }
        LocalDate exp = expiry != null ? expiry : collection.plusDays(DEFAULT_SHELF_LIFE_DAYS.get(comp));
        if (!exp.isAfter(collection)) {
            throw new BusinessException("Expiry date must be after the collection date");
        }
        if (exp.isBefore(LocalDate.now())) {
            throw new BusinessException("This stock is already expired and cannot be added");
        }
        BloodInventory b = new BloodInventory();
        b.setBatchCode(newBatchCode());
        b.setBloodGroup(bg);
        b.setComponent(comp);
        b.setUnits(units);
        b.setUnitsReceived(units);
        b.setCollectionDate(collection);
        b.setExpiryDate(exp);
        b.setDemo(demo);
        repo.save(b);
        record(TxType.RECEIVED, bg, comp, units, b.getBatchCode(), null, note, actor, demo);
        if (!demo) {
            audit.log(actor, "STOCK_RECEIVED", units + " x " + bg.getLabel() + " " + comp.getLabel() + " (" + b.getBatchCode() + ")");
        }
        return b;
    }

    // ---------------------------------------------------------------- issue
    @Transactional
    public void issue(BloodGroup bg, ComponentType comp, int units, Long requestId, String note, String actor) {
        if (units < 1) {
            throw new BusinessException("Units must be at least 1");
        }
        LocalDate today = LocalDate.now();
        List<BloodInventory> batches = repo.lockUsableBatches(bg, comp, today);
        long available = batches.stream().mapToLong(BloodInventory::getUnits).sum();
        if (available < units) {
            throw new BusinessException("Not enough usable stock: " + bg.getLabel() + " " + comp.getLabel()
                    + " has " + available + " unit(s) available, " + units + " requested");
        }
        int remaining = units;
        for (BloodInventory b : batches) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(b.getUnits(), remaining);
            b.setUnits(b.getUnits() - take);
            repo.save(b);
            record(TxType.ISSUED, bg, comp, take, b.getBatchCode(), requestId, note, actor, false);
            remaining -= take;
        }
        audit.log(actor, "STOCK_ISSUED", units + " x " + bg.getLabel() + " " + comp.getLabel()
                + (requestId != null ? " for request #" + requestId : ""));
        refreshAlerts();
    }

    @Transactional
    public void issue(StockForm f, String actor) {
        issue(f.getBloodGroup(), f.getComponent(), f.getUnits(), null, f.getNote(), actor);
    }

    // ---------------------------------------------------------------- expired stock
    /** Writes off expired units as DISCARDED so reports stay consistent. Returns the number of units discarded. */
    @Transactional
    public int discardExpired(String actor) {
        int total = 0;
        for (BloodInventory b : repo.findByUnitsGreaterThanAndExpiryDateBefore(0, LocalDate.now())) {
            int units = b.getUnits();
            b.setUnits(0);
            repo.save(b);
            record(TxType.DISCARDED, b.getBloodGroup(), b.getComponent(), units, b.getBatchCode(), null,
                    "Expired on " + b.getExpiryDate(), actor, b.isDemo());
            total += units;
        }
        if (total > 0) {
            audit.log(actor, "STOCK_DISCARDED", total + " expired unit(s) written off");
        }
        return total;
    }

    // ---------------------------------------------------------------- queries
    /** Usable (not expired) units for each group/component. Every combination is present (0 if none). */
    @Transactional(readOnly = true)
    public Map<BloodGroup, Map<ComponentType, Long>> stockMatrix() {
        Map<BloodGroup, Map<ComponentType, Long>> matrix = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) {
            Map<ComponentType, Long> row = new LinkedHashMap<>();
            for (ComponentType c : ComponentType.values()) {
                row.put(c, 0L);
            }
            matrix.put(g, row);
        }
        for (Object[] r : repo.usableStockSummary(LocalDate.now())) {
            matrix.get((BloodGroup) r[0]).put((ComponentType) r[1], ((Number) r[2]).longValue());
        }
        return matrix;
    }

    @Transactional(readOnly = true)
    public Map<ComponentType, Long> totalsByComponent() {
        Map<ComponentType, Long> totals = new LinkedHashMap<>();
        for (ComponentType c : ComponentType.values()) {
            totals.put(c, 0L);
        }
        stockMatrix().values().forEach(row -> row.forEach((c, v) -> totals.merge(c, v, Long::sum)));
        return totals;
    }

    @Transactional(readOnly = true)
    public Map<BloodGroup, Long> totalsByGroup() {
        Map<BloodGroup, Long> totals = new LinkedHashMap<>();
        stockMatrix().forEach((g, row) -> totals.put(g, row.values().stream().mapToLong(Long::longValue).sum()));
        return totals;
    }

    @Transactional(readOnly = true)
    public long totalUsableUnits() {
        return repo.totalUsableUnits(LocalDate.now());
    }

    /** A low-stock row for display. */
    public record LowStock(BloodGroup group, ComponentType component, long units) {
    }

    @Transactional(readOnly = true)
    public List<LowStock> lowStock() {
        List<LowStock> list = new ArrayList<>();
        stockMatrix().forEach((g, row) -> row.forEach((c, v) -> {
            if (v < lowStockThreshold) {
                list.add(new LowStock(g, c, v));
            }
        }));
        return list;
    }

    @Transactional(readOnly = true)
    public List<BloodInventory> expiringSoon() {
        LocalDate today = LocalDate.now();
        return repo.expiringBetween(today, today.plusDays(expiryAlertDays));
    }

    @Transactional(readOnly = true)
    public List<BloodInventory> expiredWithStock() {
        return repo.findByUnitsGreaterThanAndExpiryDateBefore(0, LocalDate.now());
    }

    /** Batch search for the inventory page. */
    @Transactional(readOnly = true)
    public List<BloodInventory> searchBatches(BloodGroup bg, ComponentType comp, String status, String sort) {
        LocalDate today = LocalDate.now();
        Specification<BloodInventory> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (bg != null) {
                p.add(cb.equal(root.get("bloodGroup"), bg));
            }
            if (comp != null) {
                p.add(cb.equal(root.get("component"), comp));
            }
            if ("expired".equals(status)) {
                p.add(cb.lessThan(root.get("expiryDate"), today));
                p.add(cb.greaterThan(root.get("units"), 0));
            } else if ("expiring".equals(status)) {
                p.add(cb.between(root.get("expiryDate"), today, today.plusDays(expiryAlertDays)));
                p.add(cb.greaterThan(root.get("units"), 0));
            } else if ("empty".equals(status)) {
                p.add(cb.equal(root.get("units"), 0));
            } else {
                p.add(cb.greaterThan(root.get("units"), 0));
                p.add(cb.greaterThanOrEqualTo(root.get("expiryDate"), today));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Sort order = switch (sort == null ? "expiry" : sort) {
            case "units" -> Sort.by(Sort.Direction.DESC, "units");
            case "group" -> Sort.by("bloodGroup", "component");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by("expiryDate");
        };
        return repo.findAll(spec, order);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> history(BloodGroup bg, ComponentType comp, TxType type) {
        Specification<InventoryTransaction> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (bg != null) {
                p.add(cb.equal(root.get("bloodGroup"), bg));
            }
            if (comp != null) {
                p.add(cb.equal(root.get("component"), comp));
            }
            if (type != null) {
                p.add(cb.equal(root.get("type"), type));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return txRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream().limit(300).toList();
    }

    // ---------------------------------------------------------------- alerts
    /** Creates in-app low-stock and expiry notifications for staff (once per alert until read). */
    @Scheduled(fixedDelay = 3600000L, initialDelay = 60000L)
    @Transactional
    public void refreshAlerts() {
        for (LowStock ls : lowStock()) {
            notifications.notifyStaff(NotificationType.LOW_STOCK,
                    "Low stock: " + ls.group().getLabel() + " " + ls.component().getLabel() + " - only " + ls.units()
                            + " unit(s) usable (threshold " + lowStockThreshold + ")",
                    "/inventory", "LOW_STOCK:" + ls.group() + ":" + ls.component());
        }
        for (BloodInventory b : expiringSoon()) {
            notifications.notifyStaff(NotificationType.EXPIRY,
                    "Expiring soon: batch " + b.getBatchCode() + " (" + b.getBloodGroup().getLabel() + " "
                            + b.getComponent().getLabel() + ", " + b.getUnits() + " unit(s)) expires on " + b.getExpiryDate(),
                    "/inventory?status=expiring", "EXPIRY:" + b.getId());
        }
    }

    // ---------------------------------------------------------------- helpers
    private void record(TxType type, BloodGroup bg, ComponentType comp, int units, String batch, Long requestId,
                        String note, String actor, boolean demo) {
        InventoryTransaction t = new InventoryTransaction();
        t.setType(type);
        t.setBloodGroup(bg);
        t.setComponent(comp);
        t.setUnits(units);
        t.setBatchCode(batch);
        t.setRequestId(requestId);
        t.setNote(note == null ? null : (note.length() > 300 ? note.substring(0, 300) : note));
        t.setPerformedBy(actor);
        t.setDemo(demo);
        txRepo.save(t);
    }

    private String newBatchCode() {
        return "LL-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
