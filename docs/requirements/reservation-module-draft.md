# Reservation Module — Requirements Draft

**Module:** Table & Reservation (also owns: login and role-based access)
**Owner:** Kieu Bao Giang (2474802010095)
**Status:** Revised draft (Week 2) — merged into the Project Proposal in `docs/proposal/project-proposal-draft.md`

> IDs use the prefix `RES` / `AUTH` so the three module drafts can be merged without collisions.
> In the Proposal they are renumbered FR-01, FR-02 … (see the mapping in the Proposal, section 2.2).

---

## 1. Scope of this module

**In scope**
- Login, logout and role-based access for all three roles (shared function, owned by this module)
- Table catalogue: tables, seat capacity, section (e.g. Indoor, Terrace, VIP room)
- Advance reservations: create, modify, cancel, check in, no-show
- Walk-in waiting list
- Table status lifecycle: Available → Reserved → Occupied → Needs Cleaning → Available
- Floor overview of all tables and their current status

**Out of scope** (handled by other modules or excluded)
- Orders, menu and kitchen tickets → Order & Kitchen module (Tang Thoai Lam)
- Bills, payments, shifts and reports → Billing & Reporting module (Nguyen Nam Khoa)
- Staff account administration (accounts are seeded at installation)
- Online booking by customers, deposits, SMS/email reminders

## 2. Actors

| Actor | Description |
|---|---|
| Waiter/Host | Front-of-house staff who take reservations, seat guests and manage the waiting list |
| Manager | Configures tables and sections, can override table status |
| Kitchen Staff | Uses only the login function of this module |

## 3. Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-AUTH-01 | The system shall require every user to log in with a username and password, and shall show only the screens and actions permitted for that user's role (Waiter/Host, Kitchen Staff, Manager). | Must |
| FR-RES-01 | The system shall allow a Manager to create, edit and deactivate a table, with a unique table number, a seat capacity between 1 and 20, and a section. A table that has future reservations cannot be deactivated. | Must |
| FR-RES-02 | The system shall allow a Waiter/Host to search for available tables by date, start time and party size, returning only tables whose capacity is greater than or equal to the party size and that have no overlapping reservation. | Must |
| FR-RES-03 | The system shall allow a Waiter/Host to create a reservation for a selected table with guest name, phone number, party size, date, start time and an optional note. The reservation shall be rejected if it overlaps another reservation on the same table (BR-RES-01). | Must |
| FR-RES-04 | The system shall allow a Waiter/Host to modify or cancel a reservation before its start time, and to check in a reservation when the guests arrive. Cancelling releases the time slot immediately; checking in sets the table to Occupied. | Must |
| FR-RES-05 | The system shall automatically mark a reservation as No-show when the guests have not checked in 15 minutes after the start time, and release the table. | Should |
| FR-RES-06 | The system shall allow a Waiter/Host to add a walk-in party (name, party size, phone number) to a waiting list, ordered by arrival time, and show each party's position. | Must |
| FR-RES-07 | The system shall allow a Waiter/Host to seat the first waiting party whose size fits a table that has just become Available, and remove that party from the waiting list. | Must |
| FR-RES-08 | The system shall enforce the table status lifecycle defined in BR-RES-05. A table in Needs Cleaning cannot be assigned to any reservation or walk-in until a Waiter/Host marks it clean. | Must |
| FR-RES-09 | The system shall display a floor overview showing every table with its number, section, capacity and current status, colour-coded by status. | Should |

## 4. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-RES-01 | Performance | An availability search (FR-RES-02) shall return results within 2 seconds for a restaurant with up to 50 tables and 500 reservations per week. |
| NFR-RES-02 | Usability | A Waiter/Host shall be able to create a reservation from the availability screen in no more than 3 steps (search → select table → confirm details). |
| NFR-RES-03 | Data integrity | Two users confirming a reservation for the same table and overlapping time at the same moment shall result in exactly one reservation being saved; the other receives a clear conflict message. |
| NFR-RES-04 | Reliability | All tables, reservations and waiting-list entries shall be stored in the MySQL database and remain available after the application is restarted. |
| NFR-RES-05 | Security | Passwords shall be stored only as BCrypt hashes. Role checks shall be enforced by the server, not only hidden in the UI; a user calling a function outside their role receives an "access denied" message. |
| NFR-RES-06 | Validation | Every input field (date, time, party size, phone number) shall be validated; invalid input shows a message next to the field and never causes an application error. |

## 5. Business rules (beyond CRUD)

Implemented in the service layer (not in UI handlers) and proven with test cases in Week 7.

| ID | Rule |
|---|---|
| BR-RES-01 | **No double booking.** Two bookings on the same table overlap if `startA < endB + turnTime` and `startB < endA + turnTime`, where `end = start + diningDuration`. An overlapping reservation is rejected. The same check applies when seating a walk-in: a walk-in cannot be seated at a table that has a reservation starting within the next dining duration plus turn-time. Defaults: dining duration 90 minutes, turn-time 15 minutes (configurable by the Manager). |
| BR-RES-02 | **Capacity fit.** A party can only be assigned to a table whose capacity is greater than or equal to the party size. |
| BR-RES-03 | **Clean before reuse.** A table becomes Available only after it is marked clean; it cannot be assigned while in Needs Cleaning. |
| BR-RES-04 | **Waiting-list order.** Walk-ins are served in arrival order; a later party may be seated first only if no earlier party fits the free table. |
| BR-RES-05 | **Table lifecycle.** Available → Reserved (automatically, from 30 minutes before a confirmed reservation starts) → Occupied (reservation checked in, or walk-in seated directly from Available) → Needs Cleaning (automatically when the table's bill is closed in the Billing module, or manually by a Waiter/Host when the table has no open order) → Available (marked clean). A reservation that becomes No-show returns the table from Reserved to Available. No other transition is allowed. |

## 6. Use cases (detailed)

### UC-03: Create Reservation

| Field | Description |
|---|---|
| Actor | Waiter/Host |
| Goal | Reserve a suitable table for a guest at a future date and time |
| Precondition | The Waiter/Host is logged in; at least one active table exists |
| Postcondition | A reservation is saved with status Confirmed and the slot no longer appears as available |
| Related requirements | FR-RES-02, FR-RES-03, BR-RES-01, BR-RES-02 |

**Main flow**
1. The Waiter/Host opens the Reservation screen.
2. The Waiter/Host enters the date, start time and party size, and clicks Search.
3. The system validates the input and lists the tables that fit the party size and have no overlapping reservation.
4. The Waiter/Host selects a table.
5. The Waiter/Host enters the guest name, phone number and an optional note, and clicks Confirm.
6. The system re-checks that the slot is still free, saves the reservation and shows a confirmation with the reservation code.

**Alternative flows**
- **3a. Invalid input** (date in the past, party size ≤ 0 or above the largest table, badly formatted time): the system shows a message next to the invalid field and does not search.
- **3b. No table available:** the system shows "No table available for this time" and offers two options: search another time, or add the guest to the waiting list (→ UC-05).
- **6a. Slot taken meanwhile** (another user booked the same table first): the system rejects the reservation, shows a conflict message and returns to step 3 with refreshed results.

### UC-05: Manage Walk-in Waiting List

| Field | Description |
|---|---|
| Actor | Waiter/Host |
| Goal | Queue walk-in guests fairly and seat them as soon as a suitable table is free |
| Precondition | The Waiter/Host is logged in |
| Postcondition | The party is either waiting in the queue or seated at a table marked Occupied |
| Related requirements | FR-RES-06, FR-RES-07, FR-RES-08, BR-RES-01 to BR-RES-04 |

**Main flow**
1. A walk-in party arrives and no suitable table is free.
2. The Waiter/Host enters the party name, party size and phone number, and clicks Add to waiting list.
3. The system saves the entry with the arrival time and shows the party's position in the queue.
4. When a table is marked clean and becomes Available, the system highlights the earliest waiting party that fits the table.
5. The Waiter/Host clicks Seat; the system sets the table to Occupied and removes the party from the waiting list.

**Alternative flows**
- **2a. Invalid input** (missing name, party size ≤ 0): the system shows a validation message and does not add the entry.
- **4a. No waiting party fits the free table:** the table stays Available and no party is highlighted.
- **5a. Party left before being seated:** the Waiter/Host clicks Remove; the system deletes the entry and moves the following parties up.
- **5b. Table not usable:** if the table is in Needs Cleaning, or has a reservation starting too soon (BR-RES-01), the system refuses and shows the reason.

## 7. Use cases owned by this module (numbering as in the Proposal)

| ID | Use case | Actor |
|---|---|---|
| UC-01 | Log in and log out | All roles |
| UC-02 | Manage tables and sections | Manager |
| UC-03 | Create reservation | Waiter/Host |
| UC-04 | Modify, cancel or check in a reservation | Waiter/Host |
| UC-05 | Manage walk-in waiting list | Waiter/Host |
| UC-06 | Update table status (mark needs cleaning, mark clean) | Waiter/Host |

Specifications of all use cases are in Appendix A of the Proposal draft.

## 8. Cross-module decisions (proposed — confirm at the team meeting)

1. Dining duration 90 minutes and turn-time 15 minutes are defaults the Manager can change; they do not depend on party size.
2. Check-in (Reserved → Occupied) belongs to this module; the Order module starts from an Occupied table.
3. When the Billing module closes a table's bill, the table moves to Needs Cleaning automatically (BR-RES-05).
4. There is no online booking; the system is for staff only.
5. Login and role-based access are owned by this module; staff accounts are seeded, not managed in the UI.
