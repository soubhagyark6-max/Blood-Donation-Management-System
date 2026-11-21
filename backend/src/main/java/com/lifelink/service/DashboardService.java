package com.lifelink.service;

import com.lifelink.model.*;
import com.lifelink.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/** Builds the numbers shown on the dashboards. Every figure is read from the database. */
@Service
public class DashboardService {

    private final DonorRepository donors;
    private final BloodRequestRepository requests;
    private final DonationRepository donations;
    private final InventoryTransactionRepository txRepo;
    private final InventoryService inventory;
    private final CampService camps;
    private final AppointmentRepository appointments;
    private final DonorService donorService;

    public DashboardService(DonorRepository donors, BloodRequestRepository requests, DonationRepository donations,
                            InventoryTransactionRepository txRepo, InventoryService inventory, CampService camps,
                            AppointmentRepository appointments, DonorService donorService) {
        this.donors = donors;
        this.requests = requests;
        this.donations = donations;
        this.txRepo = txRepo;
        this.inventory = inventory;
        this.camps = camps;
        this.appointments = appointments;
        this.donorService = donorService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> staffDashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("donorCount", donors.count());
        m.put("requestCount", requests.count());
        m.put("pendingCount", requests.countByStatus(RequestStatus.PENDING));
        m.put("reviewCount", requests.countByStatus(RequestStatus.UNDER_REVIEW));
        m.put("fulfilledCount", requests.countByStatus(RequestStatus.FULFILLED));
        m.put("totalUnits", inventory.totalUsableUnits());
        m.put("upcomingCamps", camps.upcomingCount());
        m.put("lowStock", inventory.lowStock());
        m.put("expiring", inventory.expiringSoon());
        m.put("unitsByComponent", inventory.totalsByComponent());
        m.put("recentRequests", requests.findTop8ByOrderByCreatedAtDesc());
        m.put("recentTransactions", txRepo.findTop15ByOrderByCreatedAtDesc());
        m.put("recentDonations", donations.findTop10ByOrderByCreatedAtDesc());

        // --- chart data ---
        Map<String, Long> donorsByGroup = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) {
            donorsByGroup.put(g.getLabel(), 0L);
        }
        for (Object[] r : donors.countGroupedByBloodGroup()) {
            donorsByGroup.put(((BloodGroup) r[0]).getLabel(), ((Number) r[1]).longValue());
        }
        m.put("groupLabels", new ArrayList<>(donorsByGroup.keySet()));
        m.put("groupValues", new ArrayList<>(donorsByGroup.values()));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            byStatus.put(s.getLabel(), 0L);
        }
        for (Object[] r : requests.countGroupedByStatus()) {
            byStatus.put(((RequestStatus) r[0]).getLabel(), ((Number) r[1]).longValue());
        }
        m.put("statusLabels", new ArrayList<>(byStatus.keySet()));
        m.put("statusValues", new ArrayList<>(byStatus.values()));

        List<String> compLabels = new ArrayList<>();
        List<Long> compValues = new ArrayList<>();
        inventory.totalsByComponent().forEach((c, v) -> {
            compLabels.add(c.getLabel());
            compValues.add(v);
        });
        m.put("componentLabels", compLabels);
        m.put("componentValues", compValues);

        List<String> invGroupLabels = new ArrayList<>();
        List<Long> invGroupValues = new ArrayList<>();
        inventory.totalsByGroup().forEach((g, v) -> {
            invGroupLabels.add(g.getLabel());
            invGroupValues.add(v);
        });
        m.put("invGroupLabels", invGroupLabels);
        m.put("invGroupValues", invGroupValues);

        // monthly activity: last 6 months
        YearMonth thisMonth = YearMonth.now();
        YearMonth start = thisMonth.minusMonths(5);
        Map<YearMonth, Long> reqPerMonth = new LinkedHashMap<>();
        Map<YearMonth, Long> donPerMonth = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            reqPerMonth.put(start.plusMonths(i), 0L);
            donPerMonth.put(start.plusMonths(i), 0L);
        }
        for (BloodRequest r : requests.findByCreatedAtGreaterThanEqual(start.atDay(1).atStartOfDay())) {
            reqPerMonth.merge(YearMonth.from(r.getCreatedAt()), 1L, Long::sum);
        }
        for (Donation d : donations.findByDonationDateGreaterThanEqual(start.atDay(1))) {
            donPerMonth.merge(YearMonth.from(d.getDonationDate()), 1L, Long::sum);
        }
        List<String> months = new ArrayList<>();
        for (YearMonth ym : reqPerMonth.keySet()) {
            months.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear());
        }
        m.put("monthLabels", months);
        m.put("monthRequests", new ArrayList<>(reqPerMonth.values()));
        m.put("monthDonations", new ArrayList<>(donPerMonth.values()));
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> donorDashboard(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        Donor d = donorService.getByUser(user);
        m.put("donor", d);
        m.put("donationCount", donations.countByDonorId(d.getId()));
        m.put("bookedCount", appointments.countByDonorIdAndStatus(d.getId(), AppointmentStatus.BOOKED));
        m.put("nextReminder", donorService.nextReminderDate(d));
        m.put("intervalDays", donorService.getDonationIntervalDays());
        m.put("today", LocalDate.now());
        m.put("camps", camps.upcoming());
        m.put("history", donations.findByDonorIdOrderByDonationDateDesc(d.getId()));
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> recipientDashboard(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", requests.countByRequesterId(user.getId()));
        m.put("pending", requests.countByRequesterIdAndStatus(user.getId(), RequestStatus.PENDING));
        m.put("approved", requests.countByRequesterIdAndStatus(user.getId(), RequestStatus.APPROVED));
        m.put("fulfilled", requests.countByRequesterIdAndStatus(user.getId(), RequestStatus.FULFILLED));
        m.put("recent", requests.findByRequesterIdOrderByCreatedAtDesc(user.getId()));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            byStatus.put(s.getLabel(), 0L);
        }
        for (Object[] r : requests.countGroupedByStatusForUser(user.getId())) {
            byStatus.put(((RequestStatus) r[0]).getLabel(), ((Number) r[1]).longValue());
        }
        m.put("statusLabels", new ArrayList<>(byStatus.keySet()));
        m.put("statusValues", new ArrayList<>(byStatus.values()));
        return m;
    }
}
