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

## Planned for Week 7 (business rules)
BR-01 no double booking with the 180-minute default (including two bookings at the same moment, NFR-06), BR-03 clean before reuse,
BR-04 waiting-list order when seating, BR-05 table lifecycle, check-in and no-show after 45 minutes (CR-01).
