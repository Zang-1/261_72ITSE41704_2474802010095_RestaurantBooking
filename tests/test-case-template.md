# Shared test-case template (all modules)

Owner: Nguyen Nam Khoa. Every module's test-case file uses these columns, in this order, as required by
Course Guidelines §6.3. The three module files already follow it:
`test-cases-table-reservation.md` (Giang), `test-cases-order-kitchen.md` (Lam), `test-cases-billing.md` (Khoa).

## Columns

| Column | What to write | Example |
|---|---|---|
| ID | `TC-<AREA>-<nn>`, numbered per area, never reused. Areas: RES, TBL, WL (Giang); MENU, ORD, KIT (Lam); BIL, SHF (Khoa) | TC-BIL-15 |
| Purpose | What is checked, with the requirement or rule in brackets | Cash can be more than the remaining amount (BR-11) |
| Precondition | The state before the test: logged-in role, data that must exist | Logged in as Waiter/Host; bill of 265,000 VND, shift open |
| Input | The request or the user action, with the values | POST /api/bills/{id}/payments, CASH, 300,000 |
| Expected result | HTTP status and what the user or the data must show, taken from the requirement, not from the code | 201; change 35,000; bill CLOSED |
| Actual result | What the run showed. "As expected" only if every part of the expected result was seen; otherwise describe it | As expected |
| Pass/Fail | Pass or Fail | Pass |
| Automated test | `Class.method` of the automated test, or "UI" for a check by hand in the browser | BillTest.cashRecordsChange |

## Rules

- Write the expected result before running the test.
- Fill **Actual result** and **Pass/Fail** from a real run, and write the date and the `mvn test` totals at the
  top of the file (for example "run on 21/11/2026, 176 tests, 0 failures").
- A failing case stays in the table as Fail until it is fixed and run again; do not delete it.
- Every business rule BR-01 to BR-13 has at least one case.
- A UI check names the screen and the steps, and its screenshot goes in `docs/screenshots/<member>/`.

## Empty table to copy

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| | | | | | | | |
