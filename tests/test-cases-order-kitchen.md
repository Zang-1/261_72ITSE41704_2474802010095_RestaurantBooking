# Test cases — Order & Kitchen module (Tang Thoai Lam)

Week 5–6 deliverable, first part (menu management). Columns follow Course Guidelines §6.3, same as
`test-cases-table-reservation.md`. Order and kitchen test cases are added in Weeks 6–7.

Every row is also an automated test (column **Automated test**) in `src/backend/src/test`, run with
`mvn test` on an in-memory H2 database seeded with the standard accounts and a sample menu.
**Actual result** and **Pass/Fail** are filled in after the run on the author's machine.

| ID | Purpose | Precondition | Input | Expected result | Actual result | Pass/Fail | Automated test |
|---|---|---|---|---|---|---|---|
| TC-MENU-01 | Manager adds a menu item (UC-08, FR-08) | Logged in as Manager; category exists | name "Grilled squid", price 120000, course MAIN | 201; item is in stock and active | | | MenuControllerTest.createItem |
| TC-MENU-02 | Price must be positive (NFR-11) | Logged in as Manager | price 0 | 400; message "Price must be between 1 and 10,000,000 VND" next to the price field | | | MenuControllerTest.priceMustBePositive |
| TC-MENU-03 | Blank name is rejected (NFR-11) | Logged in as Manager | name "   " | 400; message "Item name is required" next to the name field | | | MenuControllerTest.nameIsRequired |
| TC-MENU-04 | Duplicate name in the same category is rejected | Item "Fresh spring rolls" exists in Appetizers | same name in capitals, same category | 400; message on the name field | | | MenuControllerTest.duplicateNameInCategory |
| TC-MENU-05 | Only a Manager can change the menu (NFR-10) | Logged in as Waiter/Host | POST /api/menu/items | 403 | | | MenuControllerTest.waiterCannotCreateItem |
| TC-MENU-06 | Sold-out item leaves the order screens (BR-07) | Logged in as Kitchen Staff; item in stock | POST /api/menu/items/{id}/out-of-stock, then the waiter lists items with availableOnly=true | Item no longer in the waiter's list; still in the Manager's list | | | MenuControllerTest.outOfStockItemIsHiddenFromOrderScreens |
| TC-MENU-07 | Restocked item returns to the order screens (BR-07) | Item is out of stock | POST /api/menu/items/{id}/restock | Item is in the waiter's list again | | | MenuControllerTest.restockedItemReturns |
| TC-MENU-08 | Order line copies the price and rejects an out-of-stock item (BR-07) | Unit test, no database | add a line, change the menu price, then add an out-of-stock item | Line keeps the old price; out-of-stock item refused | | | CustomerOrderTest.addLineCopiesThePriceFromTheMenu, outOfStockItemCannotBeAdded |
| TC-MENU-09 | Locked order cannot be changed (BR-08) | Unit test, order locked | add a line, remove a line | Both refused; lines unchanged | | | CustomerOrderTest.lockedOrderCannotBeChanged |
