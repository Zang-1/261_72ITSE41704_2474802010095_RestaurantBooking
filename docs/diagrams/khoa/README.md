# Billing & Reporting diagrams (Nguyen Nam Khoa)

The PNG files in this folder are the Week 4 design. The diagrams below match the code as of 21/11/2026;
their images are in `updated/`. They are drawn from the classes in `src/backend/.../domain`, the Flyway script
`V4__billing_reporting.sql` and the services `BillingService` and `ShiftService`.

What changed since Week 4:
- **BillShare** has a `number` (1..N) so the waiter can say "share 2"; **BillLine** keeps a link to its share
  for a split by item (FR-24).
- **Bill** has `splitType` (NONE, EVEN, BY_ITEM), `closedAt` and the **Shift** in which it was closed, so the shift
  report counts each bill once (FR-28).
- **Payment** records who took it (`recordedBy`); `appliedAmount() = amount - changeAmount` is what pays the bill.
- **Shift** records who opened and closed it and the counted cash (FR-29, CR-05). `open_marker` is unique, so the
  database refuses a second open shift (BR-13).
- **PaymentMethod** has E_WALLET (CR-04).
- **ShiftReport** is not stored: it is calculated on request (`ShiftReportResponse`).

## 1. Class diagram

Image: `updated/w9-class-billing.png`

```mermaid
classDiagram
    direction LR
    class Order {
        <<Order & Kitchen>>
        -OrderStatus status
        +lock() void
        +unlock() void
        +close() void
        +hasKitchenWorkPending() boolean
    }
    class DiningTable {
        <<Table & Reservation>>
        +changeStatus(TableStatus) void
    }
    class Bill {
        -Long id
        -long total
        -BillStatus status
        -SplitType splitType
        -LocalDateTime createdAt
        -LocalDateTime closedAt
        +generate(Order, LocalDateTime)$ Bill
        +evenShares(long total, int count)$ long[]
        +splitEvenly(int count) void
        +splitByItem(List~List~BillLine~~) void
        +removeSplit() void
        +pay(share, method, amount, shift, user, at) Payment
        +cancel() void
        +paidAmount() long
        +remaining() long
    }
    class BillLine {
        -Long id
        -String itemName
        -int quantity
        -long unitPrice
        +lineTotal() long
    }
    class BillShare {
        -Long id
        -int number
        -long amount
        +paidAmount() long
        +remaining() long
    }
    class Payment {
        -Long id
        -PaymentMethod method
        -long amount
        -long changeAmount
        -LocalDateTime paidAt
        +appliedAmount() long
    }
    class Shift {
        -Long id
        -LocalDateTime openedAt
        -LocalDateTime closedAt
        -Long countedCash
        +close(User, LocalDateTime, Long) void
        +isOpen() boolean
    }
    class User
    class BillStatus {
        <<enumeration>>
        OPEN
        CLOSED
        CANCELLED
    }
    class SplitType {
        <<enumeration>>
        NONE
        EVEN
        BY_ITEM
    }
    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        BANK_TRANSFER
        E_WALLET
    }
    class ShiftReportResponse {
        <<calculated>>
        int closedBills
        long revenue
        Map revenueByMethod
        int splitBills
        long expectedCash
        Long countedCash
        Long cashDifference
        boolean provisional
    }
    Bill "*" --> "1" Order : generated from
    Order "*" --> "1" DiningTable : at
    Bill "1" *-- "1..*" BillLine
    Bill "1" *-- "0..20" BillShare
    Bill "1" *-- "*" Payment
    BillLine "*" --> "0..1" BillShare : assigned to
    Payment "*" --> "0..1" BillShare : pays
    Payment "*" --> "1" Shift : recorded in
    Payment "*" --> "1" User : recorded by
    Bill "*" --> "0..1" Shift : closed in
    Shift "*" --> "1" User : opened by
    Bill ..> BillStatus
    Bill ..> SplitType
    Payment ..> PaymentMethod
    ShiftReportResponse ..> Shift : calculated from
```

## 2. ERD

Image: `updated/w9-erd-billing.png`

```mermaid
erDiagram
    customer_order ||--o{ bill : "billed as"
    users ||--o{ shift : "opens / closes"
    shift |o--o{ bill : "closed in"
    bill ||--|{ bill_line : contains
    bill ||--o{ bill_share : "split into"
    bill_share |o--o{ bill_line : assigned
    bill ||--o{ payment : "paid by"
    bill_share |o--o{ payment : "paid by"
    shift ||--o{ payment : records
    users ||--o{ payment : "recorded by"

    shift {
        bigint id PK
        datetime opened_at
        datetime closed_at "null while open"
        bigint opened_by_id FK
        bigint closed_by_id FK
        bigint counted_cash "FR-29, nullable"
        tinyint open_marker UK "1 while open, null after (BR-13)"
    }
    bill {
        bigint id PK
        bigint order_id FK
        bigint total "CHECK > 0"
        varchar status "OPEN, CLOSED, CANCELLED"
        varchar split_type "NONE, EVEN, BY_ITEM"
        datetime created_at
        datetime closed_at
        bigint shift_id FK "set when closed"
    }
    bill_share {
        bigint id PK
        bigint bill_id FK
        int number "UK with bill_id"
        bigint amount "CHECK > 0"
    }
    bill_line {
        bigint id PK
        bigint bill_id FK
        bigint share_id FK "null unless split by item"
        varchar item_name
        int quantity
        bigint unit_price
    }
    payment {
        bigint id PK
        bigint bill_id FK
        bigint share_id FK "null for the whole bill"
        bigint shift_id FK
        varchar method "CASH, CARD, BANK_TRANSFER, E_WALLET"
        bigint amount "handed over, CHECK > 0"
        bigint change_amount "cash only"
        datetime paid_at
        bigint recorded_by_id FK
    }
```

## 3. Bill state machine

Image: `updated/w9-state-bill.png`

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Open : generate bill (FR-21)<br/>kitchen done (BR-09)
    Open --> Closed : payments cover the total (FR-26)
    Open --> Cancelled : cancel, no payment yet (BR-09)
    Closed --> [*]
    Cancelled --> [*]
    note right of Open
        Order is LOCKED.
        Split evenly or by item before the first payment.
        A partial payment keeps the bill Open (UC-14 5a).
    end note
    note right of Closed
        Read-only (BR-12).
        Order CLOSED, table Needs cleaning (BR-05).
    end note
    note right of Cancelled
        Kept for the record; order OPEN again.
    end note
```

## 4. Shift state machine

Image: `updated/w9-state-shift.png`

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Open : Manager opens<br/>no other shift open (BR-13)
    Open --> Closed : Manager closes<br/>no unpaid bill (BR-13)
    Closed --> [*]
    note right of Open
        Every payment is recorded in this shift.
        Closing is refused while a bill is unpaid (UC-11 3a).
    end note
    note right of Closed
        Counted cash is optional (FR-29).
        The report is final (FR-28).
    end note
```

## 5. Sequence: generate and split the bill (UC-12, UC-13)

Image: `updated/w9-sequence-generate-split.png`

```mermaid
sequenceDiagram
    actor W as Waiter/Host
    participant UI as BillPage (React)
    participant C as BillController
    participant S as BillingService
    participant DB as Repositories / MySQL
    participant B as Bill
    W->>UI: Generate bill
    UI->>C: POST /api/bills {orderId}
    C->>S: generate(orderId)
    S->>DB: lock open shift (SELECT … FOR UPDATE)
    alt no shift open
        S-->>UI: 409 "No shift is open" (BR-13)
    end
    S->>DB: lock order row
    S->>B: Bill.generate(order, now)
    alt a dish is New or In Progress, or unsent
        B-->>UI: 409 with the dish names (BR-09)
    end
    B->>B: copy charged lines, total, order.lock()
    S->>DB: save bill and lines
    S-->>UI: 201 BillResponse (total, paid 0, remaining)
    W->>UI: Split evenly, 3 shares
    UI->>C: POST /api/bills/{id}/split/even {shares: 3}
    C->>S: splitEvenly(id, 3)
    S->>DB: lock bill row
    S->>B: splitEvenly(3)
    B->>B: evenShares(total, 3) (BR-10)<br/>floor to 1,000, remainder on share 1
    S-->>UI: 200 BillResponse with 3 shares
```

## 6. Sequence: record payment and close the bill (UC-14)

Image: `updated/w9-sequence-pay-close.png`

```mermaid
sequenceDiagram
    actor W as Waiter/Host
    participant UI as BillPage (React)
    participant C as BillController
    participant S as BillingService
    participant DB as Repositories / MySQL
    participant B as Bill
    participant T as DiningTable
    W->>UI: choose share and method, enter amount
    UI->>C: POST /api/bills/{id}/payments {method, amount, shareId}
    C->>S: pay(id, request, user)
    Note over S,DB: one transaction (NFR-08)
    S->>DB: lock open shift, then bill, then table
    S->>B: pay(share, method, amount, shift, user, now)
    alt bill closed (BR-12) or no shift (BR-13)
        B-->>UI: 409 message
    else amount ≤ 0, or card / transfer / e-wallet above what is due (BR-11)
        B-->>UI: 400 message next to the amount field
    end
    B->>B: change = cash ? max(0, amount − due) : 0
    B->>B: add Payment
    opt remaining() = 0
        B->>B: status CLOSED, closedAt, shift
        B->>B: order.close()
        B->>T: changeStatus(NEEDS_CLEANING) (BR-05)
    end
    S->>DB: flush (all or nothing)
    S-->>UI: 201 BillResponse
    UI-->>W: "Paid in full … table N needs cleaning" and the change
```
