# Billing & Reporting Module — Requirements Draft (Week 3)

**Module:** Billing & Reporting  
**Owner:** Nguyen Nam Khoa (2474802010194)  
**Status:** Draft for team review — to be merged into the Project Proposal in Week 3

---

## 1. Scope of this module

### In scope

- Create and manage customer bills
- Manage bill lines and order items
- Process customer payments
- Split bills and payments
- Close completed bills
- Manage working shifts
- Generate shift reports
- View revenue and payment information

### Out of scope

- Table and reservation management → Table & Reservation module
- Order creation and kitchen processing → Order & Kitchen module
- Online customer payment → excluded from this project

---

## 2. Actors

| Actor | Description |
|---|---|
| Waiter / Host | Staff member who manages customer bills and processes payments |
| Manager | Manager who views shift reports and billing information |

---

## 3. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-BIL-01 | The system shall allow Waiter / Host to create a bill for a customer's completed order. | Must |
| FR-BIL-02 | The system shall display bill information including bill lines, quantities, prices and total amount. | Must |
| FR-BIL-03 | The system shall allow Waiter / Host to split a bill into multiple payments. | Must |
| FR-BIL-04 | The system shall allow Waiter / Host to record payment information and close a fully paid bill. | Must |
| FR-BIL-05 | The system shall allow Manager to generate and view shift reports containing revenue and payment information. | Must |

---

## 4. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-BIL-01 | Performance | The billing screen shall display bill information within 2 seconds under normal operating conditions. |
| NFR-BIL-02 | Data Integrity | The system shall store billing and payment information consistently without losing completed payment records. |
| NFR-BIL-03 | Security | Only authorized users shall be allowed to access management shift reports. |
| NFR-BIL-04 | Reliability | Billing and payment records shall remain available after the application is restarted. |
| NFR-BIL-05 | Usability | Staff shall be able to complete a standard payment process with a clear and simple workflow. |

---

## 5. Use Case 1: Close Bill and Process Payment

**Actor:** Waiter / Host

**Goal:** Complete customer payment and close the bill.

### Preconditions

- The staff member is logged in.
- The customer has an existing bill.
- The bill contains valid order items.

### Main Flow

1. Waiter / Host opens the customer's bill.
2. The system displays bill lines and the total amount.
3. Waiter / Host selects the payment method.
4. Waiter / Host enters the payment amount.
5. The system calculates the remaining amount.
6. Waiter / Host confirms the payment.
7. The system records the payment.
8. The system verifies that the bill has been fully paid.
9. The system marks the bill as paid and closed.

### Alternative Flows

- If the payment amount is insufficient, the system shows the remaining amount.
- If invalid payment information is entered, the system displays an error and asks the staff member to correct it.
- If the bill has already been closed, the system prevents another payment from being recorded.

### Postconditions

- Payment information is stored.
- The bill is marked as paid and closed when the full amount has been received.

---

## 6. Use Case 2: Generate Shift Report

**Actor:** Manager

**Goal:** Generate a report containing revenue and payment information for a selected shift.

### Preconditions

- The Manager is logged in.
- The selected shift exists.

### Main Flow

1. Manager opens the shift reporting function.
2. The system displays available shifts.
3. Manager selects a shift.
4. The system retrieves billing and payment records for the selected shift.
5. The system calculates the total revenue.
6. The system groups payment information by payment method.
7. The system generates the shift report.
8. Manager views the generated report.

### Alternative Flows

- If the selected shift has no billing records, the system displays a report with zero revenue.
- If the selected shift does not exist, the system displays an error message.

### Postconditions

- The shift report is displayed with billing and payment information.
- The Manager can use the report to review the selected shift.

---

## 7. Module Entities

The Billing & Reporting module will use the following main entities:

- Bill
- BillLine
- Payment
- Shift
- ShiftReport

These entities will be further designed during the Architecture & Design phase.

---

## 8. Week 3 Contribution

**Nguyen Nam Khoa**

- Prepared Billing & Reporting functional requirements.
- Prepared Billing & Reporting non-functional requirements.
- Prepared two Billing & Reporting use cases.
- Prepared the Billing & Reporting module scope and actors.
- Contributed the Billing & Reporting section to the Project Proposal.
