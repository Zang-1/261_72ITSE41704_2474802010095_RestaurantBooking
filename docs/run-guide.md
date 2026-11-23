# Run guide

How to install, start and try the Restaurant Table Booking and Kitchen Order System, from a fresh clone.
The short version is in the README ("Run the project"). On Windows, double-clicking `start.bat` builds and starts everything on http://localhost:8080.
This guide adds checks and a walkthrough of a whole visit.

## 1. Install

| Tool | Version | Check |
|---|---|---|
| JDK | 17 or later | `java -version` |
| Maven | 3.9 (or the one bundled with IntelliJ IDEA) | `mvn -v` |
| Node.js | 18 or later | `node -v` |
| MySQL | 8 (optional: the H2 profile needs no database) | `mysql --version` |

Keep the clone in a folder whose path has no Vietnamese accents or spaces (for example `C:\dev\restaurant`).

## 2. Start the backend (http://localhost:8080)

**With MySQL.** Create `src/backend/application-local.properties` (it is git-ignored):

```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

```bash
cd src/backend
mvn spring-boot:run
```

The database `restaurant_booking` is created on first start, and Flyway runs `V1` to `V4`
(Table & Reservation, Order & Kitchen, kitchen tickets, Billing & Reporting).

**Without MySQL** (in-memory, data is lost on restart):

```bash
cd src/backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

The backend is ready when the log shows `Started RestaurantApplication` and
`Seeded 12 past orders with their bills and shifts`.

## 3. Start the frontend (http://localhost:5173)

```bash
cd src/frontend
npm install
npm run dev
```

Open http://localhost:5173 and log in. Start the backend first: Vite forwards `/api` to it.

| Username | Password | Role | Home screen |
|---|---|---|---|
| manager | Manager@2026 | Manager (can do everything) | Floor |
| waiter | Waiter@2026 | Waiter/Host | Floor |
| kitchen | Kitchen@2026 | Kitchen Staff | Kitchen |

## 4. Walkthrough: one whole visit

Use two browser windows: **manager** in one, **kitchen** in a private window.

1. **Open the shift** (manager): *Shifts* → **Open shift**. Without an open shift no bill can be generated and no payment taken (BR-13).
2. **Seat the guests**: *Floor* → an Available table → **Seat walk-in** → party size. The table turns Occupied.
   (Or book it in *Reservations* and check in.)
3. **Take the order**: **Order** on the table → pick dishes → **Send to kitchen**.
4. **Cook** (kitchen window): *Kitchen* → **Start** then **Ready** for each dish.
5. **Generate the bill**: back on the floor, **Bill** on the table → **Generate bill**.
   If a dish is still New or In Progress the bill is refused and the dishes are named (BR-09).
6. **Split** (optional): *Split bill* → **Evenly** with the number of shares, or **By item** with a share for each dish.
7. **Pay**: choose the share (if split) and the method; the amount is already filled in. For cash, type what the
   guest handed over and the change is shown. **Record payment**. When everything is paid the bill closes and the table
   turns Needs cleaning.
8. **Clean the table**: *Floor* → **Mark clean**.
9. **Close the shift** (manager): *Shifts* → type the cash counted in the till (optional) → **Close shift**.
   The report shows closed bills, revenue by method, split bills, and expected cash against counted cash.
   Closing is refused while any bill is unpaid; the message lists the tables.

The sample data already holds 12 closed shifts from the past week; choose one in *Shifts* to see its report.

## 5. Run the tests

```bash
cd src/backend
mvn test
```

The tests use H2 and need no MySQL. Expected result on 21/11/2026: `Tests run: 176, Failures: 0, Errors: 0`.
The NFR timings are printed in the log, for example `NFR-04 shift report with 300 bills: slowest of 5 calls 25 ms`.
The documented test cases are in `tests/` (one file per module).

## 6. Common problems

| Symptom | Cause and fix |
|---|---|
| `ClassNotFoundException` with `mvn spring-boot:run` on Windows | The path has accents or spaces; move the clone, or run `mvn package` then `java -jar target/restaurant-booking-0.1.0.jar` |
| `Access denied for user 'root'` | Wrong password in `application-local.properties` |
| The page shows "Cannot reach the server" | The backend is not running on port 8080 |
| Port 8080 or 5173 already in use | Stop the other program, or start the backend with `--server.port=8081` and change `vite.config.js` |
| "No shift is open" on the bill screen | Log in as manager and open a shift in *Shifts* |
| Flyway error after pulling new code with an old MySQL database | Drop the database `restaurant_booking` and start again (the sample data is recreated) |
