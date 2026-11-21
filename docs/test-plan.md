# Test Plan and Test Cases

## 15. Test strategy
- **Unit/service tests** (JUnit 5 + Spring Boot Test, in-memory H2 in MySQL mode, each test rolled back).
- **Web tests** (MockMvc with Spring Security Test): authentication, CSRF, role access, page rendering.
- **Validation tests** (Jakarta Validation without Spring).
- **Manual UAT** with the demo accounts (checklist below).

## Automated test classes (`tests/backend-tests/java`)
| Class | Covers |
|---|---|
| `LifeLinkApplicationTests` | Spring context, entity mappings |
| `InventoryServiceTests` | receive, issue, FIFO by expiry, no negative stock, components separate, expired stock unusable and discard, low-stock and expiry lists, input validation |
| `RequestServiceTests` | create, workflow transitions, rejection remark, fulfil issues stock, insufficient stock keeps status, recipient cannot change status, ownership checks, edit/cancel only when Pending, recipient sees only own requests, emergency ordering, emergency notification |
| `DonorServiceTests` | registration + BCrypt, duplicate e-mail/phone, password mismatch, under-18, search by group/city/text, `%` treated literally, verification, matching rules |
| `FormValidationTests` | bean-validation rules for request, donor, stock forms |
| `ReportServiceTests` | CSV escaping and formula-injection protection |
| `ApiWebTests` | REST API: public stats open and JSON, stock API staff-only |
| `AuthWebTests` | public pages, Kannada switch, registration success/failure, CSRF, login success/failure/disabled, redirect to login |
| `RoleAccessWebTests` | 403 for wrong roles, status change only by staff, privacy of other users' requests, all dashboards/pages render, request and stock forms, CSV privacy, camp booking |

## Manual test cases
| # | Scenario | Steps | Expected |
|---|---|---|---|
| M1 | Donor registration | Register with valid data | Redirect to login with success message |
| M2 | Duplicate e-mail | Register again with same e-mail | Error "account already exists" |
| M3 | Weak password | Use `abc` | Field error on password |
| M4 | Login wrong password | Enter wrong password | "Invalid email or password" |
| M5 | Role protection | Log in as donor, open `/inventory` | Access denied page |
| M6 | Create request | Recipient submits request | Shown as Pending; staff get notification |
| M7 | Emergency | Submit with urgency Emergency | Highlighted red row, listed first |
| M8 | Status workflow | Staff: Pending -> Under Review -> Approved -> Fulfilled | History shows 4 entries; recipient notified |
| M9 | Reject without remark | Staff rejects with empty remark | Error message |
| M10 | Fulfil without stock | Approve and fulfil a request for an empty stock combination | Error "Not enough usable stock", status stays Approved |
| M11 | Receive stock | Add 10 units O+ red cells | Matrix increases by 10; movement logged |
| M12 | Issue too many | Issue more than available | Error, stock unchanged |
| M13 | Low stock | Reduce a group below threshold | Red cell in matrix; notification |
| M14 | Expiry | Receive batch expiring in 3 days | Shown under "Expiring soon" |
| M15 | Matching | Open "Suggest donors" | Shortlist with reasons and disclaimer |
| M16 | Camp booking | Donor books camp; books again | First OK with QR; second refused |
| M17 | Cancel appointment | Donor cancels | Status Cancelled |
| M18 | Reports | Choose date range, export CSV | Summary and file download |
| M19 | Language | Click Kannada | Navigation and login texts in Kannada |
| M20 | Responsive | Resize to phone width | Navbar collapses, tables scroll |

## Test execution status (honest disclosure)
> The automated tests and manual cases above were **written together with the code**. The environment in which the project was generated could not reach Maven Central, so `mvn test` and `mvn package` **could not be run there**. The sources were checked for syntax (`javac` parse), and cross-checked for consistent field/property names, message keys and template names, but **not compiled against Spring or executed**.
> **Before your demonstration, run `mvn clean test` on your own computer** (see the installation manual) and fix anything it reports. Record the result in the table below.

| Run | Date | Tests run | Failures | Notes |
|---|---|---|---|---|
| 1 | | | | |

## Frontend tests
See `tests/frontend-tests/frontend-test-checklist.md` (manual browser checklist) and `tests/frontend-tests/check-frontend-files.ps1` (checks that every page, CSS, JS and image file exists). Page rendering is also covered by the automated MockMvc tests.
