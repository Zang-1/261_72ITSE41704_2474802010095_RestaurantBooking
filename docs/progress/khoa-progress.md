# Progress — Nguyen Nam Khoa (Billing & Reporting module)

Plan: Proposal Table 2.10. Evidence: commits and pull requests in this repository.

| Week | Dates | Planned (Khoa) | Status | Evidence |
|---|---|---|---|---|
| 2 | 21–27/9 | Billing requirements (FR, NFR, BR, use cases) | Done | `docs/requirements/billing-reporting-module-draft.md` (26/9) |
| 4 | 5–11/10 | Class, ERD, state, sequence, DFD and activity diagrams of the module | Done | `docs/diagrams/khoa/` (10/10), PR #4, #7, #8 |
| 5 | 12–18/10 | Bill and shift classes, repositories | Done late (21/11) | `V4__billing_reporting.sql`, `domain/Bill*.java`, `Shift.java`, `Payment.java`, repositories |
| 6 | 19–25/10 | Validation, test cases, shared test-case template | Done late (21/11) | `tests/test-case-template.md`, `tests/test-cases-billing.md` |
| 7 | 26/10–1/11 | BR-09 to BR-13 with unit tests | Done late (21/11) | `BillTest` (TC-BIL-01 to 20, TC-SHF-01) |
| 8 | 2–8/11 | Exception handling; sample data; updated diagrams and screenshots | Done late (21/11) | `BillingSeeder`, `docs/diagrams/khoa/updated/`, `docs/screenshots/khoa/` |
| 9 | 9–15/11 | Feature freeze; module section of report Chapter 3 | Done late (21/11) | `docs/report/khoa.md` |
| 10 | 16–22/11 | Run guide; appendices | Done (21/11) | `docs/run-guide.md`, Appendix B and C rows in `docs/report/khoa.md` |

## Billing & Reporting module (21/11/2026)

**Code**
- Migration `V4__billing_reporting.sql`: `shift`, `bill`, `bill_share`, `bill_line`, `payment`. Money is BIGINT VND.
- Domain: `Bill`, `BillLine`, `BillShare`, `Payment`, `Shift`, enums `BillStatus`, `SplitType`, `PaymentMethod` (with E_WALLET, CR-04).
- Services `BillingService`, `ShiftService`; controllers `BillController` (`/api/bills`), `ShiftController` (`/api/shifts`).
- Generate and view the bill only when the kitchen is done (FR-21, FR-22, BR-09); cancel an unpaid bill to order more.
- Even split with rounding (FR-23, BR-10) and split by item (FR-24).
- Payments in cash (with change), card, bank transfer and e-wallet (FR-25, BR-11, CR-04).
- The last payment closes the bill (read-only, BR-12), closes the order and sets the table to Needs cleaning through
  `DiningTable.changeStatus`, all in one transaction (FR-26, NFR-08).
- Open and close shift; every payment belongs to the open shift; one open shift; no closing with unpaid bills (FR-27, BR-13).
- Shift report: closed bills, revenue, revenue per method, payments, split bills; counted cash and difference (FR-28, FR-29).
- Screens: bill `/bill/:tableId` (Bill button on the floor overview and the order screen) and shifts `/shifts` (Manager).
- Error handling: every refusal is 409 with a message, every wrong input 400 next to the field; locks in the order
  shift → order/bill → table.
- Sample data: `BillingSeeder` pays Lam's 12 sample orders in 12 past shifts (`OrderSeeder` now calls it instead of
  closing the orders without a bill).

**Tests**
- `BillTest` (unit, BR-09 to BR-13), `BillControllerTest`, `ShiftControllerTest`, `BillingPerformanceTest` (NFR-04),
  `BillingSeederTest`: 51 tests.
- NFR-04: bill of 50 lines 2 ms (limit 2 s); report of 300 bills 25 ms (limit 3 s).
- Results: `mvn test` 176 tests, 0 failures, 21/11/2026.
- 45 documented cases TC-BIL-01 to 35, TC-SHF-01 to 10 with actual results; shared template for all modules.

**Documents**
- Diagrams redrawn from the code (`docs/diagrams/khoa/README.md`, images in `updated/`), screenshots in `docs/screenshots/khoa/`.
- Run guide `docs/run-guide.md`; Chapter 3 Billing part and Appendix B and C rows in `docs/report/khoa.md`.

## Still open
- Khoa fills the `[Khoa: …]` parts of `docs/report/khoa.md` (skills gained, verification in Appendix B) in Khoa's own words.
- Commit counts and shares in Appendix C at v1.0.
