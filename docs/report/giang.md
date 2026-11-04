# Final Project Report — Kieu Bao Giang's part

Draft by Kieu Bao Giang (team leader), 04/11/2026. English, as the course requires.

## About this file

This file holds the parts of the Final Project Report that Kieu Bao Giang writes:
- the front matter (cover and declaration);
- Chapter 1;
- the Table & Reservation sections of Chapter 3;
- Chapter 4;
- Giang's rows of Appendices B and C.

When Lam's and Khoa's parts are ready, Giang copies all parts into `Template_Final-Project-Report.docx` in the template's order. Then Giang:
- numbers the figures and tables;
- builds the table of contents and the lists of figures and tables;
- exports the DOCX and the PDF.

Text in `[brackets]` is still open. It waits for Lam's or Khoa's results, or for v1.0.

## Front matter (for the cover and declaration)

- **Course:** 72ITSE41704 Application Programming Project, class group 261_72ITSE41704_01.
- **Title:** Restaurant Table Booking and Kitchen Order System.
- **Presented by:**

  | Member | Student ID | Role |
  |---|---|---|
  | Kieu Bao Giang (team leader) | 2474802010095 | Table & Reservation module |
  | Tang Thoai Lam | 2474802010210 | Order & Kitchen module |
  | Nguyen Nam Khoa | 2474802010194 | Billing & Reporting module |

- **Advisor:** Dr. Nguyen Tri Hai.
- **Place and year:** Ho Chi Minh City – 2026. Submission date: [date].
- **Repository:** https://github.com/Zang-1/261_72ITSE41704_2474802010095_RestaurantBooking (tag `v1.0`).
- **File names:**
  - `2474802010095_KieuBaoGiang_ApplicationProgrammingProject.pdf`
  - `2474802010095_KieuBaoGiang_ApplicationProgrammingProject.docx`
- **Declaration:** use the template's text with the project title. All three members sign.

## Chapter 1: Introduction

### 1.1 Background and Rationale

**Reasons for selecting the topic.**
Small and medium-sized restaurants often run their front of house on a notebook, a chat app and word of mouth.
The team interviewed staff of a steak and pasta restaurant in Ho Chi Minh City on 18/10/2026.
In this report the restaurant is called "Ember & Oak" to keep it anonymous.
The interview showed what this costs in practice:
- Reservations arrive through social-media messages.
- No-shows are the most common reservation problem.
- Late guests are held for up to 45 minutes.
- The restaurant already uses an ordering app, but the app is not linked to reservations or billing.
- Out-of-stock dishes are passed on by word of mouth.
- Cash and transfers are reconciled by hand at the end of a shift.

**Importance and practical significance.**
Tse and Poon [12] found that no-shows, cancellations and walk-ins make it hard for a busy restaurant to decide how many reservations to accept.
A system that keeps reservations, walk-ins and table status in one place is the first step to managing this.
The same system can pass a seated table on to ordering and billing, so that staff no longer copy information between tools.

### 1.2 Objectives of the Project

**General objective.**
Build a web application with three staff roles that lets a small or medium-sized restaurant manage, in one consistent system:
- table reservations;
- walk-in guests;
- orders and kitchen preparation;
- billing.

**Specific objectives, with status on 03/11/2026:**

| # | Objective (Proposal §1.2) | Status |
|---|---|---|
| 1 | 15 use cases end-to-end for three roles | UC-01 to UC-06 (Table & Reservation) done. [Lam: UC-07 to UC-10] [Khoa: UC-11 to UC-15] |
| 2 | 13 business rules in the service layer, each with at least one automated test | BR-01 to BR-05 done, each tested. [BR-06 to BR-08 Lam] [BR-09 to BR-13 Khoa] |
| 3 | Zero overlapping reservations, also for two bookings at the same moment | Done: row lock and automated concurrent test (TC-RES-27) |
| 4 | At least 30 documented test cases with expected and actual results | 31 for Table & Reservation (TC-RES-01 to TC-RES-31). [Order & Kitchen: TC-MENU, TC-ORD, TC-KIT — Lam to count] [Billing: …] |
| 5 | Measurable NFRs, e.g. search under 2 s | NFR-01 met: with 50 tables and 500 reservations in a week the slowest search took 16 ms (TC-RES-30, H2 test database). [Lam: NFR-02, NFR-03] [Khoa: NFR-04] |

### 1.3 Scope of the Study

**Research scope.**
The reservation, seating, ordering and billing process of one restaurant.
The process was learned from the interview and from published work on restaurant reservations.

**System scope: in.**
- Login with three roles: Waiter/Host, Kitchen Staff, Manager.
- Tables and sections.
- Reservations: search, create, modify, cancel, check-in, automatic no-show.
- Walk-in waiting list.
- Table lifecycle and floor overview.
- Menu, orders, kitchen display, out-of-stock. [Lam: confirm]
- Bills, splitting, payments, shifts and the shift report. [Khoa: confirm]

**System scope: out.**
- Online booking or ordering by guests.
- Deposits and pre-ordering with a reservation. The restaurant asked for these (interview F2); they are future work.
- SMS and email.
- Real payment gateways.
- Staff account administration in the UI.
- Stock of ingredients.
- More than one branch.

**Organisational and time scope.**
- Three students.
- Ten weeks, from 14/9 to 22/11/2026.
- Each member owns one module:
  - Kieu Bao Giang: Table & Reservation, plus the shared skeleton, login and team lead.
  - Tang Thoai Lam: Order & Kitchen.
  - Nguyen Nam Khoa: Billing & Reporting.

### 1.4 Research Methodologies

- **Data collection: document review.** Course documents [1], [2], requirements guidance [4], [7] and the no-show study [12].
- **Observation and practical investigation.**
  - Semi-structured interview at the restaurant on 18/10/2026, with all three members present.
  - Two staff members answered: a cashier and a waiter.
  - The notes are kept in `docs/interview/restaurant-interview-notes.md`.
- **Data analysis.**
  - Each interview finding (F1–F12) was mapped to the requirement it affects.
  - Five requirement changes followed (CR-01 to CR-05). The main ones are:
    - dining duration 90 → 180 minutes;
    - no-show limit 15 → 45 minutes.
- **Comparative and statistical methods.** The timings of BR-01 and BR-05 were checked against the times the staff gave:
  - 3-hour table use;
  - 15-minute reset;
  - guests 30 minutes early or 45 minutes late.
- **Interviews and surveys.** See above. No survey was made.

### 1.5 Structure of the Report

- **Chapter 1** introduces the problem, the objectives and the scope.
- **Chapter 2** presents:
  - the technologies used: layered architecture, Spring Boot, JPA, React;
  - the restaurant's current process;
  - the full requirement analysis, as changed after the interview.
- **Chapter 3** describes the analysis, design, implementation and testing of each module.
- **Chapter 4** compares the result with the Proposal and lists what each member learned.
- **The appendices** hold:
  - the full test-case log;
  - the AI-use declaration (Appendix B);
  - the contribution table (Appendix C).

## Chapter 3 — Table & Reservation module sections

### 3.1 Problem Description and System Objectives (module part)

**Problem statement.**
The host must answer three questions at the door:
- Which table can this party have now?
- Which tables must be kept for guests who booked?
- Who waits next?

Today the answers come from a chat history and memory. Tables are promised twice or given away shortly before a booking.

**Objectives of the module.**
- Never book one table twice for overlapping times (BR-01, NFR-06).
- Only seat a party at a table that is big enough (BR-02).
- Never seat guests at a table that is not clean (BR-03).
- Seat walk-ins fairly (BR-04).
- Make every table follow one lifecycle that the other modules also use (BR-05).

### 3.2 System Analysis and Design (module part)

#### 3.2.1 Functional analysis
- Use cases UC-01 to UC-06; see the use-case diagram, Figure 3.x (`docs/diagrams/giang/01-use-case.png`).
- Functional decomposition: `docs/diagrams/giang/09-functional-decomposition.png`.

#### 3.2.2 Data flow
- DFD level 0: `10-dfd-0-context.png`.
- DFD level 1, reservation process: `10-dfd-1a-reservation.png`.

#### 3.2.3 Database design
ERD: `docs/diagrams/giang/README.md` §2 (Mermaid, matches Flyway `V1__table_reservation_module.sql`).

| Table | Purpose | Key constraints |
|---|---|---|
| `users` | Staff accounts | `username` unique; `password_hash` is BCrypt; `role` is one of three values |
| `table_section` | Areas of the floor (Hearth Hall, Duo Lounge, Cellar Room, Sky Terrace) | `name` unique |
| `dining_table` | Tables | `number` unique; `capacity` CHECK 1–20; FK to section; `status`; `active` |
| `reservation` | Bookings | `code` unique; FK to table; index (`table_id`, `start_time`) for the overlap check |
| `waitlist_entry` | Walk-in queue | `seated_table_id` stays NULL while the party waits |
| `reservation_settings` | One row: dining minutes (180) and turn minutes (15) | Edited by the Manager (30–300 and 0–60) |

#### 3.2.4 Class design and state machines
Images for the report are in `docs/diagrams/giang/updated/`; the Mermaid sources are in `docs/diagrams/giang/README.md`.
- Class diagram: `w8-class-table-reservation.png` (README §1).
- ERD: `w8-erd-table-reservation.png` (README §2).
- Table state machine: `w8-state-table.png` (README §3).
- Reservation state machine: `w8-state-reservation.png` (README §4).
- Sequences: check-in, `w8-sequence-check-in.png` (README §5); two simultaneous bookings, `w8-sequence-concurrent-booking.png` (README §6).

Design decision: the rules live in the domain classes, not in the controllers or in React (Guidelines §4.1 item 4).
- `DiningTable.changeStatus` is the only way to change a table's status. It refuses every move the lifecycle does not allow, so Order and Billing cannot bypass BR-05.
- `Reservation.overlap` holds the BR-01 formula. The service and the walk-in check both call it.

```java
public void changeStatus(TableStatus target) {
    if (target == status) return;
    boolean allowed = switch (status) {
        case AVAILABLE -> target == TableStatus.RESERVED || target == TableStatus.OCCUPIED;
        case RESERVED -> target == TableStatus.OCCUPIED || target == TableStatus.AVAILABLE;
        case OCCUPIED -> target == TableStatus.NEEDS_CLEANING;
        case NEEDS_CLEANING -> target == TableStatus.AVAILABLE;
    };
    if (!allowed) throw new BusinessRuleException("Table " + number + " cannot go from " + label(status) + " to " + label(target));
    status = target;
}
```
*Listing 3.x. BR-05 in `DiningTable`.*
The `switch` over the enum makes the compiler check that every status is handled.
The message tells staff exactly which step was refused.

```java
public static boolean overlap(LocalDateTime startA, LocalDateTime startB, ReservationSettings s) {
    long block = s.getDiningMinutes() + s.getTurnMinutes();
    return startA.isBefore(startB.plusMinutes(block)) && startB.isBefore(startA.plusMinutes(block));
}
```
*Listing 3.x. BR-01 in `Reservation`.*
With the defaults, the block is 180 + 15 = 195 minutes.
A booking at 19:00 blocks the table until 22:15. A booking at 22:15 is accepted, and 22:14 is refused (TC-RES-17).

#### 3.2.5 Interface design
- Navigation flow: `11-screen-navigation.png`.
- Screens:
  - Login.
  - Floor overview: Figure 3.x, `docs/screenshots/giang/01-floor-overview.jpg`.
  - Reservations: Figures 3.x, `02-reservation-check-in.jpg` and `03-check-in-too-early.jpg`.
  - Waiting list: Figure 3.x, `04-waiting-list.jpg`.
  - Tables (Manager).
- The UI design preview for all three modules is `docs/design/ui-demo.html`.

### 3.3 System Development and Implementation (module part)

#### 3.3.1 Technologies and tools
| Layer | Technology |
|---|---|
| Language | Java 17 (backend), JavaScript (frontend) |
| Backend | Spring Boot 3.5: Web, Data JPA, Security, Validation, Scheduling |
| Database | MySQL 8, with schema changes in Flyway migrations; H2 for tests and the demo profile |
| Frontend | React 18, React Router 6, Vite 6 |
| Tests | JUnit 5, Spring MockMvc, Spring Security Test |
| Tools | IntelliJ IDEA / VS Code, Git and GitHub (branches, pull requests, tags), Maven, npm |

[Merge with Lam's and Khoa's rows; list each library's licence (Guidelines §10).]

#### 3.3.2 Implemented functions

| Function (FR / UC) | Input | Output | Endpoint | Screenshot |
|---|---|---|---|---|
| Log in / log out (FR-01, UC-01) | username, password | Session cookie and role; the menu shows only the screens of that role | `POST /api/auth/login`, `/logout`, `GET /api/auth/me` | — |
| Manage tables and sections (FR-02, UC-02) | number, capacity 1–20, section | Table saved. Deactivation is refused while future bookings exist | `POST/PUT /api/tables`, `/deactivate`, `/activate`, `POST /api/sections` | Tables page |
| Search available tables (FR-03) | date, start time, party size | Active tables that fit and have no booking within ±195 min | `GET /api/reservations/available-tables` | Booking form, step 1 |
| Create reservation (FR-04, UC-03) | table, guest name, phone, party size, date, time, note | Confirmed reservation with a code such as `RVUKT2TB`. 409 on overlap | `POST /api/reservations` | Booking form, step 3 |
| Modify / cancel (FR-05, UC-04) | reservation id, new details | Updated or Cancelled. The old table is released | `PUT /api/reservations/{id}`, `POST …/cancel` | Reservations page |
| Check in (FR-05, UC-04) | reservation id | Reservation Checked in, table Occupied. Refused earlier than 30 min before, or 45 min or more after the start | `POST /api/reservations/{id}/check-in` | Figures 3.x (02, 03) |
| No-show and Reserved (FR-06, BR-05) | — (runs every 60 s) | No-show 45 min after the start, and the table is released. The table turns Reserved 30 min before a booking | `TableStatusScheduler` → `ReservationService.refreshStatuses` | Floor overview |
| Waiting list (FR-07, UC-05) | party name, phone, size | Queue with positions in arrival order | `GET/POST/DELETE /api/waitlist` | Figure 3.x (04) |
| Seat next waiting party (FR-08, BR-04) | table id | Earliest party that fits is seated; the table is Occupied | `POST /api/waitlist/seat-next?tableId=` | Figure 3.x (01) |
| Seat a walk-in (BR-01, BR-02) | table id, party size | Table Occupied. Refused if a booking starts within 195 min | `POST /api/tables/{id}/seat-walk-in` | Floor overview |
| Guests left / mark clean (FR-09, UC-06, BR-03) | table id | Needs cleaning (refused while an order is open), then Available | `POST /api/tables/{id}/needs-cleaning`, `/clean` | Floor overview |
| Floor overview (FR-10) | — | All tables by section, colour-coded by status, with the actions allowed in each status | `GET /api/tables` | Figure 3.x (01) |
| Reservation settings (UC-02 step 5) | dining 30–300, turn 0–60 min | New timings are used by BR-01 at once | `GET/PUT /api/settings/reservation` | — |

**Implementation notes**

- **Two bookings at the same moment (NFR-06).**
  - `create`, `modify` and `checkIn` first lock the table row: `DiningTableRepository.findByIdForUpdate`, a JPA `PESSIMISTIC_WRITE` lock, which is `SELECT … FOR UPDATE`.
  - Only then do they check for overlaps.
  - A second request for the same table waits until the first commits. It then sees the first booking and gets 409.
  - If the lock waits too long, the user gets "Someone else is changing this table right now. Please try again." and never a 500.
- **Time-driven rules.**
  - `TableStatusScheduler` runs every 60 seconds. It reads the time from an injected `Clock`, so tests can fix the time.
  - It is switched off in the test profile (`app.scheduling.enabled=false`), and the test calls `refreshStatuses(now)` directly.
- **Link to the Order module.** `markNeedsCleaning` refuses while the table has an order in status OPEN or LOCKED (Lam's `CustomerOrder`).
- **Errors.**
  - Every failure becomes `{ message, fieldErrors }`.
  - React shows the message above the page, or next to the field (`docs/security-check.md` §1).

#### 3.3.3 Unimplemented or incomplete functions
| Item | State | Reason |
|---|---|---|
| UC-06 step 3: "a waiting party that fits is highlighted" | Replaced | Each Available tile has a "Seat next waiting" button instead. It is simpler and applies the same rule BR-04 on the server |
| Phone reminder to late guests after 30 minutes (interview F5) | Not built | SMS and calls are out of scope. The booking stays Confirmed between 30 and 45 minutes, so staff can call |
| Floor overview updates by itself | Partly | The page reloads after each action, but not when another user changes a table. Polling or WebSocket would be the next step |
| Reserved status right after "Mark clean" | Up to 1 minute late | Set by the next scheduler run (shortcut noted in `TableService.markClean`) |
| Deposits, pre-ordering, online booking | Out of scope | Proposal §1.3; the restaurant asked for them (F2). Future work |
| Table with an unpaid bill cannot be cleaned (UC-06 alt. 1a) | [Depends on Khoa] | Order status is checked now. Bill status is added when Billing is merged |

#### 3.3.4 Other deliverables
- REST API: the endpoints in Table 3.x.
- Flyway migration `V1`.
- README run guide, with the H2 demo profile that needs no MySQL.
- Design preview: `docs/design/ui-demo.html`.
- Interview notes and requirement changes.
- Updated diagrams: `docs/diagrams/giang/README.md` (Mermaid) and `docs/diagrams/giang/updated/` (PNG).
- Security check: `docs/security-check.md`.

### 3.4 Testing and Evaluation (module part)

#### 3.4.1 Testing process
- **Methods.**
  - Automated unit tests of the domain rules (JUnit 5, no Spring).
  - API tests with MockMvc on an in-memory H2 database with the real seed data.
  - One concurrent test with two threads and real transactions.
  - End-to-end tests with a real login:
    - a whole visit on one table (TC-RES-28);
    - a visit across modules: reservation, order, kitchen, then the table released (TC-RES-31).
  - One performance test for NFR-01 (TC-RES-30).
  - Manual checks in the browser for the rows marked "UI".
- **Test cases.**
  - TC-RES-01 to TC-RES-31, in `tests/test-cases-table-reservation.md`, Appendix [x].
  - Columns: ID, purpose, precondition, input, expected, actual, pass/fail, automated test.
- **Results** on 04/11/2026:
  - `mvn test` runs 115 automated tests: 54 for this module and 61 for Order & Kitchen. 0 failures.
  - All 31 documented cases pass.
  - [Update the counts when Billing is merged.]

#### 3.4.2 System evaluation
**Strengths**
- Every rule is enforced on the server and tested at the boundary, for example 22:14 refused and 22:15 accepted.
- Double booking is impossible even under concurrency (TC-RES-27).
- The messages tell staff what to do, not only what went wrong.
- NFR-01 is met with a wide margin: with 50 tables and 500 reservations in a week, the slowest of five searches took 16 ms, against a limit of 2,000 ms (TC-RES-30).

**Limitations**
- No rate limit on login.
- Demo passwords are in the README.
- No HTTPS of its own.
- The floor overview does not refresh by itself.
- NFR-01 was measured on the in-memory H2 test database, not on MySQL. MySQL adds network and disk time, but the query uses the index on (`table_id`, `start_time`).
- The cross-module test closes the order directly, because the Billing module is not merged yet.

#### 3.4.3 Comparison between theory and practice
- **Theory → practice.**
  - The state-machine diagram from the design became one `switch` in `DiningTable`.
  - The "lost update" problem from database courses became a real test with two threads.
- **Differences.**
  - The Proposal assumed a 90-minute meal and a 15-minute no-show limit. The real restaurant uses 3 hours and 45 minutes.
  - A requirement that looked final changed after one interview.
  - The dining and turn timings were Manager settings, so CR-02 needed no code change.
  - The no-show limit was a constant (`Reservation.NO_SHOW_AFTER_MINUTES`), so CR-01 needed a one-line change and a new test.

## Chapter 4: Conclusion

### 4.1 Summary

**Summary of completed work, compared with the Proposal**

| Proposal item | Completed | Not completed / changed |
|---|---|---|
| Login and three roles (FR-01) | Yes: session login, BCrypt, role checks on the server | — |
| Table & Reservation, FR-02 to FR-10, UC-01 to UC-06, BR-01 to BR-05 | All FRs, all five rules, each with automated tests | UC-06 "highlight a fitting party" is replaced by a "Seat next waiting" button (see 3.3.3) |
| Order & Kitchen, FR-11 to FR-20, BR-06 to BR-08 | [Lam] | [Lam] |
| Billing & Reporting, FR-21 to FR-29, BR-09 to BR-13 | [Khoa] | [Khoa] |
| 30 documented test cases | 31 for Table & Reservation (TC-RES-01 to TC-RES-31), [+ Order & Kitchen + Billing] | — |
| Release tags v0.1, v0.2, v1.0 | [dates] | — |

**Achievements and outcomes**
- A working web application: Spring Boot, React and MySQL, with a demo profile that needs no MySQL.
- The interview changed two of our rules (CR-01, CR-02) and three of Khoa's (CR-03 to CR-05). The code follows the restaurant's real timings.
- Double booking is prevented even when two staff book at the same second. This is proven by an automated test.
- [Lam: kitchen display, course order …] [Khoa: split bill, shift report …]

**Limitations and future work**
- **Security on the internet:** rate-limit the login, use HTTPS, and require new passwords instead of the README demo passwords.
- **Live updates:** refresh the floor overview and the kitchen display by themselves, with WebSocket or polling.
- **Ideas from the interview:** online booking with a deposit to reduce no-shows, and pre-ordering dishes with the booking (F2).
- **Reminders:** SMS or Zalo reminders to late guests at 30 minutes (F5).
- [Order / Billing future work]

### 4.2 Knowledge and Skills Gained

| Member | Technical skills | Professional skills | Practical experience |
|---|---|---|---|
| Kieu Bao Giang | Layered Spring Boot design; JPA mapping and Flyway migrations; Spring Security sessions and roles; pessimistic locking for concurrency; JUnit and MockMvc tests, including a multi-threaded test; React pages calling a REST API | Leading a team of three: splitting modules, reviewing pull requests, keeping `main` buildable; turning interview notes into requirement changes | Interviewing restaurant staff and learning that real timings (3-hour tables, 45-minute grace) differ from assumptions |

## Appendices B and C (Giang's rows)

### Appendix B — Declaration of AI-tool use

| Tool | What it was used for | Which part of the work | How the output was verified |
|---|---|---|---|
| Claude (Anthropic), via Claude Code | Generating the backend skeleton, the Table & Reservation code (domain, services, controllers, Flyway V1) and the React screens from the Proposal's requirements and Giang's Week 4 diagrams | `src/backend` and `src/frontend`, Table & Reservation part (Weeks 5–7) | Giang read every class of the module and ran the automated tests (115 passing on 04/11/2026, 54 of them for this module). Giang tested each screen by hand with the H2 demo profile (rows marked "UI" in the test-case table) and recalculated the BR-01 boundary by hand (19:00 + 195 min = 22:15). The commits are from Giang's own account, through pull requests merged by another member |
| Claude (Anthropic), via Claude Code | Writing the automated tests and the documented test cases TC-RES-01 to TC-RES-31 | `src/backend/src/test`, `tests/test-cases-table-reservation.md` | Giang compared each expected result with the requirement and the business rule it tests. Giang ran the tests and checked the "actual" column against the test output, and checked the UI rows in the browser |
| Claude (Anthropic), via Claude Code | Translating the interview notes and anonymising the restaurant and area names | `docs/interview`, `docs/requirements/requirement-changes.md`, seed data | The three members who attended the interview checked the English notes against what the staff said. Giang chose to replace the real names with invented ones |
| Claude (Anthropic), via Claude Code | Drafting the Week 7 self-check, the Week 8 security check, the updated Mermaid diagrams and this report's Chapter 1, Chapter 3 (Table & Reservation part), Chapter 4 and Giang's appendix rows | `docs/self-check-week7.md`, `docs/security-check.md`, `docs/diagrams/giang/README.md`, `docs/report/giang.md` | Giang checked every statement against the code and the test results; diagrams were compared with the classes and the Flyway script |
| Claude (Anthropic), via Claude Code; Figma AI image generation | Building the clickable UI design preview (HTML) and its illustrative images | `docs/design/ui-demo.html` (design only, not part of the running application) | The team reviewed the screens against the use cases and used the preview to agree the layout before coding. The images are decorative and generated, not photographs of the real restaurant |

We declare that we understand every part of the submitted work and can explain it on request.

**Before signing, Giang checks being able to explain these parts of the code in an online walkthrough:**
- The BR-01 formula and why the boundary is 22:15.
- Why `findByIdForUpdate` stops double booking, and what the second request sees.
- The `switch` in `DiningTable.changeStatus`.
- How `TableStatusScheduler` marks a no-show, and why tests call `refreshStatuses(now)` directly.
- The difference between the 400, 403 and 409 errors in `GlobalExceptionHandler`.

### Appendix C — Individual contribution table

Fill in the commit counts at v1.0 with `git shortlog -sn --all` (merge commits excluded). Percentages must sum to 100%.

| Member | Main responsibilities | Main artefacts produced | Commits | Share of work (%) | Signature |
|---|---|---|---|---|---|
| Kieu Bao Giang (leader) | Team lead; repository and workflow; shared skeleton, login and roles; Table & Reservation module; restaurant interview write-up; report front matter, Chapters 1 and 4, assembly of the final report | Proposal; `docs/diagrams/giang/`; Flyway V1; Table & Reservation backend and frontend; 54 automated tests and TC-RES-01 to TC-RES-31; self-check, security check | [ ] | [ ] | |

