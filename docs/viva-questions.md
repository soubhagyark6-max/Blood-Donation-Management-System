# Viva Questions with Answers

**1. What is LifeLink and what problem does it solve?**
A web application that manages blood donors, requests, inventory, camps and reports for a blood bank. It replaces manual registers with a secure, role-based system with live stock and request tracking.

**2. Why Spring Boot?**
It gives auto-configuration, an embedded server, easy integration with JPA, Security and Thymeleaf, and a clean layered structure; the project runs with a single `mvn spring-boot:run`.

**3. Explain the layered architecture.**
Controller (HTTP, validation, view selection) -> Service (business rules, transactions) -> Repository (database access with Spring Data JPA) -> MySQL. DTO classes carry validated form data; entities map tables.

**4. How are passwords protected?**
With BCrypt (`BCryptPasswordEncoder`): salted, slow, one-way hashes. Only the hash is stored; login compares using `matches()`.

**5. How is role-based access implemented?**
`SecurityConfig` maps URL patterns to roles (e.g. `/inventory/**` -> ADMIN or STAFF). Services repeat ownership checks (a recipient can only open their own request), and the UI hides links the role cannot use.

**6. What is CSRF and how is it handled?**
An attacker site makes the victim's browser send a state-changing request. Spring Security keeps CSRF protection on; Thymeleaf's `th:action` inserts a hidden token in every POST form, and requests without it get 403 (tested).

**7. How do you prevent SQL injection?**
All database access uses JPA repositories, JPQL and the Criteria API with bound parameters - no string-concatenated SQL. Search text also has `%` and `_` escaped.

**8. Describe the request workflow.**
Pending -> Under Review -> Approved -> Fulfilled; Rejected (remark required) from any open state; Cancelled by the requester while Pending. `RequestStatus.canMoveTo` enforces it and every change is stored in `request_status_history`.

**9. How do you prevent negative or inconsistent stock?**
Stock is stored in batches. `issue()` runs in a transaction, locks usable batches with `PESSIMISTIC_WRITE`, checks the total, then deducts earliest-expiry first. A DB check `units >= 0` and an `@Version` field give extra protection. If stock is insufficient the whole transaction is rolled back.

**10. What does @Transactional do?**
It wraps the method in a database transaction: either all changes commit or all roll back. Marking a request Fulfilled and issuing its stock happen in one transaction.

**11. Why are blood components tracked separately?**
Whole blood, red cells, platelets and plasma differ in use and shelf life and are not interchangeable. Every stock query filters on group and component.

**12. Does the system decide donor eligibility or blood compatibility?**
No. The matching helper only filters by group, city, availability, last-donation date and verification and shows its reasons with a disclaimer. Medical eligibility, testing, cross-matching and release are done by qualified professionals.

**13. What is the 90-day value in the project?**
A configurable *reminder interval* (`DONATION_INTERVAL_DAYS`) used to hide recently donated donors in the matching list and to show a reminder date. It is not a medical rule.

**14. How are low-stock and expiry alerts produced?**
`InventoryService.refreshAlerts()` runs hourly (`@Scheduled`) and after stock changes; it creates in-app notifications for staff, avoiding duplicates with a `dedupeKey`.

**15. How does the dashboard get its numbers?**
`DashboardService` queries repositories (counts, grouped queries) and passes lists to Chart.js through Thymeleaf JavaScript inlining. No live figure is hard-coded; demo rows are labelled DEMO.

**16. What are DTOs and why use them?**
Form classes with validation annotations (`@NotBlank`, `@Pattern`, ...). They keep entities safe from mass-assignment and give clear validation messages.

**17. Explain the ER relationships.**
User 1-0..1 Donor; Donor 1-* Donation and Appointment; User 1-* BloodRequest and Notification; BloodRequest 1-* StatusHistory; Camp 1-* Appointment; inventory transactions refer to batches (batch code) and optionally to requests.

**18. How is the Kannada option implemented?**
Spring `MessageSource` with `messages.properties` and `messages_kn.properties`; `LocaleChangeInterceptor` switches language with `?lang=kn` and the choice is kept in the session. Only navigation and main pages are translated.

**19. How is the QR code created?**
In the browser with the `qrcode.js` library from the appointment reference; the reference is unique per appointment.

**20. How did you test?**
JUnit 5 + Spring Boot Test with in-memory H2 for service rules; MockMvc with Spring Security Test for login, CSRF and role access; validation tests; manual UAT. (State honestly which results you obtained when you ran `mvn test`.)

**21. How are secrets handled?**
`application.properties` reads environment variables or a git-ignored `.env`; `.env.example` has placeholders only. Demo password is configurable and demo data can be disabled.

**22. What would you improve?**
E-mail/SMS notifications, password reset, full translation, maps, hospital integrations, Docker/CI.

**23. How do you protect privacy?**
Contact details only for staff/admin; donor CSV excludes contact fields; recipients see only their own requests; error pages hide stack traces; audit log of actions.

**24. Why Thymeleaf instead of a JavaScript framework?**
Server-side rendering keeps the project simple, secure (automatic HTML escaping and CSRF tokens) and beginner-friendly, while Chart.js and Bootstrap provide interactivity.

**25. What is the role of Hibernate `ddl-auto`?**
`update` creates/updates tables automatically on start. For production use `validate` with the SQL in `database/schema.sql`.

**26. How do the frontend and backend communicate?**
Mostly by server-side rendering: the browser sends HTTP requests (GET pages, POST forms with a CSRF token) and Spring MVC returns Thymeleaf-rendered HTML. A small JSON REST API (`/api/public/stats`, `/api/inventory/stock`) also exists, and the home page uses `fetch()` to refresh its live figures.

**27. Why are `frontend/` and `backend/` folders used if Spring Boot expects `src/main`?**
`pom.xml` maps them: `sourceDirectory` points to `backend/src/main/java`, and `<resources>` copies `frontend/pages` to `templates/`, `frontend/css|js|images` to `static/`, so the layout is clean without changing how Spring Boot runs.
