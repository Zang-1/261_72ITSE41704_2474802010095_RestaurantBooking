# Table & Reservation diagrams — updated for Week 8

The PNG files in this folder were drawn in Week 4, before the restaurant interview and before the Week 7 code.
The diagrams below replace them where the two differ. They match the code on branch `feature/giang-week7-10`.
GitHub renders Mermaid. PNG exports for the report are in `updated/` (`w8-*.png`, made with mermaid-cli at 2× scale).
Export them again after any change to a diagram below.

What changed since the Week 4 PNGs:
- Dining duration is 180 minutes, not 90 (CR-02). The no-show limit is 45 minutes (CR-01).
- New domain methods:
  - `DiningTable.changeStatus`
  - `Reservation.checkIn`, `markNoShow`, `overlap`
  - `WaitlistEntry.seat`
- New endpoints for check-in, walk-in, seat-next, needs-cleaning and clean.

## 1. Class diagram (domain layer)

```mermaid
classDiagram
    direction LR
    class TableSection {
        -Long id
        -String name
    }
    class DiningTable {
        -Long id
        -int number
        -int capacity
        -TableStatus status
        -boolean active
        +updateDetails(number, capacity, section)
        +canSeat(partySize) boolean
        +changeStatus(target)
        +deactivate()
        +activate()
    }
    class Reservation {
        -Long id
        -String code
        -String guestName
        -String phone
        -int partySize
        -LocalDateTime startTime
        -String note
        -ReservationStatus status
        +modify(table, guestName, phone, partySize, startTime, note, now)
        +cancel(now)
        +checkIn(now)
        +isNoShow(now) boolean
        +markNoShow(now)
        +endTime(settings) LocalDateTime
        +overlaps(other, settings) boolean
        +overlap(startA, startB, settings)$ boolean
    }
    class WaitlistEntry {
        -Long id
        -String partyName
        -String phone
        -int partySize
        -LocalDateTime arrivedAt
        +isWaiting() boolean
        +seat(table)
    }
    class ReservationSettings {
        -Long id
        -int diningMinutes
        -int turnMinutes
        +update(diningMinutes, turnMinutes)
    }
    class User {
        -Long id
        -String username
        -String passwordHash
        -Role role
    }
    class TableStatus {
        <<enumeration>>
        AVAILABLE
        RESERVED
        OCCUPIED
        NEEDS_CLEANING
    }
    class ReservationStatus {
        <<enumeration>>
        CONFIRMED
        CHECKED_IN
        CANCELLED
        NO_SHOW
    }
    class Role {
        <<enumeration>>
        WAITER_HOST
        KITCHEN_STAFF
        MANAGER
    }
    TableSection "1" <-- "*" DiningTable : section
    DiningTable "1" <-- "*" Reservation : table
    DiningTable "0..1" <-- "*" WaitlistEntry : seatedTable
    DiningTable --> TableStatus
    Reservation --> ReservationStatus
    Reservation ..> ReservationSettings : uses for BR-01
    User --> Role
```

## 2. ERD (Flyway `V1__table_reservation_module.sql`)

```mermaid
erDiagram
    TABLE_SECTION ||--o{ DINING_TABLE : contains
    DINING_TABLE ||--o{ RESERVATION : "is booked by"
    DINING_TABLE |o--o{ WAITLIST_ENTRY : "seats"
    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar role
    }
    TABLE_SECTION {
        bigint id PK
        varchar name UK
    }
    DINING_TABLE {
        bigint id PK
        int number UK
        int capacity "CHECK 1..20"
        bigint section_id FK
        varchar status
        boolean active
    }
    RESERVATION {
        bigint id PK
        varchar code UK
        bigint table_id FK
        varchar guest_name
        varchar phone
        int party_size
        datetime start_time "index with table_id"
        varchar note
        varchar status
    }
    WAITLIST_ENTRY {
        bigint id PK
        varchar party_name
        varchar phone
        int party_size
        datetime arrived_at
        bigint seated_table_id FK "null while waiting"
    }
    RESERVATION_SETTINGS {
        bigint id PK "always 1"
        int dining_minutes "default 180"
        int turn_minutes "default 15"
    }
```

`users` also links to Lam's `customer_order.waiter_id` (V2).

## 3. Table state machine (BR-05)

```mermaid
stateDiagram-v2
    [*] --> Available
    Available --> Reserved : booking starts within 30 min (scheduler or create)
    Available --> Occupied : walk-in / seat next waiting party
    Reserved --> Occupied : check-in (from 30 min before to 45 min after the start)
    Reserved --> Available : booking cancelled, moved or no-show
    Occupied --> NeedsCleaning : guests left (refused while an order is open)
    NeedsCleaning --> Available : mark clean
```

Every other change is refused with "Table N cannot go from X to Y" (`DiningTable.changeStatus`).

## 4. Reservation state machine

```mermaid
stateDiagram-v2
    [*] --> Confirmed : create (BR-01, BR-02 pass)
    Confirmed --> Confirmed : modify before start
    Confirmed --> Cancelled : cancel before start
    Confirmed --> CheckedIn : check-in
    Confirmed --> NoShow : 45 min after start, not checked in (scheduler)
    Cancelled --> [*]
    CheckedIn --> [*]
    NoShow --> [*]
```

## 5. Sequence — check-in (UC-04 step 4)

```mermaid
sequenceDiagram
    actor Host as Waiter/Host
    participant UI as ReservationsPage (React)
    participant C as ReservationController
    participant S as ReservationService
    participant TR as DiningTableRepository
    participant R as Reservation
    participant T as DiningTable
    Host->>UI: press "Check in"
    UI->>C: POST /api/reservations/{id}/check-in
    C->>S: checkIn(id)
    S->>TR: findByIdForUpdate(tableId)
    Note over TR: SELECT ... FOR UPDATE (NFR-06)
    TR-->>S: table (locked)
    alt table NEEDS_CLEANING or OCCUPIED
        S-->>C: BusinessRuleException
        C-->>UI: 409 { message }
    else
        S->>R: checkIn(now)
        Note over R: refused before start-30 min<br/>or from start+45 min
        S->>T: changeStatus(OCCUPIED)
        S-->>C: ReservationResponse
        C-->>UI: 200 status CHECKED_IN
    end
    UI-->>Host: "Nguyen Thi Lan checked in at table 3."
```

## 6. Sequence — two bookings at the same moment (NFR-06)

```mermaid
sequenceDiagram
    participant A as Request A
    participant B as Request B
    participant S as ReservationService
    participant DB as MySQL
    A->>S: create(table 5, 19:00)
    B->>S: create(table 5, 19:00)
    S->>DB: A: SELECT table 5 FOR UPDATE
    S->>DB: B: SELECT table 5 FOR UPDATE (waits)
    S->>DB: A: no overlap → INSERT reservation, COMMIT
    DB-->>S: B: lock granted
    S->>DB: B: overlap found (A's booking)
    S-->>B: 409 "Table 5 is already booked at 19:00, choose another time or table"
```
