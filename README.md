# Restaurant Table Booking and Kitchen Order System

## Project Overview
A system to help small and medium sized restaurants manage table reservations,
walk in queues, order taking, kitchen order tracking, and billing.

## Team Members
| Name | Student ID | Role |
|---|---|---|
| Kieu Bao Giang (Team Leader) | 2474802010095 | Table & Reservation module |
| Tang Thoai Lam | 2474802010210 | Order & Kitchen module |
| Nguyen Nam Khoa | 2474802010194 | Billing & Reporting module |

## Technology Stack
- Backend: Java 17, Spring Boot 3.5 (Web, Data JPA, Security, Validation), Flyway
- Frontend: JavaScript, React 18, React Router 6, Vite 6
- Database: MySQL 8 (H2 in-memory for automated tests and the demo profile)
- Tests: JUnit 5, Spring MockMvc, Spring Security Test

## Project Structure
```
src/backend/    Spring Boot API (layered: controller -> service -> repository -> domain)
  src/main/resources/db/migration/   Flyway SQL migrations, one file per module change
src/frontend/   React single-page app (Vite)
tests/          documented test cases (automated tests are in src/backend/src/test)
docs/           requirements, proposal, diagrams, progress notes
```

## How to Build and Run

**Requirements:** JDK 17 or later, Maven 3.9 (or the Maven bundled with IntelliJ IDEA), Node.js 18 or later, MySQL 8.

> **Windows path tip:** keep the clone in a folder whose path has no Vietnamese accents or spaces
> (e.g. `C:\dev\restaurant`). With an accented path `mvn spring-boot:run` fails with
> `ClassNotFoundException`; `mvn package` followed by `java -jar` still works.

### 1. Backend (http://localhost:8080)
Create `src/backend/application-local.properties` (git-ignored) with your MySQL login:
```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```
The database `restaurant_booking` is created automatically and Flyway builds the tables on first start.
```bash
cd src/backend
mvn spring-boot:run
```
No MySQL yet? Run with the in-memory demo database (data is lost on restart):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Run the automated tests (they use H2, no MySQL needed):
```bash
mvn test
```

### 2. Frontend (http://localhost:5173)
```bash
cd src/frontend
npm install
npm run dev
```
The Vite dev server forwards `/api` to the backend, so start the backend first.

## Configuration and Sample Accounts
Accounts, 4 sections and 27 tables (the layout of the restaurant interviewed in Week 5) are seeded on the first start (`DataSeeder`). These are test accounts only.

| Username | Password | Role |
|---|---|---|
| manager | Manager@2026 | Manager |
| waiter | Waiter@2026 | Waiter/Host |
| kitchen | Kitchen@2026 | Kitchen Staff |

Seeding can be switched off with `app.seed.enabled=false`.

## Documentation
- `docs/requirements/` — requirement drafts for each module, and `requirement-changes.md` (changes since the Proposal)
- `docs/proposal/` — Project Proposal (Markdown draft, DOCX and PDF)
- `docs/diagrams/` — UML diagrams and ERDs, one folder per member
- `docs/progress/` — weekly progress notes per member
- `docs/interview/` — restaurant interview questions and notes
- `tests/` — documented test cases with expected and actual results

## Team Workflow
- `main` always builds. Nobody commits directly to `main`.
- Work on a short-lived branch named `<type>/<short-description>`, e.g. `feature/reservation-overlap-check`,
  `docs/proposal-week3`, `fix/bill-rounding`.
- Open a pull request into `main`; another team member reviews and merges it.
- Every member commits their own work from their own GitHub account, every week.
- Commit messages are imperative and describe the change, e.g. `add overlap check to ReservationService`.
- Release tags: `v0.1` (Week 5), `v0.2` (Week 7), `v1.0` (Week 9).

## Current Status
Week 6 (21/10/2026)
- Done: Project Proposal submitted (Week 3); design diagrams for all three modules (Week 4).
- Done: application skeleton (Spring Boot + React), MySQL schema via Flyway, login with three roles,
  Table & Reservation CRUD (tables, sections, reservations, waiting list) with input validation and
  16 documented test cases (`tests/test-cases-table-reservation.md`).
- Done: restaurant interview (18/10/2026); requirement changes CR-01 to CR-05 in `docs/requirements/requirement-changes.md`.
- Next: Order & Kitchen and Billing modules start on this skeleton; release tag `v0.1`;
  Week 7 business rules BR-01 to BR-13 with unit tests.
