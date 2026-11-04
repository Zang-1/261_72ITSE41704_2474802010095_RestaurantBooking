# Test cases — Order & Kitchen module (Tang Thoai Lam)

Menu management (Week 5–6), order taking and kitchen tickets (Week 7–8). Columns follow Course Guidelines §6.3, same as
`test-cases-table-reservation.md`.

Every row is also an automated test (column **Automated test**) in `src/backend/src/test`, run with
`mvn test` on an in-memory H2 database seeded with the standard accounts and a sample menu.
**Actual result** and **Pass/Fail** come from the `mvn test` run on the author's machine on 5/11/2026 (124 tests, 0 failures).

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-MENU-01 | Manager adds a menu item (UC-07, FR-16) | Logged in as Manager; category exists | name "Grilled squid", price 120000, course MAIN | 201; item is in stock and active | As expected | Pass | MenuControllerTest.createItem |
| TC-MENU-02 | Price must be positive (NFR-11) | Logged in as Manager | price 0 | 400; message "Price must be between 1 and 10,000,000 VND" next to the price field | As expected | Pass | MenuControllerTest.priceMustBePositive |
| TC-MENU-03 | Blank name is rejected (NFR-11) | Logged in as Manager | name "   " | 400; message "Item name is required" next to the name field | As expected | Pass | MenuControllerTest.nameIsRequired |
| TC-MENU-04 | Duplicate name in the same category is rejected | Item "Fresh spring rolls" exists in Appetizers | same name in capitals, same category | 400; message on the name field | As expected | Pass | MenuControllerTest.duplicateNameInCategory |
| TC-MENU-05 | Only a Manager can change the menu (NFR-10) | Logged in as Waiter/Host | POST /api/menu/items | 403 | As expected | Pass | MenuControllerTest.waiterCannotCreateItem |
| TC-MENU-06 | Sold-out item leaves the order screens (BR-07) | Logged in as Kitchen Staff; item in stock | POST /api/menu/items/{id}/out-of-stock, then the waiter lists items with availableOnly=true | Item no longer in the waiter's list; still in the Manager's list | As expected | Pass | MenuControllerTest.outOfStockItemIsHiddenFromOrderScreens |
| TC-MENU-07 | Restocked item returns to the order screens (BR-07) | Item is out of stock | POST /api/menu/items/{id}/restock | Item is in the waiter's list again | As expected | Pass | MenuControllerTest.restockedItemReturns |
| TC-MENU-08 | Order line copies the price and rejects an out-of-stock item (BR-07) | Unit test, no database | add a line, change the menu price, then add an out-of-stock item | Line keeps the old price; out-of-stock item refused | As expected | Pass | CustomerOrderTest.addLineCopiesThePriceFromTheMenu, outOfStockItemCannotBeAdded |
| TC-MENU-09 | Locked order cannot be changed (BR-09) | Unit test, order locked | add a line, remove a line | Both refused; lines unchanged | As expected | Pass | CustomerOrderTest.lockedOrderCannotBeChanged |

## Orders (UC-08, FR-11, FR-12, FR-19)

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
| TC-ORD-11 | Locked order cannot change (BR-09) | Order is LOCKED | add a line, remove a line | 409 for both | As expected | Pass | OrderControllerTest.lockedOrderRefused |
| TC-ORD-12 | Unknown order or foreign line | Two orders exist | GET order 999999; DELETE a line of the other order | 404 for both | As expected | Pass | OrderControllerTest.unknownOrderOrLine |
| TC-ORD-13 | Order screen finds the table's order | Table has an open order | GET /api/orders?tableId= | 200; one order with that id | As expected | Pass | OrderControllerTest.currentOrderOfTable |
| TC-ORD-14 | Move an order to a free table (FR-19) | Order sent to the kitchen; another table Available | POST /api/orders/{id}/move, new table, party size 2 | 200; order on the new table, new table Occupied, old table Needs cleaning; the kitchen ticket shows the new table | As expected | Pass | OrderMoveControllerTest.moveOrder |
| TC-ORD-15 | New table must be free (BR-05) | New table is Occupied | move | 409 "Table N is not available"; old table still Occupied | As expected | Pass | OrderMoveControllerTest.targetMustBeAvailable |
| TC-ORD-16 | Party must fit the new table (BR-02) | New table has N seats | party size N + 1 | 400; message next to the party size; new table still Available | As expected | Pass | OrderMoveControllerTest.partyMustFit |
| TC-ORD-17 | Locked order cannot move (BR-09) | Order is LOCKED | move | 409; new table still Available | As expected | Pass | OrderMoveControllerTest.lockedOrderCannotMove |
| TC-ORD-18 | Invalid move input (NFR-11) | Open order | the same table; then no party size | 400 "The order is already at table N"; then 400 "Party size is required" | As expected | Pass | OrderMoveControllerTest.invalidInput |

## Send to kitchen and kitchen tickets (UC-08 steps 4-7, UC-09, FR-13 to FR-15, FR-18)

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-KIT-01 | Send creates one ticket in course order (FR-13, FR-14) | Open order with a Main and a Starter | POST /api/orders/{id}/send, then the kitchen opens GET /api/kitchen/tickets | Both lines New; one ticket for the table, Starter listed before Main | As expected | Pass | KitchenControllerTest.sendCreatesTicket |
| TC-KIT-02 | No duplicate ticket (NFR-07) | Order already sent | POST /send again | 409 "There is nothing new to send"; still one ticket | As expected | Pass | KitchenControllerTest.sendTwice |
| TC-KIT-03 | Course order (BR-06) | Order has an unsent Starter and Main | send only the Main (lineIds) | 409 "Send the Starter dishes before the Main dishes (BR-06)" | As expected | Pass | KitchenControllerTest.courseOrder |
| TC-KIT-04 | Send is atomic (BR-07, NFR-07) | One of two lines ran out of stock | POST /send | 409; no line sent, no ticket created | As expected | Pass | KitchenControllerTest.sendIsAtomic |
| TC-KIT-05 | Item lifecycle, waiter sees status (FR-15) | Item New | start, then ready | In Progress, then Ready; waiter's order shows READY; ticket leaves the kitchen display | As expected | Pass | KitchenControllerTest.itemLifecycle |
| TC-KIT-06 | No skipping In Progress | Item New | POST /ready | 409 | As expected | Pass | KitchenControllerTest.cannotSkipInProgress |
| TC-KIT-07 | Dish sent back needs a reason (UC-09 3a) | Item Ready | send-back with reason " ", then "Too salty" | 400 "Reason is required"; then 200, In Progress | As expected | Pass | KitchenControllerTest.sendBack |
| TC-KIT-08 | Locked once cooking starts (FR-18, BR-08) | Item In Progress | waiter DELETE and PUT the line | 409 for both | As expected | Pass | KitchenControllerTest.lockedAfterCookingStarts |
| TC-KIT-09 | Cancel before cooking | Item sent, still New | waiter DELETE the line | 200; line Cancelled, total 0 | As expected | Pass | KitchenControllerTest.cancelBeforeCooking |
| TC-KIT-10 | Roles (NFR-10) | — | kitchen sends an order; waiter starts an item; waiter opens the kitchen display | 403 for all three | As expected | Pass | KitchenControllerTest.roles |
| TC-KIT-11 | BR-06 rule, unit test | Unit test, no database | send a Main while a Starter is unsent; send the Starter first, then the Main | First refused, nothing sent; second gives two tickets | As expected | Pass | KitchenTicketTest.laterCourseCannotBeSentWhileAnEarlierCourseIsUnsent, earlierCourseCanBeSentFirst |
| TC-KIT-12 | BR-08 rule, unit test | Unit test, item In Progress | cancel the line; add a new dish and send | Cancel refused; new dish goes on a second ticket | As expected | Pass | KitchenTicketTest.itemInProgressCannotBeCancelled, newDishesAfterCookingStartsGoOnANewTicket |
| TC-KIT-13 | Item state diagram, unit test | Unit test | New → Ready; then New → In Progress → Ready → In Progress without and with a reason | Skips and a send-back without a reason are refused; history has 4 steps with the reason | As expected | Pass | KitchenTicketTest.itemLifecycleFollowsTheStateDiagram |
