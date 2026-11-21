# Test cases — Billing & Reporting module (Nguyen Nam Khoa)

Bills, splitting, payments, shifts and the shift report (UC-11 to UC-15, FR-21 to FR-29, BR-09 to BR-13, NFR-04).
Columns follow the shared template `test-case-template.md` (Course Guidelines §6.3).

Every row is also an automated test (column **Automated test**) in `src/backend/src/test`, run with `mvn test`
on an in-memory H2 database seeded with the standard accounts, 27 tables and the sample menu.
**Actual result** and **Pass/Fail** come from the `mvn test` run on 21/11/2026 (176 tests, 0 failures; 51 of
them for Billing & Reporting). Rows marked "UI" were also walked through in the browser on the same day with
the H2 demo profile; their screenshots are in `docs/screenshots/khoa/`.

Unless stated otherwise, the unit-test bill is 2 × Beef pho 75,000 + 1 × Grilled pork with rice 85,000 +
2 × Iced tea 15,000 = **265,000 VND**, every dish Ready in the kitchen, with a shift open.

## Generate and cancel a bill (UC-12, FR-21, FR-22, BR-09)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-BIL-01 | The bill copies every charged line and locks the order (FR-21) | Order with 3 dishes, all Ready | generate the bill | Bill OPEN, 3 lines with name, quantity and unit price copied, total 265,000, paid 0, remaining 265,000; order LOCKED | As expected | Pass | BillTest.generateCopiesLinesAndLocksOrder |
| TC-BIL-02 | No bill while the kitchen is working (BR-09) | 2 dishes sent, one In Progress, one New | generate the bill | Refused; message names both dishes; order stays OPEN | As expected | Pass | BillTest.noBillWhileTheKitchenIsWorking |
| TC-BIL-03 | Dishes not sent to the kitchen block the bill (BR-09) | Cooked order plus one unsent Iced tea | generate the bill | Refused: "Iced tea not sent to the kitchen yet …"; order still editable | As expected | Pass | BillTest.unsentDishesBlockTheBill |
| TC-BIL-04 | Cancelled dishes are not charged | One dish cancelled before cooking (BR-08), one Ready | generate the bill; then an order whose only dish is cancelled | 1 line, total 75,000; the second order is refused ("nothing to charge") | As expected | Pass | BillTest.cancelledDishesAreNotCharged |
| TC-BIL-05 | One bill per order (BR-09) | Bill already generated | generate again | Refused | As expected | Pass | BillTest.lockedOrderGetsNoSecondBill |
| TC-BIL-06 | Cancel only before payment (BR-09, UC-12 4a) | Unpaid bill; a second bill with 10,000 paid | cancel both | First: CANCELLED, order OPEN, a dish can be added. Second: refused, order stays LOCKED | As expected | Pass | BillTest.cancelOnlyBeforePayment |
| TC-BIL-21 | Generate through the API (FR-21, FR-22) | Logged in as Waiter/Host; shift open; occupied table, 3 dishes Ready | POST /api/bills {orderId} | 201; status OPEN, table number, 3 lines, total = order total, paid 0, remaining = total, splitType NONE; order LOCKED | As expected | Pass | BillControllerTest.generateBill |
| TC-BIL-22 | API refuses while cooking (BR-09) | 2 dishes still New | POST /api/bills | 409 "The kitchen is still preparing …" | As expected | Pass | BillControllerTest.kitchenMustBeDone, UI `05-bill-refused-kitchen-busy.png` |
| TC-BIL-23 | A shift must be open (UC-12 precondition, BR-13) | No shift open | POST /api/bills | 409 "No shift is open; ask the Manager to open a shift first (BR-13)" | As expected | Pass | BillControllerTest.billNeedsAnOpenShift |
| TC-BIL-24 | One bill per order; access; input errors (NFR-10, NFR-11) | Bill exists for the order | POST again as Waiter; POST as Kitchen Staff; orderId 999999; empty body; GET /api/bills/999999 | 409; 403; 400 "Order not found" on orderId; 400 "Order is required" on orderId; 404 | As expected | Pass | BillControllerTest.oneBillPerOrderAndAccess |
| TC-BIL-25 | The bill screen finds the table's unpaid bill (FR-22) | Table has an open bill | GET /api/bills?tableId= | 200; one bill with that id | As expected | Pass | BillControllerTest.openBillOfTable |
| TC-BIL-32 | Cancel through the API, then bill again (BR-09) | Unpaid bill | POST /cancel; generate again; pay 1,000; POST /cancel | CANCELLED and order OPEN; new bill 201; then 409 "A payment is already recorded, so the bill cannot be cancelled" | As expected | Pass | BillControllerTest.cancelBill |
| TC-BIL-33 | Guests cannot leave with an unpaid bill (BR-05, UC-06 1a) | Table has an open bill | POST /api/tables/{id}/needs-cleaning | 409; table stays Occupied | As expected | Pass | BillControllerTest.tableWithUnpaidBillCannotBeCleaned |

## Split a bill (UC-13, FR-23, FR-24, BR-10)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-BIL-07 | Even-split rounding (BR-10) | Unit test | (1,000,000; 3), (265,000; 2), (100,000; 3), (999,999; 7), (45,500; 20) | 334,000 + 2 × 333,000; 133,000 + 132,000; 34,000 + 2 × 33,000; 147,999 + 6 × 142,000; 7,500 + 19 × 2,000. Shares always add up to the total | As expected | Pass | BillTest.evenSharesAddUpExactly (5 runs) |
| TC-BIL-08 | 2 to 20 shares, each at least 1,000 VND (BR-10) | Unit test | 1 share; 21 shares; 15,000 in 20 shares | 400-type error for 1 and 21; refused "too small to split" | As expected | Pass | BillTest.shareCountLimits |
| TC-BIL-09 | Split evenly, split again, remove the split (FR-23) | Bill 265,000 | 2 shares, then 3 shares, then remove | 133,000 + 132,000; then 89,000 + 88,000 + 88,000 numbered 1–3; then no shares | As expected | Pass | BillTest.splitEvenly |
| TC-BIL-10 | Split by item (FR-24) | Bill with 3 lines | share 1: pho; share 2: rice + tea | Shares 150,000 and 115,000; each line knows its share | As expected | Pass | BillTest.splitByItem |
| TC-BIL-11 | Every line in exactly one share (FR-24, UC-13 2b-1) | Bill with 3 lines | a line left out; a line in two shares; an empty share; only 1 share | All refused; bill stays not split | As expected | Pass | BillTest.splitByItemNeedsEveryLineOnce |
| TC-BIL-12 | No split after a payment (UC-13 1a) | 50,000 paid | split evenly; remove split | Both refused | As expected | Pass | BillTest.noSplitAfterPayment |
| TC-BIL-26 | Even split through the API (FR-23, NFR-11) | Unpaid bill of 3 dishes | POST /split/even 3; then 2; then 1 and 21; DELETE /split | 3 shares, share 1 has the remainder; then 2 shares; 400 "The number of shares must be between 2 and 20" on the shares field; split removed | As expected | Pass | BillControllerTest.splitEvenly, UI `02-bill-split-evenly-cash-change.png` |
| TC-BIL-27 | Split by item through the API (FR-24) | Unpaid bill of 3 dishes | POST /split/by-item [[line1, line3], [line2]]; then a dish left out | 200, shares = sums of their lines, each line shows its share; then 400 "Every dish must be in a share; not assigned: …" | As expected | Pass | BillControllerTest.splitByItem, UI `04-bill-split-by-item.png` |

## Record payment and close the bill (UC-14, FR-25, FR-26, BR-11, BR-12)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-BIL-13 | Amount must be greater than 0 (BR-11) | Unit test | cash 0; card −1 | Both refused on the amount field; no payment stored | As expected | Pass | BillTest.amountMustBePositive |
| TC-BIL-14 | Card, bank transfer and e-wallet cannot exceed what is due (BR-11, CR-04) | Bill 265,000 | each method with 265,001; then e-wallet 265,000 | Refused three times on the amount field; e-wallet 265,000 accepted and closes the bill | As expected | Pass | BillTest.nonCashCannotExceedRemaining |
| TC-BIL-15 | Cash may be more and the change is recorded (BR-11) | Bill 265,000 | cash 300,000 | Payment 300,000, change 35,000, applied 265,000; remaining 0 | As expected | Pass | BillTest.cashRecordsChange |
| TC-BIL-16 | Partial payment keeps the bill open (UC-14 5a) | Bill 265,000 | card 100,000; then cash 200,000 | After the first: OPEN, paid 100,000, remaining 165,000, table Occupied. After the second: change 35,000, CLOSED | As expected | Pass | BillTest.partialPaymentKeepsBillOpen |
| TC-BIL-17 | The last payment closes the bill, the order and frees the table (FR-26, BR-05) | Bill 265,000 | bank transfer 265,000 | Bill CLOSED with closedAt and the shift; order CLOSED; table Needs cleaning | As expected | Pass | BillTest.fullPaymentClosesBill |
| TC-BIL-18 | A closed bill is final (BR-12) | Bill paid in full | cash 1,000; split; cancel | All refused; still 1 payment | As expected | Pass | BillTest.closedBillIsFinal |
| TC-BIL-19 | Shares are paid one by one (FR-25) | Bill split 133,000 + 132,000 | pay without share; card 133,001 on share 1; card 133,000 on share 1; cash 1,000 on share 1; cash 150,000 on share 2 | Refused (choose a share); refused (more than due); share 1 paid, bill OPEN; refused (share already paid); change 18,000, bill CLOSED | As expected | Pass | BillTest.sharesArePaidSeparately |
| TC-BIL-28 | Cash payment through the API closes the bill (FR-25, FR-26) | Logged in as Waiter/Host; unpaid bill | POST /payments CASH total + 50,000 | 201; CLOSED, remaining 0, change 50,000, applied = total, recordedBy "waiter"; order CLOSED; table Needs cleaning | As expected | Pass | BillControllerTest.cashPaymentClosesBill, UI `03-bill-paid-and-closed.png` |
| TC-BIL-29 | Wrong amounts through the API (BR-11, NFR-11) | Unpaid bill | CARD total + 1; CARD 0; method "CHEQUE" | 400 "A card payment cannot be more than …" on amount; 400 "The amount must be greater than 0" on amount; 400 | As expected | Pass | BillControllerTest.cardCannotOverpay, UI `06-card-more-than-due.png` |
| TC-BIL-30 | Partial payments, then a closed bill takes no more (UC-14 5a, CR-04, BR-12) | Unpaid bill | BANK_TRANSFER 50,000; E_WALLET the rest; CASH 10,000 | OPEN with remaining total − 50,000; CLOSED; 409 "This bill is closed and can no longer be changed (BR-12)" | As expected | Pass | BillControllerTest.partialThenClosed |
| TC-BIL-31 | Pay a split bill share by share through the API (FR-25) | Bill split in 2 | pay without shareId; CARD share 1; CASH share 2 | 400 "Choose which share is being paid" on shareId; 201 OPEN, share 1 remaining 0; 201 CLOSED | As expected | Pass | BillControllerTest.payShares |
| TC-BIL-20 | No payment without an open shift (BR-13) | Unit test | pay with no shift; with a closed shift; with the open shift | Refused, refused; accepted and the payment belongs to the open shift | As expected | Pass | BillTest.paymentNeedsAnOpenShift |

## Shifts and shift report (UC-11, UC-15, FR-27, FR-28, FR-29, BR-13)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-SHF-01 | A shift closes once; counted cash ≥ 0 (BR-13, FR-29) | Unit test, open shift | close with −1; close with 1,500,000; close again | Refused, still open; closed with counted cash and closedBy; refused "already closed" | As expected | Pass | BillTest.shiftClosesOnce |
| TC-SHF-02 | Open a shift; only one at a time (FR-27, BR-13) | Logged in as Manager; no shift open | POST /api/shifts twice | 201 open, openedBy "manager"; then 409 "A shift is already open since …" | As expected | Pass | ShiftControllerTest.opensShift |
| TC-SHF-03 | Only a Manager manages shifts; staff can see the open shift (NFR-10) | Shift open | Waiter: open, close, report; Kitchen: list; Waiter: GET /current | 403, 403, 403, 403; 200 with the shift | As expected | Pass | ShiftControllerTest.accessRules |
| TC-SHF-04 | No open shift: no payment (BR-13, UC-14 1b) | Bill generated, then the shift closed | GET /current; POST /payments | 204; 409 "No shift is open; ask the Manager to open a shift before taking payments (BR-13)" | As expected | Pass | ShiftControllerTest.noShiftNoPayment |
| TC-SHF-05 | A shift with an unpaid bill cannot close (BR-13, UC-11 3a) | Shift open; one unpaid bill | POST /api/shifts/{id}/close | 409 "The shift cannot be closed: unpaid bills at table N (BR-13)"; shift still open | As expected | Pass | ShiftControllerTest.unpaidBillBlocksClosing, UI `08-close-shift-refused-unpaid.png` |
| TC-SHF-06 | Close with counted cash; cash check on the report (FR-27, FR-29, CR-05) | One bill paid in cash (total + 20,000 handed, change 20,000) | close with counted cash total − 10,000; report; close again; open a new shift | Closed, closedBy "manager"; report: not provisional, expected = total, counted = total − 10,000, difference −10,000; 409 "already closed"; new shift 201 | As expected | Pass | ShiftControllerTest.closeWithCountedCash, UI `09-shift-report-closed-cash-check.png` |
| TC-SHF-07 | Counted cash cannot be negative (NFR-11) | Shift open | close with −1 | 400 "Counted cash cannot be negative" on countedCash | As expected | Pass | ShiftControllerTest.countedCashValidation |
| TC-SHF-08 | Shift report figures (FR-28) | Bill A paid in cash with change; bill B split in 2 and paid by card and e-wallet; bill C unpaid | GET /api/shifts/{id}/report | provisional true; closed bills 2; split bills 1; payments 3; revenue A + B; cash = A, card = share 1, e-wallet = share 2, transfer 0; no cash difference; 2 bills listed, the second EVEN | As expected | Pass | ShiftControllerTest.shiftReport, UI `07-shift-report-provisional.png` |
| TC-SHF-09 | Empty and unknown shift (UC-15 4a) | Shift open, no bills | report; report of shift 999999; list | Zero revenue, all methods 0; 404; list starts with the shift | As expected | Pass | ShiftControllerTest.emptyAndUnknownShift |

## Performance and sample data (NFR-04, Week 8 seed data)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-BIL-34 | A bill with 50 lines is shown within 2 seconds (NFR-04) | Bill of 50 dishes | GET /api/bills/{id}, 1 warm-up and 5 timed calls | Slowest call < 2,000 ms | Slowest 2 ms | Pass | BillingPerformanceTest.fiftyLineBill |
| TC-SHF-10 | A shift report of 300 bills within 3 seconds (NFR-04) | 300 closed bills of 3 dishes, every method, 60 split | GET /api/shifts/{id}/report, 1 warm-up and 5 timed calls | closedBills 300; slowest call < 3,000 ms | Slowest 25 ms | Pass | BillingPerformanceTest.reportOfThreeHundredBills |
| TC-BIL-35 | The sample bills follow the rules | Fresh database with sample data | start the application | Every sample order has one CLOSED bill with remaining 0 and total = order total; split shares add up; change only on cash; there are even and by-item splits and cash with change; every shift is closed; report revenue = bill totals; one shift has a cash difference | As expected | Pass | BillingSeederTest (2 tests) |

Times were measured on H2 in memory on the build machine; MySQL on a laptop is slower but the report uses two
queries whatever the number of bills (bills with their tables in one `join fetch`, totals per method grouped by
the database), so it stays far under the limit.

## Coverage of the business rules

| Rule | Cases |
|---|---|
| BR-09 Bill only when the kitchen is done | TC-BIL-02, 03, 05, 06, 22, 32 |
| BR-10 Even-split rounding | TC-BIL-07, 08, 09, 26 |
| BR-11 Valid payments | TC-BIL-13, 14, 15, 19, 29 |
| BR-12 Closed bills are final | TC-BIL-18, 30 |
| BR-13 Shift control | TC-BIL-20, 23, TC-SHF-01, 02, 04, 05 |
