package com.lifelink.config;

import com.lifelink.dto.BloodRequestForm;
import com.lifelink.dto.StatusUpdateForm;
import com.lifelink.model.*;
import com.lifelink.repository.*;
import com.lifelink.service.AppointmentService;
import com.lifelink.service.InventoryService;
import com.lifelink.service.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Loads DEMO accounts and sample records on first start (only when the users table is empty).
 * Everything created here carries demo=true and is labelled "DEMO" in the user interface.
 * Disable with DEMO_DATA_ENABLED=false.
 */
@Component
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository users;
    private final DonorRepository donors;
    private final DonationRepository donations;
    private final DonationCampRepository camps;
    private final BloodRequestRepository requestRepo;
    private final BloodInventoryRepository inventoryRepo;
    private final InventoryService inventory;
    private final RequestService requests;
    private final AppointmentService appointments;
    private final PasswordEncoder encoder;

    @Value("${lifelink.demo-data.enabled:true}")
    private boolean enabled;

    @Value("${lifelink.demo-data.password:Demo@12345}")
    private String demoPassword;

    public DemoDataLoader(UserRepository users, DonorRepository donors, DonationRepository donations,
                          DonationCampRepository camps, BloodRequestRepository requestRepo,
                          BloodInventoryRepository inventoryRepo, InventoryService inventory, RequestService requests,
                          AppointmentService appointments, PasswordEncoder encoder) {
        this.users = users;
        this.donors = donors;
        this.donations = donations;
        this.camps = camps;
        this.requestRepo = requestRepo;
        this.inventoryRepo = inventoryRepo;
        this.inventory = inventory;
        this.requests = requests;
        this.appointments = appointments;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled || users.count() > 0) {
            return;
        }
        log.info("Loading DEMO data (set DEMO_DATA_ENABLED=false to disable)...");
        User admin = user("Demo Admin", "admin@lifelink.demo", Role.ADMIN);
        User staff = user("Demo Staff", "staff@lifelink.demo", Role.STAFF);
        User recipient = user("Demo Recipient", "recipient@lifelink.demo", Role.RECIPIENT);
        User donorUser = user("Demo Donor", "donor@lifelink.demo", Role.DONOR);

        // --- donors ---
        Donor main = donor(donorUser, "Demo Donor", "donor@lifelink.demo", "9000000001", BloodGroup.O_POS, "Bengaluru", 28, 120, true, true);
        String[][] people = {
                {"Anitha Rao", "9000000002", "A_POS", "Bengaluru", "35", "200"},
                {"Mahesh Gowda", "9000000003", "B_POS", "Mysuru", "41", "30"},
                {"Divya Shetty", "9000000004", "O_NEG", "Mangaluru", "26", "150"},
                {"Imran Khan", "9000000005", "AB_POS", "Bengaluru", "33", "95"},
                {"Kavya Hegde", "9000000006", "A_NEG", "Hubballi", "30", "0"},
                {"Suresh Patil", "9000000007", "O_POS", "Belagavi", "45", "180"},
                {"Lakshmi Naik", "9000000008", "B_NEG", "Bengaluru", "29", "100"},
                {"Rahul Desai", "9000000009", "O_POS", "Bengaluru", "38", "60"},
                {"Pooja Kulkarni", "9000000010", "AB_NEG", "Mysuru", "27", "0"},
                {"Naveen Kumar", "9000000011", "A_POS", "Bengaluru", "50", "220"},
                {"Shruthi Reddy", "9000000012", "O_POS", "Bengaluru", "31", "110"}
        };
        int i = 0;
        for (String[] p : people) {
            Donor d = donor(null, p[0], p[0].toLowerCase().replace(' ', '.') + "@demo.lifelink", p[1],
                    BloodGroup.valueOf(p[2]), p[3], Integer.parseInt(p[4]), Integer.parseInt(p[5]), i % 3 != 2, i % 2 == 0);
            if (Integer.parseInt(p[5]) > 0 && i % 2 == 0) {
                donation(d, LocalDate.now().minusDays(Integer.parseInt(p[5])), staff);
            }
            i++;
        }
        donation(main, LocalDate.now().minusDays(120), staff);
        donation(main, LocalDate.now().minusDays(300), staff);

        // --- inventory (batches recorded through the same service that staff use) ---
        LocalDate today = LocalDate.now();
        for (BloodGroup g : BloodGroup.values()) {
            int base = switch (g) {
                case O_POS -> 18;
                case A_POS, B_POS -> 12;
                case O_NEG, A_NEG -> 7;
                default -> 6;
            };
            inventory.receive(g, ComponentType.WHOLE_BLOOD, base, today.minusDays(8), null, "DEMO opening stock", "demo-loader", true);
            inventory.receive(g, ComponentType.RED_CELLS, Math.max(2, base - 3), today.minusDays(5), null, "DEMO opening stock", "demo-loader", true);
            inventory.receive(g, ComponentType.PLASMA, Math.max(2, base - 2), today.minusDays(20), null, "DEMO opening stock", "demo-loader", true);
        }
        // platelets: deliberately thin so the low-stock alert is visible in the demo
        inventory.receive(BloodGroup.O_POS, ComponentType.PLATELETS, 6, today.minusDays(1), null, "DEMO opening stock", "demo-loader", true);
        inventory.receive(BloodGroup.A_POS, ComponentType.PLATELETS, 5, today.minusDays(2), null, "DEMO opening stock", "demo-loader", true);
        inventory.receive(BloodGroup.AB_NEG, ComponentType.PLATELETS, 2, today.minusDays(2), null, "DEMO opening stock", "demo-loader", true);
        // a batch that expires in 3 days (expiry alert)
        inventory.receive(BloodGroup.B_POS, ComponentType.RED_CELLS, 4, today.minusDays(39), today.plusDays(3), "DEMO near-expiry batch", "demo-loader", true);
        // an already-expired batch (can be written off with "Discard expired")
        BloodInventory expired = new BloodInventory();
        expired.setBatchCode("LL-DEMO-EXPD");
        expired.setBloodGroup(BloodGroup.A_NEG);
        expired.setComponent(ComponentType.PLATELETS);
        expired.setUnits(2);
        expired.setUnitsReceived(2);
        expired.setCollectionDate(today.minusDays(9));
        expired.setExpiryDate(today.minusDays(2));
        expired.setDemo(true);
        inventoryRepo.save(expired);

        // --- camps ---
        DonationCamp c1 = camp("Community Blood Donation Drive", today.plusDays(6), "Bengaluru", "Jayanagar Community Hall", "Ravi Kumar", "9000000101", 60);
        camp("College Campus Blood Camp", today.plusDays(14), "Mysuru", "UBDT College Auditorium", "Dr. Meena", "9000000102", 80);
        camp("Corporate Park Donation Day", today.plusDays(25), "Bengaluru", "Tech Park Atrium", "Sandeep Jain", "9000000103", 100);
        camp("Past camp (demo)", today.minusDays(30), "Hubballi", "Town Hall", "Asha Patil", "9000000104", 40);
        appointments.book(c1.getId(), donorUser);

        // --- requests (created through the real service so history + notifications exist) ---
        BloodRequest r1 = demoRequest(recipient, "Patient ref P-1001", BloodGroup.O_POS, ComponentType.WHOLE_BLOOD, 2, "City General Hospital", "Bengaluru", Urgency.NORMAL, today.plusDays(2));
        BloodRequest r2 = demoRequest(recipient, "Patient ref P-1002", BloodGroup.A_POS, ComponentType.PLATELETS, 2, "Lakeside Medical Centre", "Bengaluru", Urgency.EMERGENCY, today);
        BloodRequest r3 = demoRequest(recipient, "Patient ref P-1003", BloodGroup.B_POS, ComponentType.RED_CELLS, 1, "District Hospital", "Mysuru", Urgency.URGENT, today.plusDays(1));
        BloodRequest r4 = demoRequest(recipient, "Patient ref P-1004", BloodGroup.AB_NEG, ComponentType.PLATELETS, 4, "Sunrise Hospital", "Mangaluru", Urgency.NORMAL, today.plusDays(5));
        BloodRequest r5 = demoRequest(recipient, "Patient ref P-1005", BloodGroup.O_NEG, ComponentType.WHOLE_BLOOD, 1, "City General Hospital", "Bengaluru", Urgency.NORMAL, today.plusDays(3));

        status(r1, RequestStatus.UNDER_REVIEW, "Verifying hospital details", staff);
        status(r1, RequestStatus.APPROVED, "Approved by blood bank", staff);
        status(r1, RequestStatus.FULFILLED, "2 units issued after cross-matching by lab", staff);
        status(r3, RequestStatus.UNDER_REVIEW, "Checking stock", staff);
        status(r4, RequestStatus.REJECTED, "Insufficient platelet stock - please contact another bank", staff);
        status(r5, RequestStatus.UNDER_REVIEW, "Documents requested", staff);
        // r2 stays PENDING + EMERGENCY so the highlight is visible

        inventory.refreshAlerts();
        log.info("DEMO data ready. Demo accounts: admin@lifelink.demo, staff@lifelink.demo, donor@lifelink.demo, recipient@lifelink.demo");
    }

    // ---------------------------------------------------------------- helpers
    private User user(String name, String email, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(demoPassword));
        u.setRole(role);
        u.setDemo(true);
        return users.save(u);
    }

    private Donor donor(User user, String name, String email, String phone, BloodGroup bg, String city, int age,
                        int daysSinceDonation, boolean available, boolean verified) {
        Donor d = new Donor();
        d.setUser(user);
        d.setFullName(name);
        d.setEmail(email);
        d.setPhone(phone);
        d.setDateOfBirth(LocalDate.now().minusYears(age).minusDays(40));
        d.setGender(name.startsWith("Anitha") || name.startsWith("Divya") || name.startsWith("Kavya")
                || name.startsWith("Lakshmi") || name.startsWith("Pooja") || name.startsWith("Shruthi") ? Gender.FEMALE : Gender.MALE);
        d.setBloodGroup(bg);
        d.setCity(city);
        d.setAddress("DEMO address, " + city);
        d.setLastDonationDate(daysSinceDonation > 0 ? LocalDate.now().minusDays(daysSinceDonation) : null);
        d.setAvailable(available);
        d.setVerified(verified);
        d.setDemo(true);
        return donors.save(d);
    }

    private void donation(Donor d, LocalDate date, User staff) {
        Donation x = new Donation();
        x.setDonor(d);
        x.setDonationDate(date);
        x.setComponent(ComponentType.WHOLE_BLOOD);
        x.setUnits(1);
        x.setLocation("DEMO donation record");
        x.setStaffVerified(true);
        x.setRecordedBy(staff);
        x.setDemo(true);
        donations.save(x);
    }

    private DonationCamp camp(String name, LocalDate date, String city, String venue, String contact, String phone, int capacity) {
        DonationCamp c = new DonationCamp();
        c.setName(name);
        c.setCampDate(date);
        c.setStartTime(LocalTime.of(9, 0));
        c.setEndTime(LocalTime.of(16, 0));
        c.setVenue(venue);
        c.setCity(city);
        c.setContactName(contact);
        c.setContactPhone(phone);
        c.setCapacity(capacity);
        c.setDescription("DEMO camp - sample data for the project demonstration.");
        c.setDemo(true);
        return camps.save(c);
    }

    private BloodRequest demoRequest(User requester, String ref, BloodGroup bg, ComponentType comp, int units,
                                     String hospital, String city, Urgency urgency, LocalDate required) {
        BloodRequestForm f = new BloodRequestForm();
        f.setPatientReference(ref);
        f.setBloodGroup(bg);
        f.setComponent(comp);
        f.setUnits(units);
        f.setHospital(hospital);
        f.setCity(city);
        f.setContactPhone("9000000999");
        f.setRequiredDate(required);
        f.setUrgency(urgency);
        BloodRequest r = requests.create(f, requester);
        r.setDemo(true);
        return requestRepo.save(r);
    }

    private void status(BloodRequest r, RequestStatus next, String remarks, User staff) {
        StatusUpdateForm f = new StatusUpdateForm();
        f.setStatus(next);
        f.setRemarks(remarks);
        requests.changeStatus(r.getId(), f, staff);
    }
}
