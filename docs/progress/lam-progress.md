# Progress — Tang Thoai Lam (Order & Kitchen module)

Plan: Proposal Table 2.10. Evidence: commits and pull requests in this repository.

| Week | Dates | Planned (Lam) | Status | Evidence |
|---|---|---|---|---|
| 2 | 21–27/9 | Order, kitchen and menu requirements (FR, NFR, BR, use cases) | Done | `docs/requirements/` (22/9) |
| 4 | 5–11/10 | Class, sequence, ERD, state, DFD, activity diagrams and wireframes of the module | Diagrams done; wireframes and deployment diagram added 26/10 | `docs/diagrams/lam/`, PR #11 |
| 5 | 12–18/10 | Menu and order classes, repositories, menu CRUD | Done 26/10 (late) | PR #10: `V2__order_kitchen_module.sql`, `MenuController`, `MenuPage.jsx` |
| 6 | 19–25/10 | Input validation, test cases, cross review | Menu validation and 9 test cases done. Giang's PR #9 was merged on 21/10 before Lam reviewed it, so the cross review was not done | `tests/test-cases-order-kitchen.md` |
| 7 | 26/10–1/11 | BR-06 to BR-08 with unit tests | Done 4/11 (late): order API, kitchen tickets, BR-06 to BR-08 with unit and API tests | PR #13 (order API), PR #14 (kitchen tickets, `V3__kitchen_tickets.sql`) |
| 8 | 2–8/11 | Exception handling; seed data with 50–200 realistic records; updated diagrams and screenshots | Order screen, kitchen display and sample data done 4/11; error handling for the module is in place. Diagrams and screenshots still to update | Branches `feature/kitchen-screen`, `feature/order-sample-data` [add PR numbers] |
| 9 | 9–15/11 | Feature freeze; module section of report Chapter 3 | Chapter 3 (Order & Kitchen) drafted 4/11 | `docs/report/lam.md` |
| 10 | 16–22/11 | Report Chapter 2 | Drafted 4/11 | `docs/report/lam.md` |

## Week 5 work (26/10/2026)

- Flyway migration `V2__order_kitchen_module.sql`: `menu_category`, `menu_item`, `customer_order`, `order_line`.
  The kitchen ticket tables follow in a later migration.
- Domain: `MenuCategory`, `MenuItem`, `CustomerOrder` (the class diagram's Order; ORDER is reserved in SQL),
  `OrderLine`, enums `Course` and `OrderStatus`.
- Menu API under `/api/menu`: list and add categories, list, add, edit, remove and add back items,
  mark an item out of stock or restock it (Kitchen Staff and Manager).
- Manager screen `/menu`, sample menu seed (`MenuSeeder`).
- AI use: drafted with Claude and then reviewed and run by the author (declared in Final Report, Appendix B).

## Week 7 work (done 4/11/2026)

- Order API (FR-11, FR-12): open an order for an Occupied table, then add, change and remove lines.
  One open order per table; the table row is locked while the order is created.
- Migration `V3__kitchen_tickets.sql`: `kitchen_ticket`, `kitchen_ticket_item`, `item_status_change`.
  Billing (Khoa) continues with `V4`.
- Send to kitchen (FR-13, FR-14): one ticket per send, dishes in course order. BR-06 refuses a later course
  while an earlier one is unsent. The send is one transaction with the order row locked (NFR-07).
- BR-08 per dish: a sent dish can be cancelled only while New and is locked once In Progress. The order-level
  `LOCKED` status now means "bill generated" (BR-09), as in `08-state-4-order.png`.
- Kitchen API (FR-15): active tickets, Start, Ready, Sent back with a reason; every change kept in the history.
- Tests: `OrderControllerTest` (TC-ORD-01 to 13), `KitchenControllerTest` and `KitchenTicketTest` (TC-KIT-01 to 13).

## Week 8 work (done 4/11/2026)

- Order screen `/order/:tableId`, opened from an Occupied table on the floor overview.
- Kitchen display `/kitchen`: open tickets oldest first, polling every 2 s (NFR-02), with a sold-out panel (UC-10).
- Sample data: 29 menu items and 12 finished orders from the past week (`OrderSeeder`), checked by `OrderSeederTest`.
- Results: `mvn test` 119 tests, 0 failures (65 for Order & Kitchen), on the sample-data branch, 4/11/2026.

## Still open

- Update the module's class and ERD diagrams to match the code (kitchen methods, `version` column) and take screenshots.
- FR-19 (move an order to another table) and FR-20 (orders of a table in the shift) are Could and not built.
