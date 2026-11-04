# Final Project Report — Tang Thoai Lam's part

Draft by Tang Thoai Lam, 04/11/2026. English, as the course requires.

## About this file

This file holds the parts of the Final Project Report that Tang Thoai Lam writes:
- Chapter 2 (for the whole team);
- the Order & Kitchen sections of Chapter 3;
- Lam's rows for Chapter 4 and for Appendices B and C.

Giang copies these parts into `Template_Final-Project-Report.docx`, in the same way as `giang.md`.

Text in `[brackets]` is still open. It waits for pull requests to be merged, for Khoa's results, or for v1.0.

## Chapter 2: Theoretical Background and Requirement Analysis

[Check the chapter title against the template.]

### 2.1 Technologies and Architecture

#### 2.1.1 Layered architecture
The backend is split into layers. Each layer talks only to the layer below it.

| Layer | Package | Job | Example |
|---|---|---|---|
| Presentation | `controller`, `dto` | Receives HTTP requests, checks the role and the input format, returns JSON | `OrderController`, `OrderLineRequest` |
| Business | `service`, `domain` | Runs the use case inside a transaction and applies the business rules | `OrderService`, `CustomerOrder.sendToKitchen` |
| Data access | `repository` | Reads and writes the database | `CustomerOrderRepository` |
| Cross-cutting | `exception`, `config` | Turns every error into one JSON shape; seeds sample data | `GlobalExceptionHandler`, `OrderSeeder` |

The business rules live in the domain classes, not in the controllers or in React.
A rule such as BR-06 is written once, in `CustomerOrder`, and a unit test can check it without a database or a browser.
The React client only shows what the server allows, and the server checks every rule again.

#### 2.1.2 Spring Boot, JPA and Flyway
- **Spring Boot** starts the web server and wires the classes together (dependency injection). A service gets its repositories through its constructor, so a test can give it other ones.
- **Spring Data JPA with Hibernate** maps each domain class to a table. A repository is an interface; Spring writes the SQL for methods such as `findByTableIdAndStatusIn`.
- **Flyway** owns the database schema. Each change is a numbered SQL file (`V1` Giang, `V2` and `V3` Lam, `V4` Khoa). Hibernate only maps the tables and never changes them (`ddl-auto=none`). Every member's database therefore has the same schema.

#### 2.1.3 Transactions and locking
Two problems from database theory show up in a restaurant:
- **Lost update.** Two staff change the same row at the same moment, and one change overwrites the other.
- **Partial work.** A request fails halfway, and the data is left half changed.

The project uses three tools against them:
- **Transactions.** Each service method runs in one transaction (`@Transactional`). If any step fails, nothing is saved. NFR-07 depends on this: a failed send leaves the order unsent.
- **Pessimistic locking.** `SELECT … FOR UPDATE` locks a row until the transaction ends, and a second request waits. It is used for the table row when booking (NFR-06) and for the order row when sending to the kitchen (NFR-07).
- **Optimistic locking.** A `version` column is checked when the row is saved. If someone else saved first, the save fails and the user gets 409. It is used for kitchen ticket items, where a cook and a waiter may change the same dish at once.

#### 2.1.4 Spring Security
- Login creates a server session. The browser keeps only a session cookie (`SameSite=Strict`).
- Passwords are stored as BCrypt hashes (NFR-10).
- Each endpoint names the roles that may call it, for example `@PreAuthorize(AccessRules.KITCHEN)`. A request from another role gets 403, even if someone types the URL by hand.

#### 2.1.5 React client and short polling
- The client is a single-page application in React with React Router, built with Vite. In development, Vite forwards `/api` to Spring Boot, so the browser sees one origin.
- The kitchen display and the order screen must change when another user acts (NFR-02, NFR-03). The project uses **short polling**: the page asks the server again every 2 or 3 seconds. It is simpler than WebSocket and meets both limits. The cost is one small request every few seconds per open screen.

#### 2.1.6 Testing
- **JUnit 5** unit tests check the domain rules without Spring.
- **Spring MockMvc** API tests send real HTTP requests to the full application on an in-memory **H2** database in MySQL mode. Each test runs in a transaction that is rolled back.
- **Spring Security Test** logs in as a given role for each request.

#### 2.1.7 Libraries and licences

| Library | Version | Licence |
|---|---|---|
| Spring Boot (Web, Data JPA, Security, Validation) | 3.5.16 | Apache 2.0 |
| Hibernate ORM (through Spring Data JPA) | 6.6.53 | LGPL 2.1 |
| Flyway | 11.7.2 | Apache 2.0 |
| MySQL Connector/J | 9.7.0 | GPL 2.0 with the Universal FOSS Exception |
| H2 Database | 2.3.232 | MPL 2.0 or EPL 1.0 |
| JUnit 5, Mockito (through spring-boot-starter-test) | 5.12.2; 5.17.0 | EPL 2.0; MIT |
| React | 18.3.1 | MIT |
| React Router | 6.30.6 | MIT |
| Vite | 6.4.5 | MIT |

### 2.2 The Restaurant's Current Process

The team interviewed a cashier and a waiter of a steak and pasta restaurant in Go Vap District on 18/10/2026 (`docs/interview/restaurant-interview-notes.md`). The restaurant's name is not recorded.

- **Reservations** arrive through social-media platforms. The most common problem is guests who book, sometimes pre-order dishes, and then do not come (F1, F2).
- **Tables.** A booking holds a table for up to 3 hours, and resetting a table takes 15 minutes. Late guests are held for 45 minutes; after 30 minutes the staff call them (F3 to F5).
- **Orders** are already entered in an ordering app. The app is not linked to reservations or billing (F9).
- **Out-of-stock dishes** are passed on by word of mouth: the kitchen tells the waiter, and the waiter tells the guest (F9).
- **Changes after cooking starts** are rare, and the staff have not had to deal with one (F10).
- **Bills** are rarely split at the table. At the end of a shift the cash is counted, and screenshots of bank transfers are posted to the staff chat (F11, F12).

This replaces the Proposal's description of handwritten orders and a reservation notebook.
The system fills the same gaps:
- the steps of a visit are not linked;
- out-of-stock news travels by mouth;
- nothing stops a table being promised twice.

### 2.3 Requirement Analysis

The requirements below are the Proposal's, with the changes from the interview applied (`docs/requirements/requirement-changes.md`, CR-01 to CR-05).

#### 2.3.1 Actors

| Actor | Description |
|---|---|
| Waiter/Host | Front-of-house staff: reservations, seating, waiting list, orders, bills and payments |
| Kitchen Staff | Sees tickets on the kitchen display, updates preparation status, marks dishes out of stock |
| Manager | Configures tables and the menu, opens and closes shifts, views reports, can do everything a Waiter/Host can |

#### 2.3.2 Functional requirements

Priority: Must, Should or Could. A changed requirement is marked with its change number.

| ID | Requirement | Priority | Module |
|---|---|---|---|
| FR-01 | The system shall require every user to log in with a username and password, and shall show only the screens and actions permitted for that user's role. | Must | Giang |
| FR-02 | The system shall allow a Manager to create, edit and deactivate a table, with a unique table number, a seat capacity between 1 and 20, and a section. A table that has future reservations cannot be deactivated. | Must | Giang |
| FR-03 | The system shall allow a Waiter/Host to search for available tables by date, start time and party size, returning only tables whose capacity is at least the party size and that have no overlapping reservation. | Must | Giang |
| FR-04 | The system shall allow a Waiter/Host to create a reservation with guest name, phone number, party size, date, start time and an optional note, and shall reject it if it overlaps another reservation on the same table (BR-01). | Must | Giang |
| FR-05 | The system shall allow a Waiter/Host to modify or cancel a reservation before its start time, and to check in a reservation when the guests arrive. | Must | Giang |
| FR-06 | The system shall mark a reservation as No-show when the guests have not checked in 45 minutes after the start time, and release the table. Between 30 and 45 minutes late the reservation stays Confirmed so that staff can call the guest. **(CR-01)** | Should | Giang |
| FR-07 | The system shall allow a Waiter/Host to add a walk-in party to a waiting list ordered by arrival time, and show each party's position. | Must | Giang |
| FR-08 | The system shall allow a Waiter/Host to seat the first waiting party whose size fits a table that has just become Available. | Must | Giang |
| FR-09 | The system shall enforce the table status lifecycle (BR-05); a table in Needs Cleaning cannot be assigned until it is marked clean. | Must | Giang |
| FR-10 | The system shall display a floor overview of all tables with number, section, capacity and status, colour-coded by status. | Should | Giang |
| FR-11 | The system shall allow a Waiter/Host to create an order for an Occupied table by choosing items from the menu grouped by category; out-of-stock items are not shown. | Must | Lam |
| FR-12 | The system shall allow a Waiter/Host to add items, change the quantity (1–50), remove items and add a note of up to 200 characters to each order line before it is sent to the kitchen. | Must | Lam |
| FR-13 | The system shall assign every menu item to a course (Starter, Main, Dessert) and send items to the kitchen in course order (BR-06). | Must | Lam |
| FR-14 | When a Waiter/Host sends an order, the system shall create a kitchen ticket containing the sent items and show it on the kitchen display. | Must | Lam |
| FR-15 | The system shall allow Kitchen Staff to change the status of each ticket item from New to In Progress to Ready, and shall show the status to the Waiter/Host. | Must | Lam |
| FR-16 | The system shall allow a Manager to add, edit and hide menu items, with name, category, course and a price greater than 0 VND. A menu item that appears in any past order can be hidden but not deleted. | Must | Lam |
| FR-17 | The system shall allow Kitchen Staff or a Manager to mark a menu item as out of stock or back in stock; an out-of-stock item disappears from every order screen (BR-07). | Must | Lam |
| FR-18 | The system shall refuse to modify or cancel an order item once its kitchen ticket item is In Progress (BR-08); new items may still be added and are sent as a new ticket. | Must | Lam |
| FR-19 | The system shall allow a Waiter/Host to move an open order to another table. | Could | Lam |
| FR-20 | The system shall allow a Waiter/Host to view the orders of a table in the current shift. | Could | Lam |
| FR-21 | The system shall allow a Waiter/Host to generate a bill for an Occupied table from its order, copying each order line with quantity, unit price and line total (BR-09). | Must | Khoa |
| FR-22 | The system shall display a bill with its lines, grand total, amount paid and amount remaining. | Must | Khoa |
| FR-23 | The system shall allow a Waiter/Host to split a bill evenly into 2 to 20 shares using the rounding rule BR-10. | Must | Khoa |
| FR-24 | The system shall allow a Waiter/Host to split a bill by item, assigning every bill line to exactly one share. **(CR-03: was Should)** | Could | Khoa |
| FR-25 | The system shall allow a Waiter/Host to record one or more payments (cash, card, bank transfer, e-wallet) against a bill or a share, and shall calculate the change for cash payments (BR-11). **(CR-04)** | Must | Khoa |
| FR-26 | The system shall close a bill when it is paid in full; closing makes the bill read-only (BR-12) and sets the table to Needs Cleaning (BR-05). | Must | Khoa |
| FR-27 | The system shall allow a Manager to open a shift and to close it; every payment belongs to the shift that is open when it is recorded (BR-13). | Must | Khoa |
| FR-28 | The system shall allow a Manager to generate a report for a shift showing the number of closed bills, total revenue, revenue per payment method and number of split bills. | Must | Khoa |
| FR-29 | When closing a shift, the system shall let the Manager enter the counted cash; the shift report shall show expected cash, counted cash and the difference. **(CR-05, new)** | Could | Khoa |

If time runs short, requirements are cut in this order: FR-19, FR-20, FR-24, then FR-10 and FR-06 (CR-03).

#### 2.3.3 Non-functional requirements

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

#### 2.3.4 Business rules

| ID | Rule | Owner |
|---|---|---|
| BR-01 | **No double booking.** Two bookings of the same table overlap if `startA < endB + turnTime` and `startB < endA + turnTime`, where `end = start + diningDuration`. Overlapping reservations are rejected; a walk-in cannot be seated at a table whose next reservation starts within one dining duration plus turn-time. Defaults: 180 and 15 minutes, set by the Manager **(CR-02: was 90 and 15)**. | Giang |
| BR-02 | **Capacity fit.** A party is only assigned to a table whose capacity is at least the party size. | Giang |
| BR-03 | **Clean before reuse.** A table in Needs Cleaning cannot be assigned until it is marked clean. | Giang |
| BR-04 | **Waiting-list order.** Walk-ins are seated in arrival order; a later party goes first only if no earlier party fits the free table. | Giang |
| BR-05 | **Table lifecycle.** Available → Reserved (from 30 minutes before a confirmed reservation) → Occupied (check-in, also early from Available, or walk-in seated) → Needs Cleaning (automatically when the table's bill is closed, or manually when the table has no open order) → Available (marked clean). A No-show, a cancellation or a change of time or table returns the table from Reserved to Available. No other transition is allowed. | Giang |
| BR-06 | **Course sequencing.** Within an order, items are sent in course order (Starter → Main → Dessert): an item of a later course cannot be sent while an item of an earlier course in the same order is still unsent. The kitchen display lists items by course, then by time sent. | Lam |
| BR-07 | **Out of stock.** An out-of-stock item is removed from every order screen and cannot be added or sent; items already in sent tickets are not affected. | Lam |
| BR-08 | **Lock after cooking starts.** Once a ticket item is In Progress it cannot be modified or cancelled. New items can still be added to the order and are sent as a new ticket. | Lam |
| BR-09 | **Bill only when the kitchen is done.** A bill can be generated only when no ticket item of the order is New or In Progress. Generating the bill locks the order; to add items, the unpaid bill must be cancelled first, which is allowed only while no payment has been recorded. A cancelled bill is kept with status Cancelled, so an order has at most one bill that is not cancelled. | Khoa |
| BR-10 | **Even-split rounding.** For N shares, each share is `floor(total / N / 1,000) × 1,000` VND and the remainder is added to the first share, so the shares always add up exactly to the bill total. Example: 1,000,000 VND split 3 ways gives 334,000 + 333,000 + 333,000. | Khoa |
| BR-11 | **Valid payments.** A payment amount must be greater than 0. Card, bank-transfer and e-wallet payments cannot exceed the amount remaining; a cash payment may exceed it, and the change is recorded **(CR-04)**. | Khoa |
| BR-12 | **Closed bills are final.** A bill closes only when payments cover its total. A closed bill cannot be edited, deleted or receive further payments. | Khoa |
| BR-13 | **Shift control.** Payments can be recorded only while a shift is open; only one shift can be open at a time; a shift cannot be closed while it has unpaid bills. | Khoa |

#### 2.3.5 Use cases
Full specifications are in Appendix A. The use-case diagram is `docs/diagrams/giang/01-use-case.png`.

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
| UC-11 | Open and close shift | Manager | FR-27, FR-29 | Khoa |
| UC-12 | Generate bill | Waiter/Host | FR-21, FR-22 | Khoa |
| UC-13 | Split bill | Waiter/Host | FR-23, FR-24 | Khoa |
| UC-14 | Record payment and close bill | Waiter/Host | FR-25, FR-26 | Khoa |
| UC-15 | Generate shift report | Manager | FR-28, FR-29 | Khoa |

#### 2.3.6 Changes since the Proposal

| Change | What changed | Why (interview finding) |
|---|---|---|
| CR-01 | No-show after 45 minutes instead of 15 | Late guests are held 45 minutes and called after 30 (F5) |
| CR-02 | Default dining duration 180 minutes instead of 90 | A booking holds a table for up to 3 hours (F3) |
| CR-03 | FR-24 split by item lowered to Could | Bills are rarely split at the table (F11) |
| CR-04 | E-wallet added as a payment method | The restaurant accepts all common methods (F12) |
| CR-05 | New FR-29: counted cash in the shift report | Cash is counted by hand at the end of a shift (F12) |

The Order & Kitchen requirements did not change. The interview confirmed FR-17 and BR-07 (out-of-stock news travels by mouth today) and kept BR-08, even though guests rarely change an order after cooking starts (F9, F10).

## Chapter 3 — Order & Kitchen module sections

### 3.1 Problem Description and System Objectives (module part)

**Problem statement.**
Between the table and the kitchen, three things go wrong:
- The kitchen learns about a dish in the wrong order, so mains come out before starters.
- A dish runs out, but the waiters hear about it late and keep taking orders for it.
- A guest changes a dish that is already on the stove.

The restaurant's ordering app does not know which tables are seated, and out-of-stock news travels by mouth.

**Objectives of the module.**
- Take orders only for seated tables, from a menu that hides sold-out dishes (FR-11, BR-07).
- Send dishes in course order and show them to the kitchen at once (BR-06, NFR-02).
- Never send a dish twice, even after a double click or a failed save (NFR-07).
- Lock a dish as soon as cooking starts (BR-08).
- Tell the waiter when each dish is ready (FR-15).

### 3.2 System Analysis and Design (module part)

#### 3.2.1 Functional analysis
- Use cases UC-07 to UC-10, Appendix A; use-case diagram `docs/diagrams/giang/01-use-case.png`.
- Activity diagram of the order and kitchen flow: `docs/diagrams/lam/12-activity-4-order-kitchen.png`.

#### 3.2.2 Data flow
- DFD level 1, order and kitchen process: `docs/diagrams/lam/10-dfd-1b-order-kitchen.png`.

#### 3.2.3 Database design
ERD: `docs/diagrams/lam/updated/w8-erd-order-kitchen.png` (README §3), matching Flyway `V2__order_kitchen_module.sql` and `V3__kitchen_tickets.sql`.

| Table | Purpose | Key constraints |
|---|---|---|
| `menu_category` | Groups of dishes (Appetizers, Soups, Main dishes, Desserts, Drinks) | `name` unique |
| `menu_item` | Dishes | Name unique within a category; `price` BIGINT VND, CHECK > 0; `course`; `in_stock`; `active` (hidden, never deleted) |
| `customer_order` | The order of one table (ORDER is a reserved word in SQL) | FK to `dining_table` and `users`; `status` OPEN, LOCKED or CLOSED; index (`table_id`, `status`) |
| `order_line` | One dish on an order | `quantity` CHECK 1–50; `unit_price` copied from the menu when the line is added; `note` up to 200 |
| `kitchen_ticket` | The dishes sent together | FK to `customer_order`; `sent_at`, indexed for the kitchen display |
| `kitchen_ticket_item` | One dish on a ticket, with its kitchen status | `order_line_id` **unique**, so a line can never be sent twice (NFR-07); `version` for optimistic locking |
| `item_status_change` | History of each dish | `status`, `changed_at`, `reason` (for a dish sent back) |

#### 3.2.4 Class design and state machines
Images for the report are in `docs/diagrams/lam/updated/`; the Mermaid sources are in `docs/diagrams/lam/README.md`.
- Class diagrams: `w8-class-order-menu.png` (README §1) and `w8-class-kitchen-tickets.png` (README §2).
- State machines: ticket item `08-state-3-ticket-item.png`; order `08-state-4-order.png`.
- Sequences: send to kitchen, `w8-sequence-send-to-kitchen.png` (README §4); kitchen display, `w8-sequence-kitchen-display.png` (README §5). Taking an order: `06-sequence-order-1-take-order.png`.

Design decisions:
- **BR-08 locks one dish.** The lock sits on the ticket item of each order line, so new dishes can still be added. The order-level `LOCKED` status is kept for Billing: it means "bill generated" (BR-09).
- **A ticket item reads its quantity and note from its order line.** A sent line cannot change, so there is no second copy that could disagree.
- **Every check runs before anything changes.** A refused send leaves the order exactly as it was.

```java
public KitchenTicket sendToKitchen(List<OrderLine> selection, LocalDateTime at) {
    requireOpen();
    if (selection.isEmpty()) throw new BusinessRuleException("There is nothing new to send");
    for (OrderLine line : selection) {
        requireOwnLine(line);
        if (line.isSent()) throw new BusinessRuleException("\"" + line.getMenuItem().getName() + "\" was already sent");
        if (!line.getMenuItem().isOrderable()) throw new BusinessRuleException("\"" + line.getMenuItem().getName() + "\" ran out; remove it before sending");
    }
    Course latestSent = selection.stream().map(l -> l.getMenuItem().getCourse()).max(Comparator.naturalOrder()).orElseThrow();
    unsentLines().stream()
            .filter(line -> !selection.contains(line))
            .map(line -> line.getMenuItem().getCourse())
            .filter(course -> course.compareTo(latestSent) < 0)
            .min(Comparator.naturalOrder())
            .ifPresent(earlier -> { throw new BusinessRuleException("Send the " + courseLabel(earlier)
                    + " dishes before the " + courseLabel(latestSent) + " dishes (BR-06)"); });
    KitchenTicket ticket = new KitchenTicket(this, selection, at);
    tickets.add(ticket);
    return ticket;
}
```
*Listing 3.x. BR-06, BR-07 and NFR-07 in `CustomerOrder`.*
The enum `Course` is declared in the order Starter, Main, Dessert, so comparing two courses compares their place in the meal.
If a Starter stays unsent while a Main is sent, the send is refused and nothing is saved.

```java
public void changeStatus(ItemStatus target, String reason, LocalDateTime at) {
    boolean allowed = switch (status) {
        case NEW -> target == ItemStatus.IN_PROGRESS || target == ItemStatus.CANCELLED;
        case IN_PROGRESS -> target == ItemStatus.READY;
        case READY -> target == ItemStatus.IN_PROGRESS;
        case CANCELLED -> false;
    };
    ...
}
```
*Listing 3.x. The ticket item state machine (BR-08) in `KitchenTicketItem`.*
Cancelled is reachable only from New, so a dish that is In Progress can never be cancelled.
Going back from Ready needs a reason, which is saved in `item_status_change`.

#### 3.2.5 Interface design
- Wireframes: `13-wireframe-1-order-screen.png`, `13-wireframe-2-kitchen-display.png`.
- Screens, in `docs/screenshots/lam/`:
  - Order screen (Waiter/Host): the menu by category on one side, the order with each dish's kitchen status on the other. Figure 3.x, `01-order-screen.jpg`.
  - Kitchen display (Kitchen Staff): open tickets, oldest first, with the table number and the waiting time; one button per dish for the next step. The sold-out panel (UC-10) sits under the tickets. Figure 3.x, `02-kitchen-display.jpg`.
  - Menu (Manager). Figure 3.x, `03-menu-manager.jpg`.

### 3.3 System Development and Implementation (module part)

#### 3.3.1 Technologies and tools
Same as Giang's table. Lam's module adds no library.

#### 3.3.2 Implemented functions

| Function (FR / UC) | Input | Output | Endpoint | Screen |
|---|---|---|---|---|
| Manage menu (FR-16, UC-07) | name, category, course, price 1–10,000,000 VND | Item saved; a name already in the category is refused. Items are hidden, never deleted | `GET/POST /api/menu/items`, `PUT /api/menu/items/{id}`, `/deactivate`, `/activate`, `GET/POST /api/menu/categories` | Menu |
| Out of stock / back in stock (FR-17, UC-10, BR-07) | item id | The item disappears from the order screen's menu within 3 s | `POST /api/menu/items/{id}/out-of-stock`, `/restock` | Kitchen display, sold-out panel |
| Open an order (FR-11, UC-08) | table id | Order OPEN. Refused if the table is not Occupied or already has an open order | `POST /api/orders`, `GET /api/orders?tableId=` | Order screen |
| Edit lines (FR-12) | dish, quantity 1–50, note up to 200 | Line added with the menu price copied; quantity and note changed; line removed | `POST /api/orders/{id}/lines`, `PUT` and `DELETE /api/orders/{id}/lines/{lineId}` | Order screen |
| Send to kitchen (FR-13, FR-14, BR-06, NFR-07) | order id, optionally the line ids | One ticket in course order; refused as a whole if a dish ran out or an earlier course is left unsent | `POST /api/orders/{id}/send` | Order screen |
| Cancel a sent dish (FR-18, BR-08) | order id, line id | Cancelled while New and removed from the total; refused once In Progress | `DELETE /api/orders/{id}/lines/{lineId}` | Order screen |
| Kitchen display (FR-14, FR-15, UC-09) | — (polls every 2 s) | Tickets with a dish still New or In Progress, oldest first, dishes by course | `GET /api/kitchen/tickets` | Kitchen display |
| Change a dish's status (FR-15, UC-09) | item id; a reason when sent back | New → In Progress → Ready; Ready → In Progress with a reason. The waiter sees the status | `POST /api/kitchen/items/{id}/start`, `/ready`, `/send-back` | Kitchen display, order screen |
| Sample data (Week 8) | — | 29 dishes and 12 finished orders from the past week, created through the domain methods | `MenuSeeder`, `OrderSeeder` | — |

**Implementation notes**
- **Double click on Send (NFR-07).**
  - `OrderService.sendToKitchen` first locks the order row (`findByIdForUpdate`, `SELECT … FOR UPDATE`).
  - The second request waits, then finds nothing left to send and gets 409 "There is nothing new to send".
  - The unique key on `kitchen_ticket_item.order_line_id` stops a duplicate even if the code had a bug.
- **Cook and waiter at the same moment (BR-08).** A waiter cancelling a dish and a cook starting it both save the same ticket item. The `version` column lets only the first save win. The second user gets 409 "Someone else changed this dish a moment ago".
- **Link to the other modules.**
  - Opening an order checks Giang's table status (Occupied). The order module never changes a table's status itself.
  - `CustomerOrder.hasKitchenWorkPending()` is there for Khoa's BR-09 check before a bill is generated.
  - `lock()` marks the order when the bill is generated.
- **Errors.** Every refusal is a `BusinessRuleException` (409) or a field error (400) with a message the waiter can act on, for example "Send the Starter dishes before the Main dishes (BR-06)".

#### 3.3.3 Unimplemented or incomplete functions

| Item | State | Reason |
|---|---|---|
| FR-19 Move an order to another table | Not built | Could requirement, first in the cut order (CR-03) |
| FR-20 Orders of a table in the shift | Not built | Could requirement; the order screen shows only the current order |
| NFR-02 and NFR-03 with live updates | By polling | The pages ask again every 2 s (kitchen) and 3 s (menu). WebSocket would remove the polling requests |
| NFR-05: a 5-item order in 60 seconds | [Measure with a stopwatch at v1.0] | Each dish is one click, and Send is one more click |
| "Sent back" reason | Simple prompt | Uses the browser's prompt box instead of a form |
| Unpaid sample orders | Closed without a bill | The sample orders are closed directly because Billing is not merged. [Remove this row when `OrderSeeder` creates bills, or tell Khoa] |
| Pre-ordering dishes with a booking (interview F2) | Out of scope | Future work |

#### 3.3.4 Other deliverables
- Flyway migrations `V2` and `V3`.
- Wireframes, deployment diagram and the module's UML diagrams in `docs/diagrams/lam/`, with the diagrams updated to the code in `updated/`.
- Test cases `tests/test-cases-order-kitchen.md`.

### 3.4 Testing and Evaluation (module part)

#### 3.4.1 Testing process
- **Methods.**
  - Unit tests of the domain rules without Spring: `CustomerOrderTest`, `MenuItemTest`, `KitchenTicketTest`.
  - API tests with MockMvc on H2 with the real seed data: `MenuControllerTest`, `OrderControllerTest`, `KitchenControllerTest`.
  - A persistence test of the order mapping: `OrderPersistenceTest`.
  - A test that the sample data follows the rules: `OrderSeederTest`.
  - The screens were walked through in the browser with the H2 profile: open an order, add dishes, change a quantity and a note, send, then Start and Ready on the kitchen display.
- **Test cases.** TC-MENU-01 to 09, TC-ORD-01 to 13 and TC-KIT-01 to 13 (35 cases) in `tests/test-cases-order-kitchen.md`, Appendix [x].
- **Results** on 05/11/2026:
  - `mvn test`: 119 automated tests, 0 failures. 65 of them are for Order & Kitchen. Run on `main` with all Order & Kitchen pull requests merged (#13, #14, #17, #19). [Update when Billing is merged.]
  - All 35 documented cases pass.

#### 3.4.2 System evaluation
**Strengths**
- A dish is never sent twice. Two mechanisms prevent it: a row lock and a unique key (TC-KIT-02).
- A refused send changes nothing (TC-KIT-04).
- BR-08 locks one dish, not the whole order, so guests can still order more (TC-KIT-08, TC-KIT-12).
- Every status change of a dish is kept, including the reason when a dish is sent back.
- The sample data is created through the same domain methods, so it cannot break the rules.

**Limitations**
- Polling sends a request every 2 or 3 seconds from each open screen.
- The React screens have no automated tests; they were checked by hand.
- NFR-02 and NFR-03 follow from the polling intervals but were not timed. NFR-05 was not measured.
- The kitchen display has no sound or flash for a new ticket.

#### 3.4.3 Comparison between theory and practice
- **Theory → practice.**
  - The ticket item state diagram became one `switch`; a transition the diagram does not have cannot happen.
  - "Lost update" appeared in two forms. A double click on Send is solved with a pessimistic lock. A cook and a waiter acting at once is solved with an optimistic lock, because a conflict there is rare.
  - For atomicity, the domain method checks everything before it changes anything. In the usual refusal the transaction then has nothing to roll back.
- **Differences.**
  - The Week 5 code locked the whole order when cooking started. Re-reading BR-08 and FR-18 for Week 7 showed that this would stop guests from ordering more. The lock moved to the ticket item, and the order's `LOCKED` status was kept for the bill (BR-09).
  - The interview showed that guests rarely change an order after cooking starts (F10). The rule was kept because it protects the kitchen in the cases that do happen.

## Chapter 4 rows (for Giang to merge)

**4.1 Summary, Order & Kitchen row**

| Proposal item | Completed | Not completed / changed |
|---|---|---|
| Order & Kitchen, FR-11 to FR-20, BR-06 to BR-08 | FR-11 to FR-18, UC-07 to UC-10, BR-06 to BR-08, each rule with unit and API tests; order screen and kitchen display; sample data | FR-19 and FR-20 (Could) not built. BR-08 locks one dish instead of the whole order |

**Achievements (Lam):** the kitchen display shows dishes in course order within the 2-second polling interval, and a dish is never sent twice, even after a double click.

**Future work (Order & Kitchen):**
- WebSocket instead of polling.
- A sound when a new ticket arrives.
- Move an order to another table (FR-19).
- Pre-ordering dishes with a booking (F2).

**4.2 Knowledge and skills gained**

| Member | Technical skills | Professional skills | Practical experience |
|---|---|---|---|
| Tang Thoai Lam | [Lam: write in your own words. For example: JPA relations between order, line, ticket and history; pessimistic and optimistic locking; turning a state diagram into code; MockMvc tests; React screens that poll the server] | [Lam: for example working with branches and pull requests, recording test results] | [Lam: for example what the restaurant interview changed in your view of the kitchen] |

## Appendices B and C (Lam's rows)

### Appendix B — Declaration of AI-tool use

[Lam: check every row. The "How the output was verified" column must describe what you yourself did.]

| Tool | What it was used for | Which part of the work | How the output was verified |
|---|---|---|---|
| Claude (Anthropic), via Claude Code | Generating the Order & Kitchen code: menu, order and kitchen domain classes, services, controllers, Flyway V2 and V3, sample data, and the menu, order and kitchen screens | `src/backend` and `src/frontend`, Order & Kitchen part (Weeks 5–8) | The automated tests were run on Lam's machine (119 passing on 04/11/2026, 65 for this module). The screens were walked through in the browser with the H2 profile. [Lam: name the classes you read line by line and what you changed] The commits are from Lam's own account, through pull requests merged by another member |
| Claude (Anthropic), via Claude Code | Writing the automated tests and the documented test cases TC-MENU, TC-ORD and TC-KIT | `src/backend/src/test`, `tests/test-cases-order-kitchen.md` | [Lam: e.g. compared each expected result with its requirement and business rule; checked the "actual" column against the test output] |
| Claude (Anthropic), via Claude Code | Translating Lam's Vietnamese requirement draft; drafting the wireframes, the deployment diagram, the progress notes and this report's Chapter 2, Chapter 3 (Order & Kitchen part) and Lam's rows | `docs/requirements/`, `docs/diagrams/lam/`, `docs/progress/lam-progress.md`, `docs/report/lam.md` | [Lam: e.g. compared Chapter 2 with the interview notes and the requirement changes; checked each statement of Chapter 3 against the code] |

**Before signing, Lam checks being able to explain these parts of the code in an online walkthrough:**
- How `sendToKitchen` enforces BR-06, with an example: a Starter unsent and a Main sent.
- Why a double click on Send creates one ticket: the order row lock and the unique key.
- The `switch` in `KitchenTicketItem.changeStatus`, and why a dish In Progress cannot be cancelled.
- The difference between the pessimistic lock on the order and the optimistic lock (`version`) on the ticket item.
- Why `unit_price` is copied into `order_line` instead of read from the menu.
- How the kitchen display meets NFR-02 with polling.

### Appendix C — Individual contribution table

| Member | Main responsibilities | Main artefacts produced | Commits | Share of work (%) | Signature |
|---|---|---|---|---|---|
| Tang Thoai Lam | Order & Kitchen module: menu, orders, course sequencing, kitchen display, out-of-stock; report Chapter 2 | Requirement draft; `docs/diagrams/lam/`; Flyway V2 and V3; Order & Kitchen backend and frontend; 65 automated tests and 35 test cases; sample data | [ ] | [ ] | |
