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
| 7 | 26/10–1/11 | BR-01 to BR-05 with unit tests, concurrent-booking test (NFR-06); Week 7 self-check | Not started | — |

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

## Left for Week 7 (as planned)
- BR-01 overlap check in search, create and modify, with a pessimistic lock and the concurrent test (NFR-06).
- BR-05 table lifecycle (`DiningTable.changeStatus`), check-in, no-show after 45 minutes (CR-01), Reserved from 30 minutes before.
- Seating from the waiting list (FR-08, BR-03, BR-04); `Reservation.endTime()` and `overlaps()` from the class diagram.

## Notes for the team
- Run the backend without MySQL: `mvn spring-boot:run -Dspring-boot.run.profiles=h2`.
- Order needs an Occupied table and Billing sets a table to Needs Cleaning: both go through
  `DiningTable.changeStatus` once BR-05 is in place (Week 7).
- AI use for this work (Claude Code: generating the skeleton, module code and tests from the
  requirements and diagrams) must be declared in Appendix B of the Final Report.
