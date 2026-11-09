# Order & Kitchen diagrams (Tang Thoai Lam)

The PNG files in this folder are the Week 4 design. The diagrams below match the code as of 04/11/2026; their images are in `updated/`.
The ticket item and order state machines (`08-state-3-ticket-item.png`, `08-state-4-order.png`) still match the code and were not redrawn.

## 1. Class diagram, menu and order

Image: `updated/w8-class-order-menu.png`

```mermaid
classDiagram
    direction LR
    class MenuCategory {
        -Long id
        -String name
    }
    class MenuItem {
        -Long id
        -String name
        -String description
        -long price
        -Course course
        -boolean inStock
        -boolean active
        +updateDetails(category, name, description, price, course) void
        +markOutOfStock() void
        +restock() void
        +deactivate() void
        +activate() void
        +isOrderable() boolean
    }
    class Course {
        <<enumeration>>
        STARTER
        MAIN
        DESSERT
    }
    class Order {
        -Long id
        -OrderStatus status
        -LocalDateTime createdAt
        +addLine(item, quantity, note) OrderLine
        +changeLine(line, quantity, note) void
        +removeLine(line) void
        +cancelLine(line, at) void
        +sendToKitchen(lines, at) KitchenTicket
        +unsentLines() List~OrderLine~
        +hasKitchenWorkPending() boolean
        +total() long
        +lock() void
        +close() void
    }
    class OrderStatus {
        <<enumeration>>
        OPEN
        LOCKED
        CLOSED
    }
    class OrderLine {
        -Long id
        -int quantity
        -long unitPrice
        -String note
        +isSent() boolean
        +isCancelled() boolean
        +lineTotal() long
    }
    class DiningTable {
        <<Table and Reservation>>
    }
    class User {
        <<Shared>>
    }
    class KitchenTicket {
        <<Kitchen>>
    }
    MenuCategory "1" --> "*" MenuItem : groups
    MenuItem ..> Course
    Order "1" *-- "*" OrderLine : lines
    OrderLine "*" --> "1" MenuItem
    Order ..> OrderStatus
    Order "*" --> "1" DiningTable : for
    Order "*" --> "1" User : taken by
    Order "1" *-- "*" KitchenTicket : tickets
```

## 2. Class diagram, kitchen tickets

Image: `updated/w8-class-kitchen-tickets.png`

```mermaid
classDiagram
    direction LR
    class Order {
        <<Order and Kitchen>>
        +sendToKitchen(lines, at) KitchenTicket
        +cancelLine(line, at) void
    }
    class KitchenTicket {
        -Long id
        -LocalDateTime sentAt
    }
    class KitchenTicketItem {
        -Long id
        -ItemStatus status
        -long version
        +changeStatus(ItemStatus target, String reason, LocalDateTime at) void
        +isPending() boolean
    }
    class ItemStatusChange {
        -Long id
        -ItemStatus status
        -LocalDateTime changedAt
        -String reason
    }
    class ItemStatus {
        <<enumeration>>
        NEW
        IN_PROGRESS
        READY
        CANCELLED
    }
    class OrderLine {
        <<Order and Kitchen>>
    }
    Order "1" *-- "*" KitchenTicket : tickets
    KitchenTicket "1" *-- "1..*" KitchenTicketItem : items in course order
    KitchenTicketItem "1" --> "1" OrderLine : prepares
    KitchenTicketItem "1" *-- "*" ItemStatusChange : history
    KitchenTicketItem ..> ItemStatus
```

## 3. ERD, Flyway V2 and V3

Image: `updated/w8-erd-order-kitchen.png`

```mermaid
erDiagram
    menu_category ||--o{ menu_item : groups
    menu_item ||--o{ order_line : "ordered as"
    dining_table ||--o{ customer_order : for
    users ||--o{ customer_order : "taken by"
    customer_order ||--|{ order_line : contains
    customer_order ||--o{ kitchen_ticket : "sent as"
    kitchen_ticket ||--|{ kitchen_ticket_item : contains
    order_line ||--o| kitchen_ticket_item : "prepared as"
    kitchen_ticket_item ||--o{ item_status_change : history
    menu_category {
        bigint id PK
        varchar name UK
    }
    menu_item {
        bigint id PK
        bigint category_id FK
        varchar name "UK with category_id"
        varchar description
        bigint price "CHECK > 0, VND"
        varchar course
        boolean in_stock
        boolean active
    }
    customer_order {
        bigint id PK
        bigint table_id FK
        bigint waiter_id FK
        varchar status "OPEN, LOCKED, CLOSED"
        datetime created_at
    }
    order_line {
        bigint id PK
        bigint order_id FK
        bigint menu_item_id FK
        int quantity "CHECK 1-50"
        bigint unit_price
        varchar note "max 200"
    }
    kitchen_ticket {
        bigint id PK
        bigint order_id FK
        datetime sent_at
    }
    kitchen_ticket_item {
        bigint id PK
        bigint ticket_id FK
        bigint order_line_id "FK, UK"
        varchar status
        bigint version
    }
    item_status_change {
        bigint id PK
        bigint ticket_item_id FK
        varchar status
        datetime changed_at
        varchar reason
    }
    dining_table {
        bigint id PK
    }
    users {
        bigint id PK
    }
```

## 4. Sequence, send to kitchen (UC-08 steps 4-7)

Image: `updated/w8-sequence-send-to-kitchen.png`

```mermaid
sequenceDiagram
    autonumber
    actor W as Waiter/Host
    participant P as OrderPage
    participant C as OrderController
    participant S as OrderService
    participant O as CustomerOrder
    participant R as Repositories
    W->>P: Send to kitchen
    P->>C: POST /api/orders/{id}/send (optional lineIds)
    C->>S: sendToKitchen(id, lineIds)
    Note over S,R: one transaction (NFR-07)
    S->>R: findByIdForUpdate(id), SELECT ... FOR UPDATE
    Note over S,R: a second click waits here until the first send commits
    S->>O: sendToKitchen(selection, now)
    alt order LOCKED (BR-09), nothing unsent, dish out of stock (BR-07) or earlier course unsent (BR-06)
        O-->>S: BusinessRuleException, nothing changed
        S-->>C: exception, transaction rolled back
        C-->>P: 409 with the message
        P-->>W: show the message
    else all checks pass
        O->>O: new KitchenTicket, items sorted Starter, Main, Dessert, all NEW
        O-->>S: ticket
        S->>R: flush (order_line_id is unique on kitchen_ticket_item)
        S-->>C: OrderResponse
        C-->>P: 200, lines with kitchenStatus NEW
        P-->>W: Sent to the kitchen
    end
```

## 5. Sequence, kitchen display (UC-09)

Image: `updated/w8-sequence-kitchen-display.png`

```mermaid
sequenceDiagram
    autonumber
    actor K as Kitchen Staff
    participant D as KitchenPage
    participant C as KitchenController
    participant S as KitchenService
    participant I as KitchenTicketItem
    participant R as Repositories
    loop every 2 seconds (NFR-02)
        D->>C: GET /api/kitchen/tickets
        C->>S: listActiveTickets()
        S->>R: tickets with an item NEW or IN_PROGRESS, oldest first
        S-->>D: tickets, items in course order
    end
    K->>D: Start (or Ready, or Sent back with a reason)
    D->>C: POST /api/kitchen/items/{id}/start
    C->>S: start(id)
    S->>R: findById(id)
    S->>I: changeStatus(IN_PROGRESS, null, now)
    alt move not in the state diagram, or sent back without a reason
        I-->>S: BusinessRuleException
        S-->>D: 409 with the message
    else allowed
        I->>I: new status, add ItemStatusChange
        S->>R: flush, version checked
        alt a waiter cancelled the dish a moment earlier
            R-->>S: OptimisticLockingFailureException
            S-->>D: 409, someone else changed this dish
        else saved
            S-->>D: 200, item IN_PROGRESS
            Note over D: the dish is now locked for the waiter (BR-08)
        end
    end
```

## 6. Sequence, take an order (UC-08 steps 1-3)

Image: `updated/w8-sequence-take-order.png`

```mermaid
sequenceDiagram
    autonumber
    actor W as Waiter/Host
    participant P as OrderPage
    participant M as MenuController
    participant C as OrderController
    participant S as OrderService
    participant R as Repositories
    W->>P: Order, on an Occupied table of the floor overview
    loop every 3 seconds (NFR-03)
        P->>M: GET /api/menu/items?availableOnly=true
        M-->>P: in-stock dishes, grouped by category on the page
        P->>C: GET /api/orders?tableId=
        C-->>P: the unpaid order of the table, or none
    end
    opt the table has no order yet
        W->>P: Start order
        P->>C: POST /api/orders
        C->>S: createOrder(tableId, username)
        S->>R: findByIdForUpdate(tableId)
        alt table not Occupied, or it already has an open order
            S-->>P: 409 with the message
        else
            S->>R: save the order, status OPEN
            S-->>P: 201, empty order
        end
    end
    W->>P: Pick a dish
    P->>C: POST /api/orders/{id}/lines
    C->>S: addLine(orderId, request)
    S->>R: findByIdForUpdate(orderId), then the menu item
    alt dish out of stock (BR-07) or order locked (BR-09)
        S-->>P: 409 with the message
    else
        S->>R: flush the new line, price copied from the menu
        S-->>P: 201, updated order
    end
```
