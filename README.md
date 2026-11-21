# LifeLink - Smart Blood Donation and Blood Bank Management System
MCA final-year project - Java 21, Spring Boot 3, Thymeleaf, Bootstrap 5, MySQL 8.

> ## Honest status - read first
> This project was written in an environment with **no access to Maven Central**, so it has **never been compiled, tested or started** by its author. It has been syntax-checked and cross-checked (field names, templates, message keys, and a simulation proving the folder layout produces the same classpath as the standard Spring Boot layout), but **`mvn clean test` and `mvn spring-boot:run` still have to be run by you** (Part 4). If anything fails, send the first `[ERROR]` lines to Claude for a fix.

## 1. Introduction
LifeLink connects blood donors, recipients (patients' families), blood bank staff and administrators. Features: role-based login (Admin, Staff, Donor, Recipient); donor management with duplicate checks and staff verification; blood requests with Pending -> Under Review -> Approved -> Fulfilled / Rejected / Cancelled workflow, remarks and history; emergency highlighting; component-wise inventory (whole blood, red cells, platelets, plasma) with batches, expiry and low-stock alerts and movement history; **preliminary** donor matching (never a medical decision); donation camps and appointments with QR code; in-app notifications; dashboards with Chart.js; reports with CSV export; audit log; FAQ; English/Kannada (navigation and main pages); demo data labelled DEMO.
Not implemented: e-mail/SMS, password reset by e-mail, full Kannada translation of every page.

## 2. Technologies used
Java 21 - Spring Boot 3.3 (MVC, Data JPA/Hibernate, Security with BCrypt, Validation) - Thymeleaf - HTML5/CSS3/JavaScript - Bootstrap 5 - Chart.js - qrcode.js - MySQL 8 - Maven - JUnit 5, Spring Boot Test, MockMvc, H2 (tests only) - Lombok.

## 3. Folder structure
```
LifeLink-Blood-Donation-Management/
├── frontend/
│   ├── pages/          Thymeleaf HTML pages (home, login, register-*, faq, dashboard/, donors/, requests/,
│   │                   inventory/, camps/, appointments/, notifications/, reports/, admin/, error/)
│   ├── components/     layout.html  (shared head, navbar, flash messages, footer, scripts)
│   ├── css/            lifelink.css (red / white / navy theme)
│   ├── js/             app.js (validation, QR, charts helper), live-stats.js (REST call)
│   └── images/         logo.svg, hero-art.svg
├── backend/
│   └── src/main/
│       ├── java/com/lifelink/
│       │   ├── LifeLinkApplication.java
│       │   ├── config/        SecurityConfig, AppUserDetailsService, WebConfig, DemoDataLoader
│       │   ├── controller/    page controllers + ApiController (REST)
│       │   ├── service/       business logic
│       │   ├── repository/    Spring Data JPA
│       │   ├── model/         entities and enums
│       │   ├── dto/           validated forms
│       │   └── exception/
│       └── resources/         application.properties, messages*.properties (English / Kannada)
├── database/
│   ├── schema.sql, sample-data.sql
│   └── database-config/       README.md, create-app-user.sql, application-mysql.properties.example
├── docs/                      synopsis, SRS, architecture, design-and-modules, test-plan, user-manual,
│   │                          viva-questions, future-enhancements-conclusion
│   └── UML-diagrams/          use-case, class, ER, DFD 0/1, state, flowchart, sequence x2, activity x2 (PNG + .mmd)
├── tests/
│   ├── backend-tests/         java/ (JUnit 5 + MockMvc tests), resources/ (H2 test configuration)
│   └── frontend-tests/        frontend-test-checklist.md, check-frontend-files.ps1
├── reports/project-report.md  complete combined MCA report
├── windows/                   run-project.bat, run-tests.bat
├── .env.example, .gitignore, pom.xml, README.md
```
`pom.xml` maps these folders to what Spring Boot expects (`frontend/pages` -> `templates/`, `frontend/css|js|images` -> `static/`, `backend/src/main/java` -> sources, `tests/backend-tests` -> tests). See `docs/architecture.md`.

## 4 & 5. Frontend and backend files - purpose
| Location | Purpose |
|---|---|
| `frontend/pages/home.html`, `login.html`, `register-*.html` | landing page, login, registration |
| `frontend/pages/dashboard/*.html` | admin/staff, donor and recipient dashboards with charts |
| `frontend/pages/donors/*.html` | donor list, detail, edit, own profile and history |
| `frontend/pages/requests/*.html` | blood request list, form, detail, donor suggestions |
| `frontend/pages/inventory/*.html` | blood availability: stock matrix, batches, movements |
| `frontend/pages/camps`, `appointments`, `notifications`, `reports`, `admin` | camps, QR appointments, notifications, reports, users and audit log |
| `frontend/components/layout.html` | navbar, footer, scripts shared by all pages |
| `backend/.../config/SecurityConfig.java` | login, roles, URL permissions, CSRF, BCrypt |
| `backend/.../controller/*` | receive requests, validate forms, choose the page; `ApiController` = JSON API |
| `backend/.../service/*` | business rules (workflow, stock issue with locking, matching, reports) |
| `backend/.../repository/*` | database queries |
| `backend/.../model/*`, `dto/*` | tables (entities) and validated forms |
| `backend/.../config/DemoDataLoader.java` | creates demo accounts and sample records on first start |

REST API: `GET /api/public/stats` (open), `GET /api/inventory/stock` (staff/admin).

## 6. Database setup
MySQL 8. Tables are **created automatically** on first start; `database/schema.sql` is the equivalent manual script. Full guide: `database/database-config/README.md`.

## 7. Open the project in VS Code (Windows)
1. Install **JDK 21** (https://adoptium.net - tick "Set JAVA_HOME"), **Maven** (https://maven.apache.org - unzip to `C:\maven`, add `C:\maven\bin` to Path), **MySQL 8** (remember the root password; service MySQL80 must be running), **VS Code** with "Extension Pack for Java" and "Spring Boot Extension Pack".
2. Right-click the ZIP -> Extract All (e.g. to `C:\Projects\`).
3. VS Code -> File -> Open Folder -> choose `LifeLink-Blood-Donation-Management` (the folder containing `pom.xml`) -> "Yes, I trust the authors".
4. Terminal -> New Terminal. Check `java -version` (21) and `mvn -version`.

## 8. Run the database
Start the MySQL service (Windows Services -> MySQL80 -> Start). Then, in the VS Code terminal: `copy .env.example .env`, open `.env` and set `DB_PASSWORD=your_mysql_password`. Nothing else is needed - the database `lifelink` is created automatically.

## 9. Start the backend
Easiest: double-click `windows\run-project.bat`. Or in the terminal:
```
mvn clean test            (first: runs the automated tests, MySQL not needed)
mvn spring-boot:run       (starts the application; wait for "Started LifeLinkApplication")
```
Stop with `Ctrl + C`.

## 10. Open in the browser
http://localhost:8080 - Demo logins (password `Demo@12345`, set by `DEMO_PASSWORD`): `admin@lifelink.demo`, `staff@lifelink.demo`, `donor@lifelink.demo`, `recipient@lifelink.demo`.

## 11. Test the main features (5 minutes)
1. Recipient: **New Request** -> submit an *Emergency* request (appears red, first in the list).
2. Staff: **Blood Requests** -> open it -> Under Review -> Approved; try **Suggest donors**; then try **Fulfilled** (works only if matching stock exists - otherwise you see the "Not enough usable stock" error).
3. Staff: **Inventory** -> Receive stock 5 units; Issue more than available (error); check Movements.
4. Donor: **Camps** -> Book appointment -> **My Appointments** shows the QR code.
5. Admin: **Users** (create staff), **Audit Log**, **Reports** -> CSV. Click **ಕನ್ನಡ** to change language.
6. Frontend files check: `powershell -ExecutionPolicy Bypass -File tests\frontend-tests\check-frontend-files.ps1`; full checklist in `tests/frontend-tests/`.

## Files to open during the demonstration
| Show | File |
|---|---|
| Architecture and layering | `docs/architecture.md`, `docs/UML-diagrams/architecture.png` |
| Security (roles, BCrypt, CSRF) | `backend/src/main/java/com/lifelink/config/SecurityConfig.java` |
| Request workflow and stock rule | `service/RequestService.java`, `service/InventoryService.java`, `model/RequestStatus.java` |
| Donor matching (advisory) | `service/DonorService.java` (`matchFor`) |
| Frontend pages and theme | `frontend/pages/dashboard/staff.html`, `frontend/css/lifelink.css` |
| Database | `database/schema.sql`, `docs/UML-diagrams/er-diagram.png` |
| Tests | `tests/backend-tests/java/com/lifelink/service/InventoryServiceTests.java` |
| Report and viva | `reports/project-report.md`, `docs/viva-questions.md` |

## Troubleshooting
| Message | Fix |
|---|---|
| `mvn` not recognized | add Maven `bin` to Path and open a NEW terminal |
| `release version 21 not supported` | JDK older than 21 |
| `Access denied for user 'root'` | wrong `DB_PASSWORD` in `.env` |
| `Communications link failure` | MySQL service not running |
| Port 8080 busy | add `SERVER_PORT=8081` to `.env` |
| Demo login fails | database had users before first start: `DROP DATABASE lifelink;` then start again |
| No styling / charts | internet is needed (CDN libraries) |
| Red underlines on getters in VS Code | Lombok - wait for the Java extension, or just run `mvn clean test` |

## Configuration (`.env`)
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `DDL_AUTO`, `DEMO_DATA_ENABLED`, `DEMO_PASSWORD`, `LOW_STOCK_THRESHOLD`, `EXPIRY_ALERT_DAYS`, `DONATION_INTERVAL_DAYS` (reminder only - not a medical rule). Set `DEMO_DATA_ENABLED=false` for a clean database; change `DEMO_PASSWORD` before showing the app publicly.

## Important
The software does **not** decide medical eligibility, blood compatibility, cross-matching or release of blood; qualified blood bank professionals do. Default shelf-life values that pre-fill expiry dates are generic - enter the real expiry from the bag label.
