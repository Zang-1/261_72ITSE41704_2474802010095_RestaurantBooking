# Week 7 Self-check against the Final Report rubric (Appendix D)

Date: 03/11/2026 · Draft by Kieu Bao Giang (team leader) · To be agreed by Lam and Khoa before Week 8.
Non-graded; kept in `/docs` as the Course Guidelines ask.

| Rubric criterion | Where we are now (1–10) | What we will do in Weeks 8–9 |
|---|---|---|
| Requirements analysis & specifications | 7 | Bring the FR/BR lists in line with the interview changes CR-01 to CR-05 and with what is built; mark every FR as done / partly done / not done for Section 3.3.3 (owner: each member for their module, Giang merges). |
| Collaborative development & version control | 6 | Every member commits their own work every week through a feature branch and pull request reviewed by another member; tag `v0.2` after the Week 7 PR is merged and `v1.0` at feature freeze (Week 9). Khoa's billing code must start on `main` in Week 8. |
| System design & user interface | 6 | Update the class diagram, ERD and state machines so they match the code (dining 180 min, new methods `changeStatus`, `checkIn`, `markNoShow`, `seat`); take screenshots with realistic data for every screen; keep one style across all modules. |
| Implementation & testing | 7 | Table & Reservation: BR-01 to BR-05 done with 77 automated tests passing (including the concurrent-booking test, NFR-06). Next: end-to-end test of the whole visit, test cases TC-RES-17 onward with actual results; Order and Billing modules add their own rule tests. |
| Report writing quality & documentation | 4 | Start the Final Report in Week 9 from the template: each member drafts the Chapter 3 sections for their module, Giang drafts Chapters 1 and 4 and the appendices (AI-use declaration, contribution table). |

## Evidence for the scores (Table & Reservation module)

- **Business rules implemented this week**
  - BR-01 no double booking (195-minute block, row lock).
  - BR-02 capacity.
  - BR-03 clean before reuse.
  - BR-04 waiting list in arrival order.
  - BR-05 table lifecycle.
  - FR-06 no-show after 45 minutes.
  - Code: `DiningTable.changeStatus`, `Reservation.overlap / checkIn / markNoShow`, `ReservationService`, `TableService`, `WaitlistService`, `TableStatusScheduler`.
- **Tests:** `DiningTableTest`, `ReservationTest`, `TableLifecycleTest` (TC-RES-17 to TC-RES-26) and `ConcurrentBookingTest` (TC-RES-27). 77 tests run by `mvn test`, all passing on 03/11/2026.
- **UI:**
  - Reservations page: "Check in" button.
  - Floor page: "Seat walk-in", "Seat next waiting", "Guests left" and "Mark clean" buttons.

## Risks we see now
- Order (Lam) and Billing (Khoa) must change table status only through `DiningTable.changeStatus`, or the lifecycle rules can be bypassed.
- Khoa has few commits so far. The CLO2 criterion is marked per member from the repository history.
