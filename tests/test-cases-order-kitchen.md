# Test cases — Order & Kitchen module (Tang Thoai Lam)

Menu management (Week 5–6) and order taking (Week 7). Columns follow Course Guidelines §6.3, same as
`test-cases-table-reservation.md`. Kitchen ticket test cases are added with the kitchen display.

Every row is also an automated test (column **Automated test**) in `src/backend/src/test`, run with
`mvn test` on an in-memory H2 database seeded with the standard accounts and a sample menu.
**Actual result** and **Pass/Fail** come from the `mvn test` run on the author's machine on 4/11/2026 (92 tests, 0 failures).

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-MENU-01 | Manager adds a menu item (UC-08, FR-08) | Logged in as Manager; category exists | name "Grilled squid", price 120000, course MAIN | 201; item is in stock and active | As expected | Pass | MenuControllerTest.createItem |
| TC-MENU-02 | Price must be positive (NFR-11) | Logged in as Manager | price 0 | 400; message "Price must be between 1 and 10,000,000 VND" next to the price field | As expected | Pass | MenuControllerTest.priceMustBePositive |
| TC-MENU-03 | Blank name is rejected (NFR-11) | Logged in as Manager | name "   " | 400; message "Item name is required" next to the name field | As expected | Pass | MenuControllerTest.nameIsRequired |
| TC-MENU-04 | Duplicate name in the same category is rejected | Item "Fresh spring rolls" exists in Appetizers | same name in capitals, same category | 400; message on the name field | As expected | Pass | MenuControllerTest.duplicateNameInCategory |
| TC-MENU-05 | Only a Manager can change the menu (NFR-10) | Logged in as Waiter/Host | POST /api/menu/items | 403 | As expected | Pass | MenuControllerTest.waiterCannotCreateItem |
| TC-MENU-06 | Sold-out item leaves the order screens (BR-07) | Logged in as Kitchen Staff; item in stock | POST /api/menu/items/{id}/out-of-stock, then the waiter lists items with availableOnly=true | Item no longer in the waiter's list; still in the Manager's list | As expected | Pass | MenuControllerTest.outOfStockItemIsHiddenFromOrderScreens |
| TC-MENU-07 | Restocked item returns to the order screens (BR-07) | Item is out of stock | POST /api/menu/items/{id}/restock | Item is in the waiter's list again | As expected | Pass | MenuControllerTest.restockedItemReturns |
| TC-MENU-08 | Order line copies the price and rejects an out-of-stock item (BR-07) | Unit test, no database | add a line, change the menu price, then add an out-of-stock item | Line keeps the old price; out-of-stock item refused | As expected | Pass | CustomerOrderTest.addLineCopiesThePriceFromTheMenu, outOfStockItemCannotBeAdded |
| TC-MENU-09 | Locked order cannot be changed (BR-08) | Unit test, order locked | add a line, remove a line | Both refused; lines unchanged | As expected | Pass | CustomerOrderTest.lockedOrderCannotBeChanged |

## Orders (UC-08, FR-11, FR-12)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-ORD-01 | Waiter/Host opens an order for a table (FR-11) | Logged in as Waiter/Host; table is Occupied | POST /api/orders with the table id | 201; order is OPEN, has no lines, total 0, waiter is the logged-in user | As expected | Pass | OrderControllerTest.createOrder |
| TC-ORD-02 | Order needs seated guests (FR-11, BR-05) | Table is Available | POST /api/orders | 409; "Table N is not occupied; seat the guests before taking an order" | As expected | Pass | OrderControllerTest.tableMustBeOccupied |
| TC-ORD-03 | One open order per table | Table already has an open order | POST /api/orders again | 409; no second order | As expected | Pass | OrderControllerTest.onlyOneOpenOrderPerTable |
| TC-ORD-04 | Only front-of-house staff take orders (NFR-10) | Logged in as Kitchen Staff | POST /api/orders | 403 | As expected | Pass | OrderControllerTest.kitchenCannotCreateOrder |
| TC-ORD-05 | Add a dish (FR-12) | Open order; item in stock | menuItemId, quantity 2, note "  no onion  " | 201; line has the menu price, note "no onion", total = price × 2 | As expected | Pass | OrderControllerTest.addLine |
| TC-ORD-06 | Quantity limits (FR-12, NFR-11) | Open order | quantity 0, then 51 | 400; "Quantity must be between 1 and 50" next to the quantity field | As expected | Pass | OrderControllerTest.quantityLimits |
| TC-ORD-07 | Note limit (FR-12, NFR-11) | Open order | note of 201 characters | 400; "Note must be at most 200 characters" next to the note field | As expected | Pass | OrderControllerTest.noteLimit |
| TC-ORD-08 | Out-of-stock dish refused (BR-07) | Item marked out of stock | add the item | 409; "\"<name>\" is not available right now" | As expected | Pass | OrderControllerTest.outOfStockItemRefused |
| TC-ORD-09 | Change quantity and note (FR-12) | Order has one line, quantity 1 | PUT quantity 3, note "less spicy" | 200; line updated, total = price × 3 | As expected | Pass | OrderControllerTest.changeLine |
| TC-ORD-10 | Remove a line (FR-12) | Order has one line | DELETE the line | 200; no lines, total 0 | As expected | Pass | OrderControllerTest.removeLine |
| TC-ORD-11 | Locked order cannot change (BR-08) | Order is LOCKED | add a line, remove a line | 409 for both | As expected | Pass | OrderControllerTest.lockedOrderRefused |
| TC-ORD-12 | Unknown order or foreign line | Two orders exist | GET order 999999; DELETE a line of the other order | 404 for both | As expected | Pass | OrderControllerTest.unknownOrderOrLine |
| TC-ORD-13 | Order screen finds the table's order | Table has an open order | GET /api/orders?tableId= | 200; one order with that id | As expected | Pass | OrderControllerTest.currentOrderOfTable |
