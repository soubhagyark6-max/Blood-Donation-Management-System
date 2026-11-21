# LifeLink - Smart Blood Donation and Blood Bank Management System

**MCA Final-Year Project**

## 1. Project synopsis

LifeLink is a web application that brings blood donors, patients' families (recipients), blood bank staff and administrators onto one secure platform. Donors register and manage their profile and availability, book appointments at donation camps and keep a donation history. Recipients raise blood requests and follow them through a defined workflow. Blood bank staff manage component-wise inventory with expiry and low-stock alerts, review requests, record donations and use a preliminary donor-matching helper. Administrators manage staff accounts, view analytics and the audit log.

**Technology:** Java 21, Spring Boot 3.3 (Spring MVC, Spring Data JPA/Hibernate, Spring Security with BCrypt), Thymeleaf, Bootstrap 5, Chart.js, MySQL 8, Maven, JUnit 5.

## 2. Abstract

Blood banks in many places still depend on registers, phone calls and spreadsheets. This causes slow response to emergencies, poor visibility of stock and expiry, and no reliable history of requests. LifeLink digitises these processes. It provides role-based access (Admin, Staff, Donor, Recipient), a request workflow (Pending -> Under Review -> Approved -> Fulfilled, or Rejected/Cancelled) with a complete status history, component-wise inventory (whole blood, red cells, platelets, plasma) managed with database transactions so stock can never become negative, donation camps with appointment booking and QR references, in-app notifications for emergencies, low stock and expiry, database-driven dashboards with charts, date-filtered reports with CSV export and an audit log.

LifeLink is an *assistance and management* tool. It does **not** decide medical eligibility, blood compatibility, cross-matching or release of blood; these remain the responsibility of qualified blood bank professionals. The donor-matching feature is clearly labelled as preliminary.

## 3. Problem statement

1. Patients' families struggle to find which blood group and component is available and to follow the progress of a request.
2. Blood banks lack a single view of stock by group and component, of near-expiry batches and of low-stock situations.
3. Donor records are scattered; contacting suitable donors for urgent needs is slow and privacy is hard to protect.
4. Camps, appointments and donation histories are managed manually.
5. There is no audit trail of who changed a request or stock and when.

## 4. Objectives and scope

### Objectives
- Provide secure, role-based access with hashed passwords and protected routes.
- Manage donors, donations, camps and appointments.
- Manage blood requests with an enforced status workflow, remarks and history.
- Maintain component-wise inventory with transactions, alerts and movement history.
- Offer preliminary donor matching with transparent reasons.
- Present live dashboards, reports and CSV export.
- Keep an audit log of important actions.

### In scope (implemented)
Authentication and roles; donor module; request module; inventory module; matching helper; camps and appointments with QR code; notifications (in-app); dashboards and charts; reports and CSV; audit log; English/Kannada interface for navigation and main public pages; FAQ page with keyword help search; demo data (labelled DEMO).

### Out of scope / not implemented
E-mail or SMS delivery, online payments, hospital system integration (HL7/FHIR), mobile apps, GPS/maps, full Kannada translation of every page, password-reset by e-mail, medical eligibility rules, automatic cross-matching.


---

# Existing and Proposed Systems, Requirements

## 5. Existing and proposed systems

### Existing (manual or semi-manual) system
- Paper registers or spreadsheets for donors and stock.
- Requests by phone or walk-in; no visible status for the family.
- Expiry and low stock noticed only by manual checking.
- No role separation and no audit trail; personal data widely visible.

### Limitations of the existing system
Slow response, human error in stock counts, duplicated donor entries, no analytics, no accountability, privacy risks.

### Proposed system - LifeLink
| Aspect | Proposed solution |
|---|---|
| Access | Role-based login (Admin, Staff, Donor, Recipient), BCrypt passwords, CSRF protection |
| Requests | Online request with urgency, enforced workflow, remarks, history, notifications |
| Inventory | Batch-wise stock per group and component, FIFO by earliest expiry, no negative stock, ledger of movements |
| Donors | Registration with duplicate checks, search by group/city, staff verification flag, contact details only for staff |
| Camps | Create camps, book/cancel appointments, QR reference, staff registration list |
| Insight | Dashboards, charts, reports, CSV export, audit log |

### Advantages
Faster response, transparency, accurate stock with expiry control, privacy by design, measurable operations.

## 6. Software and hardware requirements

### Software
| Item | Requirement |
|---|---|
| Operating system | Windows 10/11 (also Linux/macOS) |
| JDK | Java 21 (e.g. Temurin / OpenJDK 21) |
| Build tool | Apache Maven 3.9+ (or the IDE's bundled Maven) |
| Database | MySQL 8.x |
| Browser | Current Chrome, Edge or Firefox (Internet needed for CDN libraries: Bootstrap, Chart.js, QR, fonts) |
| IDE | IntelliJ IDEA or VS Code with the Java extension pack (enable annotation processing for Lombok) |

### Hardware (minimum)
- Dual-core CPU, 4 GB RAM (8 GB recommended), 1 GB free disk, network connection.
