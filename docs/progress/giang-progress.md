# Progress — Kieu Bao Giang (Team leader, Table & Reservation module)

Plan: Proposal Table 2.10. Evidence: commits and pull requests in this repository.

| Week | Dates | Planned (Giang) | Status | Evidence |
|---|---|---|---|---|
| 1 | 14–20/9 | Register topic, create repository and README | Done | Repository, README, .gitignore (13/9) |
| 2 | 21–27/9 | Reservation and login requirements (FR, NFR, BR, use cases) | Done | `docs/requirements/reservation-module-draft.md` (18/9, revised 26/9) |
| 3 | 28/9–4/10 | Proposal Chapters 1 and 4, cover, review; interview | Proposal done, submitted | PR #1, PR #2 (26/9); final proposal PDF/DOCX (11/10). Interview: moved to Week 5 in the plan |
| 4 | 5–11/10 | Architecture, ERD, reservation diagrams | Done | `docs/diagrams/giang/` (PR #3, PR #5, 10/10); revised proposal (PR #8, 11/10) |
| 5 | 12–18/10 | Interview; skeleton, login, tables and reservations; tag v0.1 | Interview done on 18/10 (all three members). Code done late, on 21/10. Tag v0.1: after the PR is merged | `docs/interview/restaurant-interview-notes.md`; branch `feature/skeleton-login-table-reservation` |
| 6 | 19–25/10 | Input validation; five test cases for own module; cross review (Lam reviews Giang) | Validation and 16 test cases done (21/10). Review: waiting for Lam on the PR | `tests/test-cases-table-reservation.md` |
| 7 | 26/10–1/11 | BR-01 to BR-05 with unit tests, concurrent-booking test (NFR-06); Week 7 self-check | Done late, on 03/11 | Branch `feature/giang-week7-10`; `docs/self-check-week7.md`; TC-RES-17 to TC-RES-27 |
| 8 | 2–8/11 | Exception handling, security check, updated diagrams and screenshots | Done (03/11) | `docs/security-check.md`; `docs/diagrams/giang/README.md`; `docs/screenshots/giang/` |
| 9 | 9–15/11 | Feature freeze; end-to-end test of own module; test cases; tag v1.0 | Own part done early (03/11). Tag v1.0 after Lam and Khoa merge | `ReservationFlowEndToEndTest` (TC-RES-28, TC-RES-29, TC-RES-31 across modules), `ReservationPerformanceTest` (TC-RES-30, NFR-01) |
| 10 | 16–22/11 | Final Report: Chapter 1, Chapter 3 (own module), Chapter 4, appendices | Drafts written (03/11); waiting for Lam and Khoa sections | `docs/report/` |

## Week 5–6 work (21/10/2026)

**Backend** (`src/backend`)
- Spring Boot 3.5 project, layered packages: `controller`, `service`, `repository`, `domain`, `dto`,
  `exception`, `config`. Teammates add their own classes to the same packages.
- Flyway migration `V1__table_reservation_module.sql` creates `users`, `table_section`, `dining_table`,
  `reservation`, `waitlist_entry` and `reservation_settings`, matching the ERD in
  `docs/diagrams/giang/04-erd-1-table-reservation.png`. Lam and Khoa add `V2__…` and `V3__…`.
- Login with a session cookie, BCrypt passwords, three roles; every endpoint checks the role on the
  server with `@PreAuthorize` (FR-01, NFR-10).
- Tables and sections: list, create, edit, deactivate (refused while future reservations exist), activate (FR-02).
- Reservations: availability search by date, time and party size (capacity only for now), create, list
  by date, modify and cancel before the start time (FR-03 to FR-05).
- Waiting list: add, list with positions in arrival order, remove (FR-07).
- Reservation settings (dining duration 180 min, turn-time 15 min, from the interview) editable by the Manager.
- Seed data: 3 staff accounts, 4 sections and 27 tables following the interviewed restaurant (seats per table assumed).
- Input validation on every request; errors return `{ message, fieldErrors }` and never a stack trace (NFR-11).

**Frontend** (`src/frontend`)
- Login, role-based navigation, floor overview, reservations (3-step booking form), waiting list,
  table management for the Manager, and a placeholder kitchen page for Lam's module.

**Tests**
- 28 automated tests (MockMvc + JUnit), all passing; 16 of them are documented as TC-RES-01 to TC-RES-16.

## Restaurant interview (18/10/2026)
- Notes and findings F1–F12: `docs/interview/restaurant-interview-notes.md`.
- Changes applied to this module: dining duration 90 → 180 minutes (CR-02), no-show 15 → 45 minutes (CR-01),
  seed data with the real layout (F8). Changes for Billing (CR-03 to CR-05) are listed for Khoa in
  `docs/requirements/requirement-changes.md`.

## Week 7–10 work (03/11/2026, branch `feature/giang-week7-10`)

**Business rules (Week 7)**
- BR-01: overlap check (195-minute block) in search, create and modify, and for walk-ins.
  - The table row is locked first (`findByIdForUpdate`, SELECT … FOR UPDATE), for NFR-06.
- BR-02: capacity check for bookings, walk-ins and the waiting list.
- BR-03 and BR-05: `DiningTable.changeStatus` allows only the lifecycle moves. New endpoints:
  - check-in;
  - seat walk-in;
  - guests left (refused while an order is open);
  - mark clean.
- BR-04: "Seat next waiting" seats the earliest party that fits.
- FR-06 and BR-05: `TableStatusScheduler` runs every minute.
  - No-show 45 minutes after the start, and the table is released.
  - The table turns Reserved 30 minutes before a booking.
- Frontend:
  - "Check in" button on the Reservations page.
  - Table actions on the Floor page.
- Tests: 79 automated tests (52 for this module), all passing. Includes the concurrent-booking test and the end-to-end test.

**Week 8**
- Exception handling and security check: `docs/security-check.md`.
- Diagrams updated to match the code (Mermaid): `docs/diagrams/giang/README.md`.
- Screenshots with realistic data: `docs/screenshots/giang/`.

**Week 9**
- End-to-end test of a whole visit with a real login (TC-RES-28).
- Test-case table extended to TC-RES-31, with actual results (04/11).
- End-to-end test across modules, using Lam's order and kitchen API (TC-RES-31, 04/11). Billing is stubbed until Khoa merges.
- NFR-01 measured: slowest search 16 ms with 50 tables and 500 reservations (TC-RES-30, 04/11).
- Updated diagrams exported as PNG for the report: `docs/diagrams/giang/updated/` (04/11).

**Week 10 (drafts)**
- `docs/report/giang.md`: front matter, Chapter 1, Chapter 3 (Table & Reservation part), Chapter 4, Giang's rows of Appendices B and C (one file, 04/11).
- Lam and Khoa add their sections.

## Notes for the team
- Run the backend without MySQL: `mvn spring-boot:run -Dspring-boot.run.profiles=h2`.
- Order needs an Occupied table and Billing sets a table to Needs Cleaning.
  - Both must go through `DiningTable.changeStatus` (BR-05). Never set the status field directly.
- Closing a bill should call `TableService.markNeedsCleaning`, or `changeStatus(NEEDS_CLEANING)` on the locked table.
- AI use for this work (Claude Code: generating the skeleton, module code and tests from the
  requirements and diagrams) must be declared in Appendix B of the Final Report.
