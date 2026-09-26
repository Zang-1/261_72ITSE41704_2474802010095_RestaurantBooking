# Project Proposal — Draft

**Restaurant Table Booking and Kitchen Order System**
72ITSE41704 – Application Programming Project · Class group 261_72ITSE41704_01 · Semester 1, 2026–2027
Lecturer: Dr. Nguyen Tri Hai

| Member | Student ID | Role |
|---|---|---|
| Kieu Bao Giang (Team leader) | 2474802010095 | Table & Reservation module; login and access control; proposal integration |
| Tang Thoai Lam | 2474802010210 | Order & Kitchen module |
| Nguyen Nam Khoa | 2474802010194 | Billing & Reporting module |

> **How to use this draft.** This is the working text of the Proposal. The submitted file must be
> `Template_ProjectProposal.docx` (Guidelines §5): copy each chapter into the matching template section, keep the
> template's chapter names, cover page, non-plagiarism declaration and table of contents.
> Items marked **[TODO]** must be completed by the team before submission — they need real information
> that only the team has.
>
> Source drafts: `docs/requirements/reservation-module-draft.md`, `docs/requirements/order-kitchen-module-draft.md`,
> `docs/requirements/billing-reporting-module-draft.md`.

---

## Chapter 1 — Introduction

### 1.1 Background and rationale

Many small and medium-sized restaurants still run their front of house and kitchen on paper and word of mouth.
Reservations are written in a notebook, walk-in guests are queued informally at the door, orders are handwritten
and carried to the kitchen, and the bill is assembled by hand at the end of the meal. This causes recurring,
visible problems:

- **Front of house:** the same table is promised to two parties, a table is given to walk-in guests shortly before a
  reservation arrives, and waiting guests are not seated in a fair order.
- **Kitchen:** orders are lost or misread when the restaurant is busy, main courses are prepared before starters,
  and waiters keep taking orders for dishes that have already run out.
- **Billing:** splitting a bill between guests is done with a calculator and often does not add up, and at the
  end of a shift nobody can quickly tell how much was taken in cash, by card or by transfer.

The people who suffer are the waiters and hosts who handle complaints, the kitchen staff who waste food on
changed or duplicated orders, the manager who cannot see the state of the restaurant, and the guests who wait
longer and receive the wrong dishes.

The reservation side of this problem is documented in the hospitality literature. Tse and Poon [12] observed a
busy restaurant in Hong Kong for two years and found that demand regularly exceeded the number of tables, while
no-shows, cancellations and walk-ins made it difficult to decide how many reservations to accept. A system that
keeps reservations, walk-ins and table status in one place is a precondition for managing these situations.
The team will confirm the order-taking, kitchen and billing problems with restaurant staff (see §1.4).

### 1.2 Objectives

**General objective.** Build a web application that lets a small or medium-sized restaurant manage table
reservations, walk-in guests, orders, kitchen preparation and billing in one consistent system, with three
staff roles.

**Specific objectives** (all verified by Week 9, release v1.0):

1. Implement **15 use cases** end-to-end for three roles (Waiter/Host, Kitchen Staff, Manager).
2. Enforce **13 business rules** in the service layer, each covered by at least one automated unit test.
3. Prevent double booking: **zero** overlapping reservations for the same table, including when two users book
   at the same moment.
4. Document at least **30 test cases** with expected and actual results (course minimum for a team of three: 20 [2]).
5. Meet the measurable non-functional requirements in §2.3, for example an availability search in under
   2 seconds and a new order visible in the kitchen within 3 seconds.

### 1.3 Scope

**In scope**
- Login and role-based access for three staff roles
- Tables and sections; reservations (create, modify, cancel, check in, no-show); walk-in waiting list; table status
- Menu management; order taking; course sequencing; kitchen display; out-of-stock handling
- Bill generation; even and by-item bill splitting; payments (cash, card, bank transfer); shifts; shift report

**Explicitly out of scope**
- Online booking or ordering by customers; deposits; SMS or email notifications
- Real payment gateways or card terminals (payments are recorded, not processed)
- Staff account administration in the UI (accounts are seeded at installation)
- Ingredient-level stock control and purchasing
- Multiple restaurant branches
- Tax invoices; prices are integer amounts in VND, VAT included, with no service charge

### 1.4 Requirements-gathering methods

1. **Document review** — published research on restaurant reservations, no-shows and walk-ins [12], the course
   catalogue entry for this topic [2], requirements-engineering guidance [4], and use-case writing guidance [7].
2. **Interview** — in Weeks 3–4, a semi-structured interview with the owner or staff of at least one restaurant in
   Ho Chi Minh City about reservations, order taking, the kitchen and billing. The restaurant, the date and the
   findings will be reported in Chapter 2 of the Final Report, and any requirement changes will be listed there.
3. **Observation** — if the restaurant agrees, observing the front of house and the kitchen during a busy period
   to see where orders are lost or delayed.
4. **Team walkthroughs** — each module's use cases are walked through by the other two members to find gaps
   between modules (for example, what happens to a table after its bill is closed).

### 1.5 Expected outcomes

- **Product:** a working web application (Spring Boot backend, React frontend, MySQL database) with seeded,
  realistic sample data, tagged v1.0 in the repository.
- **Documentation:** this proposal, UML use-case, class and sequence diagrams, an ERD, wireframes, a test-case
  table, a run guide, and the Final Project Report.
- **Skills:** requirements analysis, object-oriented design, layered architecture, automated testing, and
  team development with Git branches, pull requests and release tags.

### 1.6 Structure of the final report

The final report follows the course template [1]. Chapter 1 introduces the problem, objectives and scope. Chapter 2 presents the theoretical background, the
current manual process and the requirement analysis. Chapter 3 covers system analysis, design, implementation
and testing. Chapter 4 concludes with results against the proposal, limitations and future work. Appendices hold
the full test-case log, the AI-use declaration and the individual contribution table.

---

## Chapter 2 — Proposed project content

### 2.1 Topic and problem

The system is catalogue topic **#22, Restaurant Table Booking and Kitchen Order System**. It covers the full
journey of a restaurant visit: a guest reserves a table or joins the waiting list, is seated, orders, the kitchen
prepares the dishes in the right course order, and the bill is split and paid. It is divided into three modules,
one per member, that share one database and one set of users:

| Module | Owner | Covers |
|---|---|---|
| Table & Reservation | Kieu Bao Giang | Login, tables, reservations, waiting list, table status |
| Order & Kitchen | Tang Thoai Lam | Menu, orders, course sequencing, kitchen display, out-of-stock |
| Billing & Reporting | Nguyen Nam Khoa | Bills, splitting, payments, shifts, shift reports |

**Actors**

| Actor | Description |
|---|---|
| Waiter/Host | Front-of-house staff: reservations, seating, waiting list, orders, bills and payments |
| Kitchen Staff | Sees tickets on the kitchen display, updates preparation status, marks dishes out of stock |
| Manager | Configures tables and the menu, opens and closes shifts, views reports, can do everything a Waiter/Host can |

### 2.2 Functional requirements

Priority: **Must** (needed to pass), **Should** (planned), **Could** (only if time allows).

**Login and Table & Reservation — Kieu Bao Giang**

| ID | Requirement | Priority | Source ID |
|---|---|---|---|
| FR-01 | The system shall require every user to log in with a username and password, and shall show only the screens and actions permitted for that user's role. | Must | FR-AUTH-01 |
| FR-02 | The system shall allow a Manager to create, edit and deactivate a table, with a unique table number, a seat capacity between 1 and 20, and a section. A table that has future reservations cannot be deactivated. | Must | FR-RES-01 |
| FR-03 | The system shall allow a Waiter/Host to search for available tables by date, start time and party size, returning only tables whose capacity is at least the party size and that have no overlapping reservation. | Must | FR-RES-02 |
| FR-04 | The system shall allow a Waiter/Host to create a reservation with guest name, phone number, party size, date, start time and an optional note, and shall reject it if it overlaps another reservation on the same table (BR-01). | Must | FR-RES-03 |
| FR-05 | The system shall allow a Waiter/Host to modify or cancel a reservation before its start time, and to check in a reservation when the guests arrive. | Must | FR-RES-04 |
| FR-06 | The system shall mark a reservation as No-show when the guests have not checked in 15 minutes after the start time, and release the table. | Should | FR-RES-05 |
| FR-07 | The system shall allow a Waiter/Host to add a walk-in party to a waiting list ordered by arrival time, and show each party's position. | Must | FR-RES-06 |
| FR-08 | The system shall allow a Waiter/Host to seat the first waiting party whose size fits a table that has just become Available. | Must | FR-RES-07 |
| FR-09 | The system shall enforce the table status lifecycle (BR-05); a table in Needs Cleaning cannot be assigned until it is marked clean. | Must | FR-RES-08 |
| FR-10 | The system shall display a floor overview of all tables with number, section, capacity and status, colour-coded by status. | Should | FR-RES-09 |

**Order & Kitchen — Tang Thoai Lam**

| ID | Requirement | Priority | Source ID |
|---|---|---|---|
| FR-11 | The system shall allow a Waiter/Host to create an order for an Occupied table by choosing items from the menu grouped by category; out-of-stock items are not shown. | Must | FR-ORD-01 |
| FR-12 | The system shall allow a Waiter/Host to add items, change the quantity (1–50), remove items and add a note of up to 200 characters to each order line before it is sent to the kitchen. | Must | FR-ORD-02 |
| FR-13 | The system shall assign every menu item to a course (Starter, Main, Dessert) and send items to the kitchen in course order (BR-06). | Must | FR-ORD-03 |
| FR-14 | When a Waiter/Host sends an order, the system shall create a kitchen ticket containing the sent items and show it on the kitchen display. | Must | FR-ORD-04 |
| FR-15 | The system shall allow Kitchen Staff to change the status of each ticket item from New to In Progress to Ready, and shall show the status to the Waiter/Host. | Must | FR-ORD-05 |
| FR-16 | The system shall allow a Manager to add, edit and hide menu items, with name, category, course and a price greater than 0 VND. A menu item that appears in any past order can be hidden but not deleted. | Must | FR-ORD-06 |
| FR-17 | The system shall allow Kitchen Staff or a Manager to mark a menu item as out of stock or back in stock; an out-of-stock item disappears from every order screen (BR-07). | Must | FR-ORD-07 |
| FR-18 | The system shall refuse to modify or cancel an order item once its kitchen ticket item is In Progress (BR-08); new items may still be added and are sent as a new ticket. | Must | FR-ORD-08 |
| FR-19 | The system shall allow a Waiter/Host to move an open order to another table. | Could | FR-ORD-09 |
| FR-20 | The system shall allow a Waiter/Host to view the orders of a table in the current shift. | Could | FR-ORD-10 |

**Billing & Reporting — Nguyen Nam Khoa**

| ID | Requirement | Priority | Source ID |
|---|---|---|---|
| FR-21 | The system shall allow a Waiter/Host to generate a bill for an Occupied table from its order, copying each order line with quantity, unit price and line total (BR-09). | Must | FR-BIL-01 |
| FR-22 | The system shall display a bill with its lines, grand total, amount paid and amount remaining. | Must | FR-BIL-02 |
| FR-23 | The system shall allow a Waiter/Host to split a bill evenly into 2 to 20 shares using the rounding rule BR-10. | Must | FR-BIL-03 |
| FR-24 | The system shall allow a Waiter/Host to split a bill by item, assigning every bill line to exactly one share. | Should | FR-BIL-04 |
| FR-25 | The system shall allow a Waiter/Host to record one or more payments (cash, card, bank transfer) against a bill or a share, and shall calculate the change for cash payments (BR-11). | Must | FR-BIL-05 |
| FR-26 | The system shall close a bill when it is paid in full; closing makes the bill read-only (BR-12) and sets the table to Needs Cleaning (BR-05). | Must | FR-BIL-06 |
| FR-27 | The system shall allow a Manager to open a shift and to close it; every payment belongs to the shift that is open when it is recorded (BR-13). | Must | FR-BIL-07 |
| FR-28 | The system shall allow a Manager to generate a report for a shift showing the number of closed bills, total revenue, revenue per payment method and number of split bills. | Must | FR-BIL-08 |

### 2.3 Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | An availability search returns results within 2 seconds for up to 50 tables and 500 reservations per week. |
| NFR-02 | Performance | A sent order appears on the kitchen display within 3 seconds. |
| NFR-03 | Performance | Marking an item out of stock is reflected on every open order screen within 5 seconds, without reloading the page. |
| NFR-04 | Performance | A bill with up to 50 lines is displayed within 2 seconds; a shift report covering up to 300 bills is generated within 3 seconds. |
| NFR-05 | Usability | A Waiter/Host can create a reservation in at most 3 steps, enter a standard 5-item order in at most 60 seconds, and record a single payment in at most 4 steps from the table screen. |
| NFR-06 | Data integrity | When two users confirm the same table for overlapping times at the same moment, exactly one reservation is saved and the other user sees a conflict message. |
| NFR-07 | Data integrity | Sending an order is atomic: if saving fails, the order stays unsent, no ticket is created, and sending again never creates a duplicate ticket. |
| NFR-08 | Data integrity | Recording the final payment and closing the bill happen in one database transaction; money is stored as integer VND, never as floating-point numbers. |
| NFR-09 | Reliability | All data is stored in MySQL and is unchanged after the application is restarted. |
| NFR-10 | Security | Passwords are stored only as BCrypt hashes; role checks are enforced by the server, and a request outside the user's role receives an "access denied" response. |
| NFR-11 | Validation | Every input is validated on the server; invalid input produces a message next to the field and never causes an unhandled error. |
| NFR-12 | Portability | The application runs on Windows and macOS with Java 17, Node.js 18 or later and MySQL 8, in current versions of Chrome and Edge, and builds from a fresh clone by following the README. |

### 2.4 Business rules

These rules are what makes the system more than data entry. They are implemented in service classes, not in UI
event handlers, and each has automated unit tests.

| ID | Rule | Owner |
|---|---|---|
| BR-01 | **No double booking.** Two bookings of the same table overlap if `startA < endB + turnTime` and `startB < endA + turnTime`, where `end = start + diningDuration`. Overlapping reservations are rejected; a walk-in cannot be seated at a table whose next reservation starts within one dining duration plus turn-time. Defaults: 90 and 15 minutes, set by the Manager. | Giang |
| BR-02 | **Capacity fit.** A party is only assigned to a table whose capacity is at least the party size. | Giang |
| BR-03 | **Clean before reuse.** A table in Needs Cleaning cannot be assigned until it is marked clean. | Giang |
| BR-04 | **Waiting-list order.** Walk-ins are seated in arrival order; a later party goes first only if no earlier party fits the free table. | Giang |
| BR-05 | **Table lifecycle.** Available → Reserved (from 30 minutes before a confirmed reservation) → Occupied (check-in or walk-in seated) → Needs Cleaning (automatically when the table's bill is closed, or manually when the table has no open order) → Available (marked clean). A No-show returns the table from Reserved to Available. No other transition is allowed. | Giang |
| BR-06 | **Course sequencing.** Within an order, items are sent in course order (Starter → Main → Dessert): an item of a later course cannot be sent while an item of an earlier course in the same order is still unsent. The kitchen display lists items by course, then by time sent. | Lam |
| BR-07 | **Out of stock.** An out-of-stock item is removed from every order screen and cannot be added or sent; items already in sent tickets are not affected. | Lam |
| BR-08 | **Lock after cooking starts.** Once a ticket item is In Progress it cannot be modified or cancelled. New items can still be added to the order and are sent as a new ticket. | Lam |
| BR-09 | **Bill only when the kitchen is done.** A bill can be generated only when no ticket item of the order is New or In Progress. Generating the bill locks the order; to add items, the unpaid bill must be cancelled first, which is allowed only while no payment has been recorded. | Khoa |
| BR-10 | **Even-split rounding.** For N shares, each share is `floor(total / N / 1,000) × 1,000` VND and the remainder is added to the first share, so the shares always add up exactly to the bill total. Example: 1,000,000 VND split 3 ways gives 334,000 + 333,000 + 333,000. | Khoa |
| BR-11 | **Valid payments.** A payment amount must be greater than 0. Card and bank-transfer payments cannot exceed the amount remaining; a cash payment may exceed it, and the change is recorded. | Khoa |
| BR-12 | **Closed bills are final.** A bill closes only when payments cover its total. A closed bill cannot be edited, deleted or receive further payments. | Khoa |
| BR-13 | **Shift control.** Payments can be recorded only while a shift is open; only one shift can be open at a time; a shift cannot be closed while it has unpaid bills. | Khoa |

### 2.5 Use cases

Full specifications (actors, preconditions, main and alternative flows) are in **Appendix A**.

| ID | Use case | Primary actor | Requirements | Owner |
|---|---|---|---|---|
| UC-01 | Log in and log out | All roles | FR-01 | Giang |
| UC-02 | Manage tables and sections | Manager | FR-02 | Giang |
| UC-03 | Create reservation | Waiter/Host | FR-03, FR-04 | Giang |
| UC-04 | Modify, cancel or check in a reservation | Waiter/Host | FR-05, FR-06 | Giang |
| UC-05 | Manage walk-in waiting list | Waiter/Host | FR-07, FR-08 | Giang |
| UC-06 | Update table status | Waiter/Host | FR-09, FR-10 | Giang |
| UC-07 | Manage menu | Manager | FR-16 | Lam |
| UC-08 | Create order and send to kitchen | Waiter/Host | FR-11 to FR-14, FR-18 | Lam |
| UC-09 | Process kitchen ticket | Kitchen Staff | FR-15 | Lam |
| UC-10 | Mark menu item out of stock or back in stock | Kitchen Staff, Manager | FR-17 | Lam |
| UC-11 | Open and close shift | Manager | FR-27 | Khoa |
| UC-12 | Generate bill | Waiter/Host | FR-21, FR-22 | Khoa |
| UC-13 | Split bill | Waiter/Host | FR-23, FR-24 | Khoa |
| UC-14 | Record payment and close bill | Waiter/Host | FR-25, FR-26 | Khoa |
| UC-15 | Generate shift report | Manager | FR-28 | Khoa |

FR-19 and FR-20 are Could requirements and will be added to UC-08 only if time allows.

### 2.6 Technologies

| Technology | Purpose | Justification |
|---|---|---|
| Java 17, Spring Boot 3 [8] | Backend REST API | Spring's controller, service and repository layers give the presentation / business / data-access separation the course requires [3], [6], and the team already knows Java. |
| Spring Data JPA (Hibernate) | Data access | Maps domain classes to tables and removes boilerplate DAO code, so effort goes into the business rules; generated code will be identified in the report. |
| MySQL 8 [10] | Database | A relational database with transactions and unique constraints, which the no-double-booking and payment-integrity rules depend on. |
| Spring Security, BCrypt | Login and roles | Provides hashed passwords and server-side role checks for the three roles. |
| React 18 (JavaScript) [9] | Frontend | Reusable components suit the several role-specific screens (floor overview, order screen, kitchen display). |
| Short polling (every 2 s) | Kitchen and out-of-stock updates | The simplest way to meet NFR-02 and NFR-03; WebSocket is a stretch goal. |
| JUnit 5 [11], Mockito | Automated tests | Unit tests for every business rule in the service layer, independent of the UI. |
| Git, GitHub | Version control | Feature branches, pull requests and release tags v0.1, v0.2 and v1.0, as required by the course. |
| draw.io or PlantUML | Diagrams | Use-case, class and sequence diagrams in UML 2.5 notation [5] and the ERD, kept in `/docs`. |

### 2.7 Work plan

Week dates are Monday to Sunday; exact submission deadlines are those published on E-learning/CTE.

| Week | Tasks | Responsible | Expected output |
|---|---|---|---|
| 1 (14–20/9) | Form team, register topic, create repository and README | Giang (registration, repository); Lam, Khoa (review README) | Registration form; repository with `/src /docs /tests` |
| 2 (21–27/9) | Requirements per module: FR, NFR, business rules, two detailed use cases | Giang: reservation and login; Lam: order and kitchen; Khoa: billing and reporting | Three requirement drafts in `/docs/requirements` |
| 3 (28/9–4/10) | Merge drafts into the template; Chapter 1 and 4 (Giang); Chapter 2 technologies and work plan (Lam); Chapter 3 expected results and references (Khoa); final review against checklist 12.1 (all three, led by Giang) | Giang, Lam, Khoa as listed | **Project Proposal (PDF + DOCX) — 20%** |
| 4 (5–11/10) | Shared layered architecture and ERD (led by Giang); class, use-case and sequence diagrams and wireframes for each module | Giang: reservation; Lam: order and kitchen; Khoa: billing | Diagrams and wireframes in `/docs` |
| 5 (12–18/10) | Project skeleton, database schema, login (Giang); domain classes and repositories per module; basic CRUD | Giang: tables and reservations; Lam: menu and orders; Khoa: bills and shifts | Tag **v0.1** — application runs and connects to MySQL |
| 6 (19–25/10) | Input validation; shared test-case table format (Khoa); 5 test cases per module; cross code review | Giang reviews Khoa, Lam reviews Giang, Khoa reviews Lam | About 15 documented test cases |
| 7 (26/10–1/11) | Business rules: BR-01 to BR-05 (Giang), BR-06 to BR-08 (Lam), BR-09 to BR-13 (Khoa), each with unit tests; Week 7 self-check (Appendix D) | Giang, Lam, Khoa as listed | Tag **v0.2**; self-check in `/docs` |
| 8 (2–8/11) | Exception handling for each module; seed data with 50–200 realistic records (Lam); security check (Giang); report performance check (Khoa) | Giang, Lam, Khoa as listed | Updated diagrams and screenshots |
| 9 (9–15/11) | Feature freeze; end-to-end test of the full visit (Giang leads); run guide (Khoa); each member writes their module in report Chapter 3 | Giang, Lam, Khoa as listed | Tag **v1.0**; draft report |
| 10 (16–22/11) | Chapter 1 and 4 (Giang), Chapter 2 (Lam), appendices and test log (Khoa); repository clean-up; checklist 12.2 | Giang, Lam, Khoa as listed | **Final Project Report (PDF + DOCX) — 80%** |

---

## Chapter 3 — Expected results

### 3.1 Features

- **Front of house:** a colour-coded floor overview, reservation booking with automatic conflict checks,
  check-in and no-show handling, and a fair walk-in waiting list.
- **Kitchen:** orders sent directly to a kitchen display in course order, preparation status visible to waiters,
  and out-of-stock dishes removed from ordering immediately.
- **Billing:** bills generated from orders, even and by-item splitting that always adds up, cash, card and
  bank-transfer payments, and shift reports.
- **Control:** three roles with server-side access checks, and all data kept in MySQL.

### 3.2 Data

The domain model is expected to contain about 17 classes, excluding UI and utility classes:

| Module | Main classes |
|---|---|
| Shared | User (with role) |
| Table & Reservation | TableSection, DiningTable, Reservation, WaitlistEntry |
| Order & Kitchen | MenuCategory, MenuItem, Order, OrderLine, KitchenTicket, KitchenTicketItem |
| Billing & Reporting | Bill, BillLine, BillShare, Payment, Shift, ShiftReport |

Status values (table status, reservation status, ticket item status, bill status) and Course are enumerations.
`TABLE` and `ORDER` are reserved words in SQL, so the database tables will be named `dining_table` and
`customer_order`. Key relationships: a reservation belongs to one table; an order belongs to one table visit; a
kitchen ticket belongs to one order; a bill is generated from one order; a payment belongs to one bill and one
shift.

### 3.3 Screens

| No. | Screen | Role |
|---|---|---|
| 1 | Login | All |
| 2 | Floor overview | Waiter/Host, Manager |
| 3 | Reservation search and booking | Waiter/Host |
| 4 | Reservation list (modify, cancel, check in) | Waiter/Host |
| 5 | Waiting list | Waiter/Host |
| 6 | Table and section management | Manager |
| 7 | Menu management | Manager |
| 8 | Order taking | Waiter/Host |
| 9 | Kitchen display | Kitchen Staff |
| 10 | Bill and split | Waiter/Host |
| 11 | Payment | Waiter/Host |
| 12 | Shift management | Manager |
| 13 | Shift report | Manager |

### 3.4 Learning outcomes

- Turning a real business process into testable requirements and use cases (CLO1).
- Working as a team through Git branches, pull requests and release tags (CLO2).
- Designing a layered, object-oriented system with UML and an ERD (CLO3).
- Implementing and testing business rules, validation and exception handling (CLO4).
- Writing clear technical documentation in English (CLO5).

---

## Chapter 4 — Conclusion

Restaurants that manage reservations, orders and bills on paper lose tables to double bookings, lose orders in
the kitchen, and cannot split or reconcile payments reliably. The team proposes a three-role web application,
built on a layered Spring Boot and React architecture with a MySQL database, that covers the whole visit from
reservation to payment through 15 use cases and 13 business rules enforced in the service layer. The expected
outcome is a working, tested system tagged v1.0 in Week 9, with a complete Final Report in Week 10.

**Main risk:** the three modules depend on one another. An order needs an Occupied table, a bill needs a
finished order, and closing a bill changes the table status, so a delay or a design mismatch in one module
blocks the others. **Mitigation:** the shared ERD and the status lifecycles (BR-05, BR-08, BR-09) are agreed in
Week 4 before any code is written; the skeleton and database schema are built together in Week 5; and the
Billing module starts against sample data so it does not wait for the other two modules to be finished.

---

## References

[1] N. T. Hai, "Application Programming Project — Course Guidelines for Students," Faculty of Information
Technology, Van Lang University, Ho Chi Minh City, Vietnam, 2026.

[2] N. T. Hai, "Application Programming Project — Suggested Project Titles," Faculty of Information Technology,
Van Lang University, Ho Chi Minh City, Vietnam, 2026.

[3] I. Sommerville, *Software Engineering*, 10th ed. Boston, MA, USA: Pearson, 2016.

[4] *Systems and Software Engineering — Life Cycle Processes — Requirements Engineering*, ISO/IEC/IEEE 29148:2018,
2018.

[5] Object Management Group, "OMG Unified Modeling Language (OMG UML), Version 2.5.1," Dec. 2017.

[6] M. Fowler, *Patterns of Enterprise Application Architecture*. Boston, MA, USA: Addison-Wesley, 2002.

[7] A. Cockburn, *Writing Effective Use Cases*. Boston, MA, USA: Addison-Wesley, 2001.

[8] "Spring Boot Reference Documentation." [Online]. Available: https://docs.spring.io/spring-boot/
(accessed Sep. 26, 2026).

[9] "React Documentation." [Online]. Available: https://react.dev/ (accessed Sep. 26, 2026).

[10] "MySQL 8.0 Reference Manual." [Online]. Available: https://dev.mysql.com/doc/refman/8.0/en/
(accessed Sep. 26, 2026).

[11] "JUnit 5 User Guide." [Online]. Available: https://junit.org/junit5/docs/current/user-guide/
(accessed Sep. 26, 2026).

[12] T. S. M. Tse and Y.-T. Poon, "Modeling no-shows, cancellations, overbooking, and walk-ins in restaurant
revenue management," *Journal of Foodservice Business Research*, vol. 20, no. 2, pp. 127–145, 2017,
doi: 10.1080/15378020.2016.1198626.

---

## Appendix A — Use case specifications

Each use case follows the structure recommended by Cockburn [7]: actors, precondition, main flow, alternative
flows numbered by the step they branch from, and postcondition.

### UC-01: Log in and log out
- **Actors:** Waiter/Host, Kitchen Staff, Manager
- **Precondition:** The user has a seeded account.
- **Main flow:** 1. The user opens the application and sees the login screen. 2. The user enters a username and
  password. 3. The system checks the password against the stored hash. 4. The system opens the home screen for
  the user's role: floor overview (Waiter/Host, Manager) or kitchen display (Kitchen Staff). 5. When the user clicks
  Log out, the session ends and the login screen is shown.
- **Alternative flows:** 3a. Wrong username or password: the system shows "Invalid username or password"
  without saying which one was wrong. 2a. A field is empty: the system shows a validation message.
- **Postcondition:** The user is logged in with the functions of their role only.

### UC-02: Manage tables and sections
- **Actor:** Manager
- **Precondition:** The Manager is logged in.
- **Main flow:** 1. The Manager opens Table management. 2. The Manager adds or edits a table: number, capacity,
  section. 3. The system validates the input and saves it. 4. The Manager can deactivate a table.
- **Alternative flows:** 3a. The table number already exists, or the capacity is outside 1–20: the system shows an
  error and does not save. 4a. The table has future reservations: the system refuses to deactivate it and shows
  how many reservations exist.
- **Postcondition:** The table list is updated and used by availability searches.

### UC-03: Create reservation
- **Actor:** Waiter/Host
- **Precondition:** The Waiter/Host is logged in; at least one active table exists.
- **Main flow:** 1. The Waiter/Host opens the Reservation screen. 2. Enters date, start time and party size and
  clicks Search. 3. The system validates the input and lists the tables that fit the party and have no overlapping
  reservation (BR-01, BR-02). 4. The Waiter/Host selects a table. 5. Enters guest name, phone number and an optional
  note, and clicks Confirm. 6. The system re-checks the slot, saves the reservation and shows its code.
- **Alternative flows:** 3a. Invalid input (date in the past, party size ≤ 0 or larger than any table): a message is
  shown next to the field. 3b. No table available: the system offers another time or the waiting list (UC-05).
  6a. The slot was taken meanwhile by another user: the reservation is rejected with a conflict message and the
  list is refreshed.
- **Postcondition:** The reservation is Confirmed and the slot is no longer available.

### UC-04: Modify, cancel or check in a reservation
- **Actor:** Waiter/Host
- **Precondition:** A Confirmed reservation exists.
- **Main flow:** 1. The Waiter/Host opens the reservation list for a date and selects a reservation.
  2a. **Modify:** changes the time, party size or table; the system re-applies BR-01 and BR-02 and saves.
  2b. **Cancel:** confirms; the reservation becomes Cancelled and the slot is released.
  2c. **Check in:** the guests have arrived; the table becomes Occupied (BR-05).
- **Alternative flows:** 2a-1. The change would overlap another reservation: it is rejected and the original is
  kept. 2x. The start time has passed: modify and cancel are not allowed. 2c-1. The table is still Needs Cleaning:
  check-in is refused until it is marked clean. 2c-2. More than 15 minutes late: the reservation is already
  No-show; the system offers to seat the party as a walk-in (UC-05).
- **Postcondition:** The reservation and table status are updated.

### UC-05: Manage walk-in waiting list
- **Actor:** Waiter/Host
- **Precondition:** The Waiter/Host is logged in.
- **Main flow:** 1. A walk-in party arrives and no suitable table is free. 2. The Waiter/Host enters name, party
  size and phone number, and clicks Add to waiting list. 3. The system saves the entry with the arrival time and
  shows its position. 4. When a table becomes Available, the system highlights the earliest waiting party that fits
  (BR-04). 5. The Waiter/Host clicks Seat; the table becomes Occupied and the party leaves the list.
- **Alternative flows:** 2a. Missing name or party size ≤ 0: validation message. 4a. No waiting party fits: nothing
  is highlighted. 5a. The party left: the Waiter/Host removes the entry and later parties move up. 5b. The table is
  Needs Cleaning or has a reservation starting too soon (BR-01): seating is refused with the reason.
- **Postcondition:** The party is waiting or seated.

### UC-06: Update table status
- **Actor:** Waiter/Host
- **Precondition:** The Waiter/Host is logged in and sees the floor overview.
- **Main flow:** 1. The Waiter/Host selects an Occupied table whose guests have left and clicks Needs cleaning.
  2. After cleaning, the Waiter/Host selects the table and clicks Mark clean. 3. The table becomes Available and,
  if a waiting party fits, it is highlighted (UC-05).
- **Alternative flows:** 1a. The table still has an open order or unpaid bill: the system refuses and asks to
  close the bill first (BR-05). 1b. A transition not allowed by BR-05 is never offered.
- **Postcondition:** The table status follows the lifecycle.

### UC-07: Manage menu
- **Actor:** Manager
- **Precondition:** The Manager is logged in.
- **Main flow:** 1. The Manager opens Menu management. 2. Adds or edits an item: name, category, course, price,
  description. 3. The system validates and saves it. 4. The Manager can hide an item.
- **Alternative flows:** 3a. Empty name or price ≤ 0: a validation message. 4a. The Manager tries to delete an item
  used in past orders: the system refuses and offers to hide it instead, so history stays correct.
- **Postcondition:** The menu shown on the order screen is updated.

### UC-08: Create order and send to kitchen
- **Actor:** Waiter/Host (secondary: Kitchen Staff)
- **Precondition:** The table is Occupied; the Waiter/Host is logged in.
- **Main flow:** 1. The Waiter/Host selects the table. 2. The system shows the menu by category, without
  out-of-stock items. 3. The Waiter/Host adds items with quantities and notes. 4. The Waiter/Host clicks Send.
  5. The system creates a kitchen ticket, respecting course order (BR-06). 6. The ticket appears on the kitchen
  display. 7. The system confirms that the order was sent.
- **Alternative flows:** 3a. An item runs out while it is being added: it is removed with a message before
  sending (BR-07). 4a. Saving fails: the order stays unsent, the Waiter/Host is told, and retrying creates no
  duplicate ticket (NFR-07). 7a. The guests order more: new items are sent as a new ticket; items already In
  Progress cannot be changed (BR-08). 7b. The bill has already been generated: the order is locked (BR-09).
- **Postcondition:** The order is saved and its ticket is on the kitchen display in course order.

### UC-09: Process kitchen ticket
- **Actor:** Kitchen Staff (secondary: Waiter/Host)
- **Precondition:** Kitchen Staff is logged in; tickets are waiting.
- **Main flow:** 1. Kitchen Staff sees tickets sorted by course and time sent. 2. Selects an item and sets it to In
  Progress; the item is now locked for the waiter (BR-08). 3. When the dish is done, sets it to Ready. 4. The
  Waiter/Host sees that the item is ready.
- **Alternative flows:** 3a. A dish is sent back: Kitchen Staff sets it back to In Progress with a reason; the
  status history is kept.
- **Postcondition:** Item statuses are updated on the kitchen display and the order screen.

### UC-10: Mark menu item out of stock or back in stock
- **Actors:** Kitchen Staff, Manager
- **Precondition:** The user is logged in.
- **Main flow:** 1. The user selects a menu item and clicks Out of stock. 2. The system saves the change and
  within 5 seconds removes the item from every open order screen (NFR-03). 3. Later, the user clicks Back in stock
  and the item reappears.
- **Alternative flows:** 2a. The item is already in sent tickets: those tickets are not affected (BR-07).
  2b. A waiter tries to send the item at the same moment: that line is rejected with a message.
- **Postcondition:** Only in-stock items can be ordered.

### UC-11: Open and close shift
- **Actor:** Manager
- **Precondition:** The Manager is logged in.
- **Main flow:** 1. At the start of service, the Manager clicks Open shift; the system records the start time.
  2. At the end of service, the Manager clicks Close shift. 3. The system checks that no bill is unpaid, records
  the end time and offers the shift report (UC-15).
- **Alternative flows:** 1a. A shift is already open: opening is refused (BR-13). 3a. Unpaid bills exist: closing is
  refused and the unpaid bills are listed.
- **Postcondition:** Exactly one shift is open during service, or none after closing.

### UC-12: Generate bill
- **Actor:** Waiter/Host
- **Precondition:** The table is Occupied and has an order; a shift is open.
- **Main flow:** 1. The Waiter/Host selects the table and clicks Generate bill. 2. The system checks that no item is
  New or In Progress (BR-09). 3. The system copies the order lines into bill lines, calculates the total and locks
  the order. 4. The bill is displayed with total, paid and remaining amounts.
- **Alternative flows:** 2a. Items are still being prepared: the system refuses and lists them. 1a. The table has
  no order: the system refuses. 4a. The guests want to order more: the Waiter/Host cancels the unpaid bill, which is
  allowed only if no payment has been recorded, and the order is unlocked.
- **Postcondition:** An open bill exists for the table.

### UC-13: Split bill
- **Actor:** Waiter/Host
- **Precondition:** The bill is open and has no payments yet.
- **Main flow:** 1. The Waiter/Host opens the bill and clicks Split. 2a. **Evenly:** enters the number of shares;
  the system calculates the shares with BR-10. 2b. **By item:** assigns each bill line to a share. 3. The system
  shows each share's amount; the shares always add up to the bill total.
- **Alternative flows:** 2a-1. The number of shares is outside 2–20: a validation message. 2b-1. A line is
  unassigned or assigned twice: the split cannot be confirmed. 1a. A payment was already recorded: splitting is
  refused.
- **Postcondition:** The bill is divided into shares that can be paid separately.

### UC-14: Record payment and close bill
- **Actor:** Waiter/Host
- **Precondition:** The bill (or share) is open; a shift is open.
- **Main flow:** 1. The Waiter/Host opens the bill or share. 2. Selects the method (cash, card, bank transfer) and
  enters the amount. 3. The system validates it (BR-11) and, for cash, shows the change. 4. The system records the
  payment. 5. When payments cover the total, the system closes the bill in the same transaction, makes it
  read-only (BR-12) and sets the table to Needs Cleaning (BR-05).
- **Alternative flows:** 3a. Amount ≤ 0, or a card or transfer amount larger than the remaining amount: an error
  message. 5a. The amount is less than the remaining amount: the payment is recorded and the bill stays open with
  the new remaining amount. 1a. The bill is already closed: no payment can be added. 4a. The database save fails:
  nothing is recorded and the user can retry (NFR-08). 1b. No shift is open: payment is refused (BR-13).
- **Postcondition:** Payments are stored; a fully paid bill is closed.

### UC-15: Generate shift report
- **Actor:** Manager
- **Precondition:** The Manager is logged in; at least one shift exists.
- **Main flow:** 1. The Manager opens Shift reports. 2. Selects a shift. 3. The system collects the shift's bills
  and payments. 4. The system shows the number of closed bills, total revenue, revenue per payment method and
  number of split bills.
- **Alternative flows:** 4a. The shift has no bills: the report shows zero revenue. 2a. The selected shift is still
  open: the report is labelled provisional.
- **Postcondition:** The Manager sees the figures for the shift.

---

## Appendix B — Declaration of AI-tool use

| Tool | What it was used for | Which part of the work | How the output was verified |
|---|---|---|---|
| Claude (Anthropic), via Claude Code | Analysing the course documents; proposing the ten-week timeline and task split | Planning (Week 1) | **[TODO — team]** |
| Claude (Anthropic), via Claude Code | Drafting requirements, business rules and use cases for the Table & Reservation module | Chapter 2, Appendix A (UC-01 to UC-06) | **[TODO — Giang]** |
| Claude (Anthropic), via Claude Code | Reviewing the three module drafts against the Guidelines; translating the Order & Kitchen draft into English; proposing measurable NFRs and business rules for Billing | Chapter 2, Appendix A | **[TODO — Lam, Khoa]** |
| Claude (Anthropic), via Claude Code | Assembling this proposal draft from the module drafts | Chapters 1–4 | **[TODO — team]** |
| Claude (Anthropic), via Claude Code | Searching for a published source on restaurant no-shows and walk-ins | §1.1, reference [12] | Title, authors, journal, volume, pages and DOI checked against the published PDF of the article |

We declare that we understand every part of the submitted work and can explain it on request.

**[TODO]** Fill in the last column honestly, for example "each member re-read every requirement of their
module, corrected X and Y, and confirmed the rules with restaurant staff". Add any other AI tool a member used.

---

## Before submitting — checklist (Guidelines §12.1)

- [ ] All chapters copied into `Template_ProjectProposal.docx`, chapter names unchanged
- [ ] Cover page: course code and name, class group, topic, members with IDs and roles, lecturer, submission date
- [ ] Non-plagiarism declaration signed (typed names) by all three members
- [ ] Table of contents generated
- [ ] Every **[TODO]** in this draft resolved
- [ ] Team meeting has confirmed: 15 use cases, the cross-module rules BR-05, BR-08 and BR-09, and the work plan
- [ ] Figures and tables numbered and captioned; references in IEEE style, cited in the text
- [ ] 8–15 pages without cover and appendices
- [ ] README updated; lecturer has access to the repository
- [ ] PDF and DOCX named `StudentID_FullName_ApplicationProgrammingProject`, opened again after upload
