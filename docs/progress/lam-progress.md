# Progress — Tang Thoai Lam (Order & Kitchen module)

Plan: Proposal Table 2.10. Evidence: commits and pull requests in this repository.

| Week | Dates | Planned (Lam) | Status | Evidence |
|---|---|---|---|---|
| 2 | 21–27/9 | Order, kitchen and menu requirements (FR, NFR, BR, use cases) | Done | `docs/requirements/` (22/9) |
| 4 | 5–11/10 | Class, sequence, ERD, state, DFD, activity diagrams and wireframes of the module | Diagrams done; wireframes and deployment diagram added 26/10 | `docs/diagrams/lam/` |
| 5 | 12–18/10 | Menu and order classes, repositories, menu CRUD | Done 26/10 (late) | Branch `feature/order-menu-crud`: `V2__order_kitchen_module.sql`, `MenuController`, `MenuPage.jsx` |
| 6 | 19–25/10 | Input validation, test cases, cross review | Menu validation and 9 test cases written; review of Giang's PR #9 pending | `tests/test-cases-order-kitchen.md` |
| 7 | 26/10–1/11 | BR-06 to BR-08 with unit tests | Order domain rules BR-07, BR-08 have unit tests; order and kitchen API next | — |

## Week 5 work (26/10/2026)

- Flyway migration `V2__order_kitchen_module.sql`: `menu_category`, `menu_item`, `customer_order`, `order_line`.
  The kitchen ticket tables follow in a later migration.
- Domain: `MenuCategory`, `MenuItem`, `CustomerOrder` (the class diagram's Order; ORDER is reserved in SQL),
  `OrderLine`, enums `Course` and `OrderStatus`.
- Menu API under `/api/menu`: list and add categories, list, add, edit, remove and add back items,
  mark an item out of stock or restock it (Kitchen Staff and Manager).
- Manager screen `/menu`, sample menu seed (`MenuSeeder`).
- AI use: drafted with Claude and then reviewed and run by the author (declared in Final Report, Appendix B).
