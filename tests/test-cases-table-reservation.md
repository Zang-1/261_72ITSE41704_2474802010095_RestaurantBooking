# Test cases — Table & Reservation module (Kieu Bao Giang)

Week 6 deliverable. Columns follow Course Guidelines §6.3. If Khoa's shared test-case template uses
different columns, these rows are copied into it unchanged.

Every row is also an automated test (column **Automated test**) in `src/backend/src/test`, run with
`mvn test` on an in-memory H2 database seeded with the standard accounts and 27 tables.
**Actual result** and **Pass/Fail** come from the run on 21/10/2026 (28 tests, 0 failures). Rows marked
"UI" were also checked by hand in the browser on the same day (H2 demo profile).

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-RES-01 | Log in with a valid account (UC-01, FR-01) | Seeded account `manager` | username `manager`, correct password | 200; role MANAGER; `/api/auth/me` works with the same session | As expected; UI opens the floor overview | Pass (UI) | AuthControllerTest.loginWithValidAccount |
| TC-RES-02 | Wrong password gives a generic message (UC-01 alt. 3a) | Seeded account `waiter` | username `waiter`, password `wrong-password` | 401 "Invalid username or password" | As expected | Pass (UI) | AuthControllerTest.loginWithWrongPassword |
| TC-RES-03 | Empty login field is validated (UC-01 alt. 2a, NFR-11) | — | username empty | 400; message "Username is required" next to the field | As expected | Pass (UI) | AuthControllerTest.loginWithEmptyUsername |
| TC-RES-04 | API call without login is refused (NFR-10) | Not logged in | GET /api/tables | 401 "Please log in" | As expected | Pass | AuthControllerTest.requestWithoutLogin |
| TC-RES-05 | Role check on the server (NFR-10) | Logged in as Waiter/Host | POST /api/tables | 403 "Access denied" | As expected | Pass | AuthControllerTest.waiterCannotCreateTable |
| TC-RES-06 | Duplicate table number (FR-02, UC-02 alt. 3a) | Table 1 exists; Manager | number 1, capacity 4 | 400; "Table number 1 already exists" on `number` | As expected | Pass | TableControllerTest.duplicateTableNumber |
| TC-RES-07 | Capacity range 1–20 (FR-02) | Manager | number 40, capacity 25 | 400; "Capacity must be between 1 and 20" on `capacity` | As expected | Pass | TableControllerTest.capacityOutOfRange |
| TC-RES-08 | Table with future reservations cannot be deactivated (FR-02, UC-02 alt. 4a) | Table 3 has a Confirmed reservation tomorrow 19:00; Manager | POST /api/tables/{id}/deactivate | 409; "Table 3 has 1 future reservation(s) and cannot be deactivated" | As expected; same message on the Tables screen for table 6 | Pass (UI) | TableControllerTest.deactivateTableWithFutureReservation |
| TC-RES-09 | Reservation date in the past (UC-03 alt. 3a) | Waiter/Host | date = yesterday | 400; "Date cannot be in the past" on `date` | As expected | Pass | ReservationControllerTest.reservationInThePast |
| TC-RES-10 | Party larger than the chosen table (BR-02) | Waiter/Host; table 1 seats 2 | partySize 5 on table 1 | 400; "Table 1 seats only 2 guests" on `partySize` | As expected | Pass | ReservationControllerTest.partyLargerThanTable |
| TC-RES-11 | Invalid phone number (NFR-11) | Waiter/Host | phone `12345` | 400; "Phone number must have 10 digits and start with 0" | As expected | Pass (UI) | ReservationControllerTest.invalidPhone |
| TC-RES-12 | Create a valid reservation (FR-04, UC-03) | Waiter/Host | table of 4, Tran Van Binh, 0912345678, 2 guests, tomorrow 19:00 | 201; status CONFIRMED; code like `R7KQ2MXA`; listed for that date | As expected; UI showed "Reservation RCARE23C confirmed … table 6" | Pass (UI) | ReservationControllerTest.createValidReservation |
| TC-RES-13 | Cancel a future reservation (FR-05, UC-04) | Confirmed reservation tomorrow | POST /cancel twice | 1st: CANCELLED; 2nd: 409 "Only a confirmed reservation can be changed" | As expected | Pass | ReservationControllerTest.cancelReservation |
| TC-RES-14 | Party larger than any table (UC-03 alt. 3a) | Largest active table seats 12 | search partySize 13 | 400; "No table seats more than 12 guests" | As expected | Pass | ReservationControllerTest.searchPartyLargerThanAnyTable |
| TC-RES-15 | Waiting list keeps arrival order (FR-07, UC-05 alt. 5a) | Waiter/Host; empty list | add Le Hoang Nam (4), then Pham Thu Ha (2); remove the first | positions 1, 2; after removal Pham Thu Ha is position 1 | As expected | Pass (UI) | WaitlistControllerTest.partiesKeepArrivalOrder |
| TC-RES-16 | Invalid walk-in (UC-05 alt. 2a) | Waiter/Host | name blank, party size 0 | 400; "Party name is required", "Party size must be at least 1" | As expected | Pass | WaitlistControllerTest.invalidWalkIn |

Additional automated tests without a numbered row: search returns only tables that fit, badly formatted
time gives a field message, Kitchen Staff cannot list reservations, logout ends the session, a Manager
can add and deactivate a free table, the default timings are 180 and 15 minutes (CR-02), and domain unit tests in `ReservationTest`.

## Week 7–9: business rules and end-to-end (added 03/11/2026)

Rows TC-RES-17 to TC-RES-29 were run on 03/11/2026: `mvn test`, 79 tests, 0 failures.
Rows marked "UI" were also checked by hand on the same day, in the browser, with the H2 demo profile.
Screenshots are in `docs/screenshots/giang/`.

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-RES-17 | No double booking inside the 195-minute block (BR-01, CR-02) | Table of 4 booked tomorrow 19:00 | 2nd booking 22:14, then 22:15, same table | 22:14: 409 "Table N is already booked at 19:00; choose another time or table"; 22:15: 201 | As expected | Pass | TableLifecycleTest.noDoubleBooking |
| TC-RES-18 | Search leaves out a booked table (FR-03, BR-01) | Table booked tomorrow 19:00 | search tomorrow 20:00, party 2 | That table is not in the result | As expected | Pass | TableLifecycleTest.searchSkipsBookedTable |
| TC-RES-19 | Booking soon holds the table; check-in occupies it (BR-05, UC-04) | — | booking now + 20 min; POST /check-in | Table RESERVED after booking; reservation CHECKED_IN; table OCCUPIED | As expected; UI showed "Nguyen Thi Lan checked in at table 3." | Pass (UI) | TableLifecycleTest.checkInOccupiesTable |
| TC-RES-20 | No seating at a table that needs cleaning (BR-03) | Booking now + 20 min; table set to Needs cleaning | POST /check-in | 409 "Table N must be cleaned before the guests sit down" | As expected | Pass | TableLifecycleTest.checkInRefusedWhileTableNeedsCleaning |
| TC-RES-21 | Walk-in refused before a booking (BR-01) | Table booked now + 60 min | seat walk-in, party 2 | 409 "Table N has a reservation at HH:mm; a walk-in would overlap it" | As expected | Pass | TableLifecycleTest.walkInRefusedBeforeABooking |
| TC-RES-22 | Seat next takes the earliest party that fits (BR-04, FR-08) | Waiting: Large Group (6), Vo Thanh Tam (2), Dang Kim Lien (3) | seat-next on a table of 4 | Vo Thanh Tam seated; table OCCUPIED; Large Group stays 1st | As expected; UI: "Pham Thu Ha (4) seated at table 4." | Pass (UI) | TableLifecycleTest.seatNextTakesEarliestPartyThatFits |
| TC-RES-23 | Open order blocks cleaning; after the bill the table can be cleaned (BR-05, link to Order module) | Walk-in seated; open order on the table | needs-cleaning; close order; needs-cleaning; clean | 409 "Table N still has an open order; close its bill first"; then NEEDS_CLEANING; then AVAILABLE | As expected | Pass | TableLifecycleTest.cleaningCycle |
| TC-RES-24 | Occupied cannot skip cleaning (BR-05) | Table Occupied | POST /clean | 409 "Table N cannot go from Occupied to Available" | As expected | Pass | TableLifecycleTest.occupiedCannotBeMarkedClean |
| TC-RES-25 | Kitchen Staff cannot check in or seat (NFR-10) | Logged in as Kitchen Staff | POST /check-in, POST /seat-walk-in | 403 for both | As expected | Pass | TableLifecycleTest.kitchenStaffIsForbidden |
| TC-RES-26 | No-show after 45 minutes frees the table; Reserved 30 minutes before (FR-06, CR-01, BR-05) | Booking A started 45 min ago (table Reserved); booking B starts in 30 min | scheduler run `refreshStatuses(now)` | A NO_SHOW and its table AVAILABLE; B still CONFIRMED and its table RESERVED | As expected | Pass | TableLifecycleTest.refreshMarksNoShowAndReservesUpcoming |
| TC-RES-27 | Two bookings at the same moment (NFR-06) | Two threads, same table and time, separate transactions | 2 × create at once | Exactly one saved; the other gets BusinessRuleException "Table N is already booked at 19:00; …" | As expected | Pass | ConcurrentBookingTest.onlyOneOfTwoSimultaneousBookingsWins |
| TC-RES-28 | End to end: one table through a whole visit, with a real login | Logged in as `waiter` | book now + 20 min → walk-in → check-in → waiting party added → guests left → seat-next → clean → seat-next | RESERVED; walk-in 409; OCCUPIED; position 1; NEEDS_CLEANING; seat-next 409 "must be cleaned"; AVAILABLE; Pham Thu Ha seated, list empty | As expected | Pass | ReservationFlowEndToEndTest.wholeVisitOnOneTable |
| TC-RES-29 | Logout ends access (NFR-10) | Logged in, then logged out | POST /tables/1/clean with the old session | 401 | As expected | Pass | ReservationFlowEndToEndTest.logoutEndsAccess |

Extra checks found by hand in the UI on 03/11/2026 (no defect):
- **Early check-in:** pressing "Check in" 4 hours before the booking shows "Check-in opens 30 minutes before the booking".
- **Walk-in on a held table:** a walk-in on a Reserved table is refused with "Table 3 is not available".
- **Booking a table that is too small:** party of 6 on table 12 shows "Table 12 seats only 2 guests".

Domain unit tests without a numbered row:
- `DiningTableTest` (6 tests): every allowed and refused lifecycle step.
- `ReservationTest`: overlap boundary at 194 and 195 minutes, check-in window from −30 to +44 minutes, no-show at 45 minutes.
