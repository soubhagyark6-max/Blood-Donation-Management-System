# Software Requirements Specification (SRS)

## 1. Introduction
**Purpose.** Defines the requirements of LifeLink. **Intended readers:** project guide, examiners, developers, testers. **Product scope:** see synopsis.md.

## 2. Overall description
- **Product perspective:** stand-alone web application, three-tier (browser, Spring Boot server, MySQL).
- **User classes:** Administrator, Blood Bank Staff, Donor, Recipient.
- **Operating environment:** Java 21 JVM, MySQL 8, modern browser.
- **Constraints:** notifications are in-app only; medical decisions are outside the system.
- **Assumptions:** staff enter stock accurately; users have internet for CDN assets.

## 3. Functional requirements

| ID | Requirement | Actor |
|---|---|---|
| FR-1 | Register as donor (name, DOB, gender, blood group, phone, e-mail, city, address, optional last donation, contact preference) | Donor |
| FR-2 | Register as recipient | Recipient |
| FR-3 | Log in / log out; accounts can be disabled | All |
| FR-4 | Reject duplicate e-mail or phone; reject under-18 donor accounts; passwords need 8+ chars with letters and digits | System |
| FR-5 | Donor edits profile, availability, contact preference | Donor |
| FR-6 | Donor adds own donation history (shown as self-reported) | Donor |
| FR-7 | Staff/admin search and filter donors by group, city, text, availability, verified | Staff, Admin |
| FR-8 | Staff verify donors, edit donors, record donations (optionally add units to stock) | Staff, Admin |
| FR-9 | Recipient creates a request: patient reference, group, component, units, hospital, city, phone, required date, urgency | Recipient |
| FR-10 | Recipient views history, edits or cancels while Pending | Recipient |
| FR-11 | Staff change status along the allowed workflow; rejection needs a remark; every change stored in history | Staff, Admin |
| FR-12 | Fulfilling a request issues stock in the same transaction; fails if stock is insufficient | System |
| FR-13 | Emergency requests are highlighted, listed first and notify all staff | System |
| FR-14 | Staff receive and issue stock per group and component; batches have collection and expiry dates | Staff, Admin |
| FR-15 | Stock never negative; expired units are excluded and can be written off | System |
| FR-16 | Low-stock and expiry alerts as in-app notifications | System |
| FR-17 | Inventory movement history with filters | Staff, Admin |
| FR-18 | Preliminary donor matching: group, city, availability, last donation, verification; shows reasons and a disclaimer | Staff, Admin |
| FR-19 | Create/edit/cancel camps; donors book/cancel appointments; QR reference; staff see registrations and mark attendance | Staff, Donor |
| FR-20 | Role-specific dashboards with live figures and charts | All |
| FR-21 | Reports by date range with summary, tables, CSV export (donor CSV has no contact data) and print | Staff, Admin |
| FR-22 | Audit log of important actions | Admin |
| FR-23 | English/Kannada switch; FAQ and keyword help | All |
| FR-24 | Admin creates staff/admin accounts and enables/disables users | Admin |

## 4. Non-functional requirements
- **Security:** BCrypt hashing, CSRF, role-based URL rules plus ownership checks in services, server-side validation, no secrets in code (environment variables), generic error pages without stack traces.
- **Reliability:** transactional stock changes with pessimistic locking and optimistic version field.
- **Usability:** responsive Bootstrap UI, clear validation messages, success/error notifications.
- **Performance:** indexed columns for search; suitable for a single blood bank.
- **Maintainability:** layered architecture (controller, service, repository), tests for important rules.
- **Privacy:** contact details only for staff/admin; CSV exports omit contact data.

## 5. External interfaces
Browser (HTTP/HTML), MySQL via JDBC, CDN for Bootstrap, Bootstrap Icons, Chart.js, qrcode.js and Google Fonts.

## 6. Status workflow rules
Pending -> Under Review | Approved | Rejected; Under Review -> Approved | Rejected; Approved -> Fulfilled | Rejected; Pending -> Cancelled (requester only). Fulfilled, Rejected and Cancelled are final.
