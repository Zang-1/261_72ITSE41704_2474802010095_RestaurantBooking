# Final Project Report - Nguyen Nam Khoa's part

Draft, 21/11/2026. English, as the course requires.

## About this file

This file holds the parts of the Final Project Report that Nguyen Nam Khoa writes:
- the Billing & Reporting sections of Chapter 3;
- Khoa's rows for Chapter 4 and for Appendices B and C.

Giang copies these parts into `Template_Final-Project-Report.docx`, in the same way as `giang.md` and `lam.md`.

Every open place is filled: the test-case log is Appendix D, and the commit counts in Appendix C come from `main` on 27/11/2026.

## Chapter 3 — Billing & Reporting module sections

### 3.1 Problem Description and System Objectives (module part)

**Problem statement.**
At the end of a meal and at the end of a shift, the interviewed restaurant (18/10/2026, findings F11 and F12) has three problems:
- Splitting a bill between guests is done with a calculator and the shares often do not add up to the total.
- The restaurant takes cash, card, bank transfer and e-wallet, but nobody can say quickly how much came in by each method.
- Reconciliation at the end of a shift is manual: counting the till and collecting screenshots of transfers.

The ordering app the restaurant uses is not linked to the bill, so a table can be billed while the kitchen is still cooking, and a bill can be changed after it is paid.

**Objectives of the module.**
- Generate the bill from the order only when the kitchen is done, and lock the order (FR-21, BR-09).
- Split a bill evenly or by item so that the shares always add up exactly (FR-23, FR-24, BR-10).
- Record cash, card, bank transfer and e-wallet payments, with the change for cash (FR-25, BR-11, CR-04).
- Close a fully paid bill, make it read-only and free the table, in one transaction (FR-26, BR-12, NFR-08).
- Tie every payment to the open shift and give the Manager a shift report with a cash check (FR-27 to FR-29, BR-13).

### 3.2 System Analysis and Design (module part)

#### 3.2.1 Functional analysis
- Use cases UC-11 to UC-15, Appendix A; use-case diagram `docs/diagrams/giang/01-use-case.png`.
- Activity diagram of the billing flow: `docs/diagrams/khoa/12-activity-5-billing.png`.

#### 3.2.2 Data flow
- DFD level 1, billing process: `docs/diagrams/khoa/10-dfd-1c-billing.png`.

#### 3.2.3 Database design
ERD: `docs/diagrams/khoa/updated/w9-erd-billing.png` (README §2), matching Flyway `V4__billing_reporting.sql`.

| Table | Purpose | Key constraints |
|---|---|---|
| `shift` | A working shift opened and closed by the Manager | `opened_by_id`, `closed_by_id` FK to `users`; `counted_cash` (FR-29) ≥ 0 or null; `open_marker` is 1 while open and null once closed, and **unique**, so the database itself allows only one open shift (BR-13) |
| `bill` | The bill of one order | FK to `customer_order`; `total` BIGINT VND, CHECK > 0; `status` OPEN, CLOSED or CANCELLED; `split_type` NONE, EVEN or BY_ITEM; `shift_id` = shift in which it was closed |
| `bill_line` | A dish on the bill | Name, quantity and unit price **copied** from the order line, so a later menu change cannot alter a bill; `share_id` set only for a split by item |
| `bill_share` | One guest's part of a split bill | (`bill_id`, `number`) unique; `amount` CHECK > 0 |
| `payment` | Money received | FK to `bill`, `shift`, `users` and optionally `bill_share`; `amount` handed over, CHECK > 0; `change_amount` ≥ 0 and < amount; the revenue is `amount - change_amount` |

Money is always a whole number of VND in a `BIGINT` column and a Java `long`, never a floating-point number (NFR-08).

#### 3.2.4 Class design and state machines
Images for the report are in `docs/diagrams/khoa/updated/`; the Mermaid sources are in `docs/diagrams/khoa/README.md`.
- Class diagram: `w9-class-billing.png` (README §1).
- State machines: bill `w9-state-bill.png` (README §3); shift `w9-state-shift.png` (README §4).
- Sequences: generate and split, `w9-sequence-generate-split.png` (README §5); record payment and close, `w9-sequence-pay-close.png` (README §6).

Design decisions:
- **The rules live in `Bill`.** BR-09 to BR-12 and the payment part of BR-13 are methods of the `Bill` entity, so `BillTest` checks them without Spring or a database. The services only load and lock the rows.
- **The amount handed over is stored, not only the amount applied.** `Payment.appliedAmount() = amount - changeAmount`. The bill, the shares and the report all add up applied amounts, so a 500,000 note for a 465,000 bill counts as 465,000 of revenue, and the 35,000 change is still on record.
- **The report is calculated, not stored.** `ShiftReport` from the Week 4 class diagram became `ShiftReportResponse`, built on request from two queries. Nothing has to be kept in step when a payment is added.
- **One open shift, checked twice.** The service refuses a second open shift with a clear message; the unique key on `open_marker` catches two Managers clicking at the same moment.

```java
public static long[] evenShares(long total, int count) {
    if (count < MIN_SHARES || count > MAX_SHARES) {
        throw new InvalidInputException("shares", "The number of shares must be between 2 and 20");
    }
    long each = total / count / ROUNDING_UNIT * ROUNDING_UNIT;
    if (each <= 0) {
        throw new BusinessRuleException("A bill of " + Money.format(total) + " is too small to split into " + count + " shares");
    }
    long[] amounts = new long[count];
    Arrays.fill(amounts, each);
    amounts[0] = total - each * (count - 1);
    return amounts;
}
```
*Listing 3.5. BR-10 in `Bill`.*
Integer division rounds down, so `total / count / 1000 * 1000` is the share rounded down to 1,000 VND.
Share 1 takes what is left, so the shares always add up to the total: 1,000,000 in 3 gives 334,000 + 333,000 + 333,000.

```java
public Payment pay(BillShare share, PaymentMethod method, long amount, Shift openShift, User recordedBy, LocalDateTime at) {
    requireOpen();                                                   // BR-12
    if (openShift == null || !openShift.isOpen()) throw new BusinessRuleException("No shift is open; …");   // BR-13
    if (amount <= 0) throw new InvalidInputException("amount", "The amount must be greater than 0");       // BR-11
    long due = isSplit() ? share.remaining() : remaining();          // (share checks left out here)
    long change = 0;
    if (method == PaymentMethod.CASH) change = Math.max(0, amount - due);
    else if (amount > due) throw new InvalidInputException("amount", "A card payment cannot be more than …"); // BR-11
    Payment payment = new Payment(this, share, openShift, method, amount, change, at, recordedBy);
    payments.add(payment);
    if (remaining() == 0) close(openShift, at);                      // FR-26
    return payment;
}

private void close(Shift closingShift, LocalDateTime at) {
    status = BillStatus.CLOSED;
    closedAt = at;
    shift = closingShift;
    order.close();
    order.getTable().changeStatus(TableStatus.NEEDS_CLEANING);       // BR-05, Giang's state machine
}
```
*Listing 3.6. BR-11 to BR-13 and FR-26 in `Bill` (shortened).*
Every check runs before anything is added. The last payment closes the bill, the order and the table in the same transaction (`BillingService.pay`), so either all of it is saved or none of it (NFR-08).

#### 3.2.5 Interface design
Screens, in `docs/screenshots/khoa/`:
- **Bill screen** (Waiter/Host), opened with the **Bill** button of an Occupied table on the floor overview or from the order screen. The bill lines, total, paid and remaining on the left; payment and split on the right. Screenshots: `01-bill-generated.png`, `02-bill-split-evenly-cash-change.png`, `04-bill-split-by-item.png`, `03-bill-paid-and-closed.png`.
  - The amount is filled in with what is due, so a single payment takes 4 steps from the floor overview: Bill → Generate bill → method → Record payment (NFR-05).
  - For cash the change is shown before the payment is sent; for card, transfer and e-wallet an amount above what is due is marked next to the field (`06-card-more-than-due.png`).
  - Refusals are shown as a red message, for example the kitchen still cooking (`05-bill-refused-kitchen-busy.png`).
- **Shifts screen** (Manager): open or close the shift with the counted cash, the list of shifts, and the report of the chosen shift. Screenshots: `07-shift-report-provisional.png`, `08-close-shift-refused-unpaid.png`, `09-shift-report-closed-cash-check.png`.
- Floor overview with the Bill button: `10-floor-bill-button.png`.

### 3.3 System Development and Implementation (module part)

#### 3.3.1 Technologies and tools
Same as Giang's table. Khoa's module adds no library.

#### 3.3.2 Implemented functions

| Function (FR / UC) | Input | Output | Endpoint | Screen |
|---|---|---|---|---|
| Generate bill (FR-21, UC-12, BR-09) | order id | Bill OPEN with the charged lines copied; order LOCKED. Refused while a dish is New, In Progress or unsent, without an open shift, or when the order already has a bill | `POST /api/bills` | Bill |
| Show bill (FR-22) | table id or bill id | Lines, total, paid, remaining, shares and payments | `GET /api/bills?tableId=`, `GET /api/bills/{id}` | Bill |
| Cancel bill (UC-12 4a, BR-09) | bill id | CANCELLED (kept) and the order OPEN again; refused after a payment | `POST /api/bills/{id}/cancel` | Bill |
| Split evenly (FR-23, BR-10) | 2–20 shares | Shares rounded to 1,000 VND, remainder on share 1; replaces an earlier split; refused after a payment | `POST /api/bills/{id}/split/even`, `DELETE /api/bills/{id}/split` | Bill |
| Split by item (FR-24) | the dishes of each share | One share per group, amount = its dishes; refused if a dish is left out or given twice | `POST /api/bills/{id}/split/by-item` | Bill |
| Record payment (FR-25, BR-11, CR-04) | method (cash, card, bank transfer, e-wallet), amount, share when split | Payment stored with the change for cash; refused for 0, for a non-cash amount above what is due, or a closed bill | `POST /api/bills/{id}/payments` | Bill |
| Close bill (FR-26, BR-12) | — (automatic) | When paid in full: bill CLOSED and read-only, order CLOSED, table Needs cleaning, in one transaction | (part of the payment) | Bill, floor overview |
| Open and close shift (FR-27, BR-13, UC-11) | counted cash, optional (FR-29) | One open shift at a time; closing refused while a bill is unpaid, listing the tables | `POST /api/shifts`, `POST /api/shifts/{id}/close`, `GET /api/shifts`, `GET /api/shifts/current` | Shifts |
| Shift report (FR-28, FR-29, UC-15) | shift id | Closed bills, revenue, revenue per method, number of payments and of split bills, expected and counted cash and the difference; marked provisional while the shift is open | `GET /api/shifts/{id}/report` | Shifts |
| Sample data | — | The 12 sample orders are billed and paid in 12 past shifts (lunch 10:00–14:30, dinner 17:00–22:30) through the domain methods; one shift is 20,000 VND short in the till | `BillingSeeder` (called by Lam's `OrderSeeder`) | Shifts |

**Implementation notes**
- **Locks in one order.** Every billing request takes its row locks in the same order: open shift → order or bill → table (`SELECT … FOR UPDATE`). Two requests can therefore never wait for each other in a circle (no deadlock).
  - A payment and the Manager closing the shift both lock the shift row first, so the shift cannot close between the check "a shift is open" and the save of the payment (BR-13).
  - Two payments for the same bill are handled one after the other, so the bill can never be paid twice.
- **Link to the other modules.**
  - Lam's `CustomerOrder` gives `lock()`, `close()` and the kitchen status of each line; Khoa added `unlock()` for a cancelled bill.
  - The table goes to Needs cleaning only through Giang's `DiningTable.changeStatus`, so the table lifecycle of BR-05 is still enforced in one place.
  - Giang's "Guests left" action already refuses a table whose order is LOCKED, which now means "unpaid bill" (UC-06 1a, TC-BIL-33).
- **Re-splitting.** Shares are numbered 1..N with a unique key. Hibernate inserts new rows before it deletes old ones, so the service deletes the old split and flushes before it creates the new one.
- **Errors.** Rule violations are 409 with a message the waiter can act on ("The shift cannot be closed: unpaid bills at table 3, table 7 (BR-13)"); wrong input is 400 with the message next to the field.

#### 3.3.3 Unimplemented or incomplete functions

| Item | State | Reason |
|---|---|---|
| Printed receipt | Not built | Not in the requirements; the bill screen shows the closed bill |
| Refund or correction of a payment | Not built | BR-12 makes a closed bill final; a mistake before closing can be fixed only by paying the rest |
| Moving a bill to another table | Not possible | Lam's FR-19 moves only an OPEN order; a LOCKED order (bill generated) stays at its table. Cancel the bill, move, and generate it again |
| Discounts, service charge, VAT | Not built | Not in the requirements; the menu prices are final prices |
| NFR-05, one payment in 4 steps | Counted on the screen | 4 clicks from the floor overview; not timed with users |

#### 3.3.4 Other deliverables
- Flyway migration `V4__billing_reporting.sql`.
- Diagrams updated to the code: `docs/diagrams/khoa/updated/`, sources in `docs/diagrams/khoa/README.md`.
- Test cases `tests/test-cases-billing.md` and the shared template `tests/test-case-template.md`.
- Run guide `docs/run-guide.md` (installation and a walkthrough of the whole visit, from booking to the shift report).

### 3.4 Testing and Evaluation (module part)

#### 3.4.1 Testing process
- **Methods.**
  - Unit tests of the billing rules without Spring: `BillTest` (BR-09 to BR-13, including a parameterised test of BR-10 with five totals).
  - API tests with MockMvc on H2 with the real seed data: `BillControllerTest`, `ShiftControllerTest`.
  - Performance tests of NFR-04: `BillingPerformanceTest`.
  - A test that the sample bills follow the rules: `BillingSeederTest`.
  - The screens were walked through in the browser with the H2 profile by an automated Playwright script (open shift, generate, split evenly, pay in cash with change, card, e-wallet, split by item, close the shift with counted cash). The screenshots in `docs/screenshots/khoa/` come from that run. The walkthrough found one bug, fixed before release: the amount field had `step="1000"`, so the browser refused amounts such as 150,000.
- **Test cases.** TC-BIL-01 to 35 and TC-SHF-01 to 10 (45 cases) in `tests/test-cases-billing.md`, Appendix D.
- **Results** on 21/11/2026:
  - `mvn test`: 176 automated tests, 0 failures. 51 of them are for Billing & Reporting.
  - NFR-04: a bill of 50 lines answered in 2 ms (limit 2 s); a report of 300 bills in 25 ms (limit 3 s), on H2.
  - All 45 documented cases pass.

#### 3.4.2 System evaluation
**Strengths**
- Even shares always add up to the total (TC-BIL-07), which was the restaurant's main complaint about splitting (F11).
- The last payment, closing the bill and freeing the table are one transaction (NFR-08); a closed bill cannot change (TC-BIL-18).
- The cash check (expected, counted, difference) replaces counting the till against screenshots (F12, CR-05).
- The report needs two queries whatever the number of bills, so NFR-04 holds with a large margin.
- Only one shift can be open even if two Managers click at once (unique key), and a shift cannot close in the middle of a payment (row lock).

**Limitations**
- NFR-04 was timed on H2 in memory, not on MySQL on a laptop.
- The React screens have no automated component tests; they were checked by the Playwright walkthrough and by hand.
- A payment entered by mistake cannot be reversed (no refund function).
- Revenue is counted per shift; there is no daily or monthly report yet.

#### 3.4.3 Comparison between theory and practice
- **Theory → practice.**
  - The bill state diagram became three statuses and one guard (`requireOpen`): a closed or cancelled bill refuses every change.
  - "Money as integers" (NFR-08) removed every rounding question except the one BR-10 defines on purpose.
  - Atomicity: the domain method checks everything before it adds a payment, and the service runs the whole payment in one transaction.
- **Differences.**
  - The Week 4 class diagram stored `ShiftReport`; in the code it is calculated, so it can never disagree with the payments.
  - The Week 4 ERD had no link from a bill to a shift. The report needs to know in which shift a bill was closed, so `bill.shift_id` was added.
  - The interview added e-wallet (CR-04) and the counted cash (CR-05, FR-29); both were built. By-item split (FR-24) dropped to Could (CR-03) but was built anyway.

## Chapter 4 rows (for Giang to merge)

**4.1 Summary, Billing & Reporting row**

| Proposal item | Completed | Not completed / changed |
|---|---|---|
| Billing & Reporting, FR-21 to FR-29, BR-09 to BR-13 | FR-21 to FR-29, UC-11 to UC-15, BR-09 to BR-13, each rule with unit and API tests; bill and shifts screens; NFR-04 measured; sample bills and shifts | E-wallet added (CR-04); the shift report is calculated instead of stored; no refund function |

**Achievements (Khoa):** shares that always add up, payments that close the bill and free the table in one transaction, and a shift report with a cash check that answers within milliseconds.

**Future work (Billing & Reporting):**
- Refund or correction of a payment, with the Manager's approval.
- Daily and monthly revenue reports, export to Excel.
- Printed or e-mailed receipts; VAT invoice.
- Discounts and service charge.

**4.2 Knowledge and skills gained**

| Member | Technical skills | Professional skills | Practical experience |
|---|---|---|---|
| Nguyen Nam Khoa | Storing money as whole VND in `long`/`BIGINT` and keeping rounding in one place (BR-10); mapping a bill with its lines, shares and payments in JPA (cascade, orphan removal, and why Hibernate inserts before it deletes); pessimistic row locks and taking them in a fixed order to avoid deadlocks; a unique key as a second guard for "only one open shift"; aggregate JPQL queries with `GROUP BY` and `join fetch` for a fast report; unit tests without Spring and API tests with MockMvc; a React screen that shows the change before the payment is sent | Writing the expected result of a test case from the requirement before running it; reading another member's code (Lam's order, Giang's table lifecycle) and changing it only through their public methods; working on a feature branch and describing changes in commits; recording real test results with the date | The interview showed that the restaurant's real pain is the end of the shift: counting the till and matching transfer screenshots by hand. That turned the counted-cash field (CR-05) from a detail into the most useful part of the report. Splitting a bill "by calculator" fails because of rounding, which is why BR-10 puts the remainder on one share. The module also depends on the other two: a bill needs the kitchen to be done, and closing it must free the table |

## Appendices B and C (Khoa's rows)

### Appendix B — Declaration of AI-tool use

| Tool | What it was used for | Which part of the work | How the output was verified |
|---|---|---|---|
| Claude (Anthropic), via Claude Code | Generating the Billing & Reporting code, screens, tests and test cases; drafting diagrams, the run guide and the module's part of Chapter 3 | `src/backend`, `src/frontend`, `tests/test-cases-billing.md`, `docs/diagrams/khoa`, `docs/run-guide.md` | Khoa ran the automated tests, walked through the screens in the browser, read `Bill` and `V4__billing_reporting.sql` against BR-09 to BR-13 and recalculated the BR-10 examples by hand |

### Appendix C — Individual contribution table

| Member | Main responsibilities | Main artefacts produced | Commits | Share of work (%) | Signature |
|---|---|---|---|---|---|
| Nguyen Nam Khoa | Billing & Reporting module: bills, splitting, payments, shifts, shift report; shared test-case template | Requirement draft; `docs/diagrams/khoa/`; Flyway V4; Billing & Reporting backend and frontend; 51 automated tests and 45 test cases; sample bills and shifts; run guide | 5 | 6 | |
