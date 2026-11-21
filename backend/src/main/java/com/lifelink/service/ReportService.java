package com.lifelink.service;

import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationRepository;
import com.lifelink.repository.DonorRepository;
import com.lifelink.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Date-filtered reports and CSV export (staff/admin only - enforced in the controller). */
@Service
public class ReportService {

    private final BloodRequestRepository requests;
    private final InventoryTransactionRepository txRepo;
    private final DonationRepository donations;
    private final DonorRepository donors;

    public ReportService(BloodRequestRepository requests, InventoryTransactionRepository txRepo,
                         DonationRepository donations, DonorRepository donors) {
        this.requests = requests;
        this.txRepo = txRepo;
        this.donations = donations;
        this.donors = donors;
    }

    public record Summary(long requestsTotal, Map<RequestStatus, Long> byStatus, long emergencyCount,
                          long unitsRequested, long unitsReceived, long unitsIssued, long unitsDiscarded,
                          long donationsCount) {
    }

    @Transactional(readOnly = true)
    public List<BloodRequest> requestsBetween(LocalDate from, LocalDate to) {
        LocalDateTime a = from.atStartOfDay();
        LocalDateTime b = to.plusDays(1).atStartOfDay();
        return requests.findAll().stream()
                .filter(r -> !r.getCreatedAt().isBefore(a) && r.getCreatedAt().isBefore(b))
                .sorted(Comparator.comparing(BloodRequest::getCreatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> movementsBetween(LocalDate from, LocalDate to) {
        return txRepo.findByCreatedAtBetweenOrderByCreatedAtDesc(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public Summary summary(LocalDate from, LocalDate to) {
        List<BloodRequest> rs = requestsBetween(from, to);
        Map<RequestStatus, Long> byStatus = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) {
            byStatus.put(s, 0L);
        }
        long emergency = 0;
        long unitsRequested = 0;
        for (BloodRequest r : rs) {
            byStatus.merge(r.getStatus(), 1L, Long::sum);
            if (r.getUrgency() == Urgency.EMERGENCY) {
                emergency++;
            }
            unitsRequested += r.getUnits();
        }
        long received = 0;
        long issued = 0;
        long discarded = 0;
        for (InventoryTransaction t : movementsBetween(from, to)) {
            switch (t.getType()) {
                case RECEIVED -> received += t.getUnits();
                case ISSUED -> issued += t.getUnits();
                case DISCARDED -> discarded += t.getUnits();
            }
        }
        long donationCount = donations.findByDonationDateGreaterThanEqual(from).stream()
                .filter(d -> !d.getDonationDate().isAfter(to)).count();
        return new Summary(rs.size(), byStatus, emergency, unitsRequested, received, issued, discarded, donationCount);
    }

    // ---------------------------------------------------------------- CSV
    @Transactional(readOnly = true)
    public String requestsCsv(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("Request ID,Created,Patient reference,Blood group,Component,Units,Hospital,City,Required date,Urgency,Status,Staff remarks\n");
        for (BloodRequest r : requestsBetween(from, to)) {
            row(sb, r.getId(), r.getCreatedAt().toLocalDate(), r.getPatientReference(), r.getBloodGroup().getLabel(),
                    r.getComponent().getLabel(), r.getUnits(), r.getHospital(), r.getCity(), r.getRequiredDate(),
                    r.getUrgency().getLabel(), r.getStatus().getLabel(), r.getStaffRemarks());
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String movementsCsv(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("Date/time,Type,Blood group,Component,Units,Batch,Request ID,Performed by,Note\n");
        for (InventoryTransaction t : movementsBetween(from, to)) {
            row(sb, t.getCreatedAt(), t.getType().getLabel(), t.getBloodGroup().getLabel(), t.getComponent().getLabel(),
                    t.getUnits(), t.getBatchCode(), t.getRequestId(), t.getPerformedBy(), t.getNote());
        }
        return sb.toString();
    }

    /** Donor export deliberately omits e-mail, phone and address (privacy). */
    @Transactional(readOnly = true)
    public String donorsCsv() {
        StringBuilder sb = new StringBuilder("Donor ID,Name,Blood group,City,Available,Verified,Last donation date\n");
        for (Donor d : donors.findAll()) {
            row(sb, d.getId(), d.getFullName(), d.getBloodGroup().getLabel(), d.getCity(),
                    d.isAvailable() ? "Yes" : "No", d.isVerified() ? "Yes" : "No", d.getLastDonationDate());
        }
        return sb.toString();
    }

    private void row(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csv(cells[i]));
        }
        sb.append('\n');
    }

    /** Escapes a CSV cell and neutralises spreadsheet formulas (=, +, -, @) typed by users. */
    static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
