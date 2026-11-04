# Chapter 3 — Table & Reservation module sections — draft

Draft by Kieu Bao Giang, 03/11/2026.
These sections go into Chapter 3 of the Final Report, next to Lam's (Order & Kitchen) and Khoa's (Billing & Reporting) sections.
Figure and table numbers are provisional. Renumber them when the three parts are merged.

## 3.1 Problem Description and System Objectives (module part)

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

## 3.2 System Analysis and Design (module part)

### 3.2.1 Functional analysis
- Use cases UC-01 to UC-06; see the use-case diagram, Figure 3.x (`docs/diagrams/giang/01-use-case.png`).
- Functional decomposition: `docs/diagrams/giang/09-functional-decomposition.png`.

### 3.2.2 Data flow
- DFD level 0: `10-dfd-0-context.png`.
- DFD level 1, reservation process: `10-dfd-1a-reservation.png`.

### 3.2.3 Database design
ERD: `docs/diagrams/giang/README.md` §2 (Mermaid, matches Flyway `V1__table_reservation_module.sql`).

| Table | Purpose | Key constraints |
|---|---|---|
| `users` | Staff accounts | `username` unique; `password_hash` is BCrypt; `role` is one of three values |
| `table_section` | Areas of the floor (Hearth Hall, Duo Lounge, Cellar Room, Sky Terrace) | `name` unique |
| `dining_table` | Tables | `number` unique; `capacity` CHECK 1–20; FK to section; `status`; `active` |
| `reservation` | Bookings | `code` unique; FK to table; index (`table_id`, `start_time`) for the overlap check |
| `waitlist_entry` | Walk-in queue | `seated_table_id` stays NULL while the party waits |
| `reservation_settings` | One row: dining minutes (180) and turn minutes (15) | Edited by the Manager (30–300 and 0–60) |

### 3.2.4 Class design and state machines
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

### 3.2.5 Interface design
- Navigation flow: `11-screen-navigation.png`.
- Screens:
  - Login.
  - Floor overview: Figure 3.x, `docs/screenshots/giang/01-floor-overview.jpg`.
  - Reservations: Figures 3.x, `02-reservation-check-in.jpg` and `03-check-in-too-early.jpg`.
  - Waiting list: Figure 3.x, `04-waiting-list.jpg`.
  - Tables (Manager).
- The UI design preview for all three modules is `docs/design/ui-demo.html`.

## 3.3 System Development and Implementation (module part)

### 3.3.1 Technologies and tools
| Layer | Technology |
|---|---|
| Language | Java 17 (backend), JavaScript (frontend) |
| Backend | Spring Boot 3.5: Web, Data JPA, Security, Validation, Scheduling |
| Database | MySQL 8, with schema changes in Flyway migrations; H2 for tests and the demo profile |
| Frontend | React 18, React Router 6, Vite 6 |
| Tests | JUnit 5, Spring MockMvc, Spring Security Test |
| Tools | IntelliJ IDEA / VS Code, Git and GitHub (branches, pull requests, tags), Maven, npm |

[Merge with Lam's and Khoa's rows; list each library's licence (Guidelines §10).]

### 3.3.2 Implemented functions

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

### 3.3.3 Unimplemented or incomplete functions
| Item | State | Reason |
|---|---|---|
| UC-06 step 3: "a waiting party that fits is highlighted" | Replaced | Each Available tile has a "Seat next waiting" button instead. It is simpler and applies the same rule BR-04 on the server |
| Phone reminder to late guests after 30 minutes (interview F5) | Not built | SMS and calls are out of scope. The booking stays Confirmed between 30 and 45 minutes, so staff can call |
| Floor overview updates by itself | Partly | The page reloads after each action, but not when another user changes a table. Polling or WebSocket would be the next step |
| Reserved status right after "Mark clean" | Up to 1 minute late | Set by the next scheduler run (shortcut noted in `TableService.markClean`) |
| Deposits, pre-ordering, online booking | Out of scope | Proposal §1.3; the restaurant asked for them (F2). Future work |
| Table with an unpaid bill cannot be cleaned (UC-06 alt. 1a) | [Depends on Khoa] | Order status is checked now. Bill status is added when Billing is merged |

### 3.3.4 Other deliverables
- REST API: the endpoints in Table 3.x.
- Flyway migration `V1`.
- README run guide, with the H2 demo profile that needs no MySQL.
- Design preview: `docs/design/ui-demo.html`.
- Interview notes and requirement changes.
- Updated diagrams: `docs/diagrams/giang/README.md` (Mermaid) and `docs/diagrams/giang/updated/` (PNG).
- Security check: `docs/security-check.md`.

## 3.4 Testing and Evaluation (module part)

### 3.4.1 Testing process
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

### 3.4.2 System evaluation
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

### 3.4.3 Comparison between theory and practice
- **Theory → practice.**
  - The state-machine diagram from the design became one `switch` in `DiningTable`.
  - The "lost update" problem from database courses became a real test with two threads.
- **Differences.**
  - The Proposal assumed a 90-minute meal and a 15-minute no-show limit. The real restaurant uses 3 hours and 45 minutes.
  - A requirement that looked final changed after one interview.
  - The dining and turn timings were Manager settings, so CR-02 needed no code change.
  - The no-show limit was a constant (`Reservation.NO_SHOW_AFTER_MINUTES`), so CR-01 needed a one-line change and a new test.
