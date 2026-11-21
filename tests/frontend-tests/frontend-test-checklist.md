# Frontend test checklist (manual, in Chrome or Edge)

Start the app (`windows\run-project.bat`), open http://localhost:8080 and tick each line. Record date and result.

| # | Page / feature | Check | Result |
|---|---|---|---|
| F1 | Home | hero, three live figures, upcoming camps, footer; resize to phone width - navbar collapses | |
| F2 | Language | click "ಕನ್ನಡ" - navbar and login texts change; click "EN" to return | |
| F3 | Login | wrong password shows red message; correct login opens the dashboard | |
| F4 | Register donor | empty submit shows validation messages; duplicate e-mail shows error | |
| F5 | Register recipient | works and redirects to login with green message | |
| F6 | Admin dashboard | 8 cards, 5 charts show data, alerts for low stock / expiry | |
| F7 | Donors list (staff) | search, filter by group and city, verified / available switches | |
| F8 | Donor detail | record donation, verify donor | |
| F9 | Blood request list | emergency rows red and first; filters work | |
| F10 | Request detail | status history timeline; status form only for staff | |
| F11 | Suggest donors | disclaimer visible, reasons listed per donor | |
| F12 | Inventory (blood availability) | stock matrix colours, receive and issue modals, filters, movements page | |
| F13 | Camps / appointments | book camp, QR code visible, cancel | |
| F14 | Notifications | bell badge count, mark all read | |
| F15 | Reports | date filter, CSV download, Print button | |
| F16 | Admin Users / Audit | create staff, disable user, audit entries | |
| F17 | Access control | donor opening /inventory sees the Access denied page | |
| F18 | Keyboard / contrast | tab through login form; focus ring visible | |
