# Reservation Module — Requirements Draft (Week 2)

**Module:** Table & Reservation
**Owner:** Kieu Bao Giang (2474802010095)
**Status:** Draft for team review — to be merged into the Project Proposal (Chapter 2) in Week 3

> IDs use the prefix `RES` so the three module drafts can be merged without collisions.
> They will be renumbered to FR-01, FR-02 … when merged into the Proposal template.

---

## 1. Scope of this module

**In scope**
- Table catalogue: tables, seat capacity, section (e.g. Indoor, Terrace, VIP room)
- Advance reservations: create, modify, cancel, no-show
- Walk-in waiting list
- Table status lifecycle: Available → Reserved → Occupied → Needs Cleaning → Available
- Floor overview of all tables and their current status

**Out of scope** (handled by other modules or excluded)
- Orders, menu and kitchen tickets → Order & Kitchen module (Lam)
- Bills, payments and shift reports → Billing & Reporting module (Khoa)
- Online booking by customers, deposits, SMS/email reminders → excluded from this project

## 2. Actors

| Actor | Description |
|---|---|
| Host / Waiter | Front-of-house staff who take reservations, seat guests and manage the waiting list |
| Manager | Configures tables and sections, views reports, can override table status |

## 3. Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-RES-01 | The system shall allow a Manager to create, edit and deactivate a table, with a unique table number, a seat capacity between 1 and 20, and a section. A table that has future reservations cannot be deactivated. | Must |
| FR-RES-02 | The system shall allow a Host to search for available tables by date, start time and party size, returning only tables whose capacity is greater than or equal to the party size and that have no overlapping reservation. | Must |
| FR-RES-03 | The system shall allow a Host to create a reservation for a selected table with guest name, phone number, party size, date, start time and an optional note. The reservation shall be rejected if it overlaps another reservation on the same table, where each reservation occupies the table for its dining duration plus a turn-time buffer. | Must |
| FR-RES-04 | The system shall allow a Host to modify or cancel a reservation before its start time. Cancelling releases the time slot immediately so it appears in availability searches. | Must |
| FR-RES-05 | The system shall automatically mark a reservation as No-show when the guest has not checked in 15 minutes after the start time, and release the table. | Should |
| FR-RES-06 | The system shall allow a Host to add a walk-in party (name, party size, phone number) to a waiting list, ordered by arrival time, and show the estimated waiting position. | Must |
| FR-RES-07 | The system shall allow a Host to seat the first waiting party whose size fits a table that has just become Available, and remove that party from the waiting list. | Must |
| FR-RES-08 | The system shall enforce the table status lifecycle Available → Reserved → Occupied → Needs Cleaning → Available. A table in Needs Cleaning cannot be assigned to any reservation or walk-in until a Host marks it clean. | Must |
| FR-RES-09 | The system shall display a floor overview showing every table with its number, section, capacity and current status, colour-coded by status. | Should |

## 4. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-RES-01 | Performance | An availability search (FR-RES-02) shall return results within 2 seconds for a restaurant with up to 50 tables and 500 reservations per week. |
| NFR-RES-02 | Usability | A Host shall be able to create a reservation from the availability screen in no more than 3 steps (search → select table → confirm details). |
| NFR-RES-03 | Data integrity | Two Hosts confirming a reservation for the same table and overlapping time at the same moment shall result in exactly one reservation being saved; the other receives a clear conflict message. |
| NFR-RES-04 | Reliability | All tables, reservations and waiting-list entries shall be stored in the MySQL database and remain available after the application is restarted. |
| NFR-RES-05 | Security | Only users with the Manager role can create, edit or deactivate tables; a Host attempting it receives an "access denied" message. |
| NFR-RES-06 | Validation | Every input field (date, time, party size, phone number) shall be validated; invalid input shows a message next to the field and never causes an application error. |

## 5. Business rules (beyond CRUD)

These are the rules that will be implemented in the service layer (not in UI handlers) and proven with test cases in Week 7.

| ID | Rule |
|---|---|
| BR-RES-01 | **No double booking.** Two reservations on the same table overlap if `startA < endB + turnTime` and `startB < endA + turnTime`. Such a reservation is rejected. Default dining duration: 90 minutes; default turn-time: 15 minutes (configurable). |
| BR-RES-02 | **Capacity fit.** A party can only be assigned to a table whose capacity is ≥ party size. |
| BR-RES-03 | **Clean before reuse.** A table becomes Available only after it is marked clean; it cannot be assigned while in Needs Cleaning. |
| BR-RES-04 | **Waiting list order.** Walk-ins are served in arrival order; a later party may only be seated first if no earlier party fits the free table. |

## 6. Use cases (draft)

### UC-RES-01: Create Reservation

| Field | Description |
|---|---|
| Actor | Host / Waiter |
| Goal | Reserve a suitable table for a guest at a future date and time |
| Precondition | The Host is logged in; at least one active table exists |
| Postcondition | A reservation is saved with status Confirmed and the slot no longer appears as available |
| Related requirements | FR-RES-02, FR-RES-03, BR-RES-01, BR-RES-02 |

**Main flow**
1. The Host opens the Reservation screen.
2. The Host enters the date, start time and party size, and clicks Search.
3. The system validates the input and lists the tables that fit the party size and have no overlapping reservation.
4. The Host selects a table.
5. The Host enters the guest name, phone number and an optional note, and clicks Confirm.
6. The system re-checks that the slot is still free, saves the reservation and shows a confirmation with the reservation code.

**Alternative flows**
- **3a. Invalid input** (date in the past, party size ≤ 0 or above the largest table, badly formatted time): the system shows a message next to the invalid field and does not search.
- **3b. No table available:** the system shows "No table available for this time" and offers two options: search another time, or add the guest to the waiting list (→ UC-RES-02).
- **6a. Slot taken meanwhile** (another Host booked the same table first): the system rejects the reservation, shows a conflict message and returns to step 3 with refreshed results.

### UC-RES-02: Manage Walk-in Waiting List

| Field | Description |
|---|---|
| Actor | Host / Waiter |
| Goal | Queue walk-in guests fairly and seat them as soon as a suitable table is free |
| Precondition | The Host is logged in |
| Postcondition | The party is either waiting in the queue or seated at a table marked Occupied |
| Related requirements | FR-RES-06, FR-RES-07, FR-RES-08, BR-RES-02, BR-RES-03, BR-RES-04 |

**Main flow**
1. A walk-in party arrives and no suitable table is free.
2. The Host enters the party name, party size and phone number, and clicks Add to waiting list.
3. The system saves the entry with the arrival time and shows the party's position in the queue.
4. When a table is marked clean and becomes Available, the system highlights the earliest waiting party that fits the table.
5. The Host clicks Seat; the system sets the table to Occupied and removes the party from the waiting list.

**Alternative flows**
- **2a. Invalid input** (missing name, party size ≤ 0): the system shows a validation message and does not add the entry.
- **4a. No waiting party fits the free table:** the table stays Available and no party is highlighted.
- **5a. Party left before being seated:** the Host clicks Remove; the system deletes the entry and moves the following parties up.
- **5b. Table not yet cleaned:** if the Host tries to seat a party at a table in Needs Cleaning, the system refuses and shows "Table must be marked clean first".

## 7. Proposed use-case list for the module (for the team meeting)

Contribution to the team target of 14 use cases:

| ID | Use case | Actor |
|---|---|---|
| UC-RES-01 | Create reservation | Host |
| UC-RES-02 | Manage walk-in waiting list | Host |
| UC-RES-03 | Modify / cancel reservation | Host |
| UC-RES-04 | Update table status (seat, mark needs cleaning, mark clean) | Host |
| UC-RES-05 | Manage tables and sections | Manager |

## 8. Open questions for the team meeting

1. Is the default dining duration (90 min) and turn-time (15 min) acceptable, or should they depend on party size?
2. Does check-in of a reservation (Reserved → Occupied) belong here, or does the Order module start from an Occupied table? Proposed: here.
3. Agree with Lam and Khoa on the shared link: an `Order` references a `Table`; a `Bill` references the `Order`.
4. Should customers book online? Proposed: no — staff-only system, stated as out of scope.
