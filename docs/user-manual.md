# Installation and User Manual

## 16.1 Installation (Windows)
See the step-by-step guide in the project's `README.md` (or double-click `windows/run-project.bat`). In short: install JDK 21, Maven and MySQL 8; create `.env` from `.env.example`; run `mvn spring-boot:run`; open http://localhost:8080.

## 16.2 Demo accounts (DEMO data)
| Role | E-mail | Password |
|---|---|---|
| Admin | admin@lifelink.demo | Demo@12345 |
| Staff | staff@lifelink.demo | Demo@12345 |
| Donor | donor@lifelink.demo | Demo@12345 |
| Recipient | recipient@lifelink.demo | Demo@12345 |

The password comes from `DEMO_PASSWORD` in `.env`. Set `DEMO_DATA_ENABLED=false` for a clean database. All demo rows carry a **DEMO** badge.

## 16.3 User manual
### Donor
1. Register (Register -> I want to donate blood). 2. Log in; the dashboard shows blood group, donation count, reminder date and upcoming camps. 3. *My Profile* - update contact details, availability, contact preference. 4. *Donation History* - add past donations (self-reported). 5. *Camps* - book an appointment; *My Appointments* shows the QR code; cancel if needed. 6. Bell icon - notifications.

### Recipient
1. Register (I need blood). 2. *New Request* - fill patient reference, group, component, units, hospital, city, phone, date, urgency. 3. *Blood Requests* - search/filter, open a request to see the status history and staff remarks. 4. Edit or cancel while the request is Pending.

### Blood bank staff
1. Dashboard - alerts, counts, charts. 2. *Blood Requests* - open a request, choose the next status, add remarks (required for rejection). Use *Suggest donors* for a preliminary shortlist. 3. *Inventory* - receive stock, issue stock, filter batches, discard expired units, view movement history. 4. *Donors* - search, verify, edit, record a donation (optionally add to stock). 5. *Camps* - create/edit/cancel camps, open registrations, mark attended/no-show. 6. *Reports* - choose dates, export CSV, print.

### Administrator
Everything staff can do, plus *Users* (create staff/admin, enable/disable) and *Audit Log*.

## 16.4 Troubleshooting
| Problem | Fix |
|---|---|
| `Access denied for user 'root'` | wrong `DB_PASSWORD` in `.env` |
| `Communications link failure` | MySQL not running / wrong port |
| `release version 21 not supported` | JDK older than 21 - install JDK 21 and set `JAVA_HOME` |
| Port 8080 busy | set `SERVER_PORT=8081` |
| Page looks unstyled / no charts | no internet access for CDN libraries |
