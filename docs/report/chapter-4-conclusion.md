# Chapter 4: Conclusion — draft

Draft by Kieu Bao Giang, 03/11/2026. **[Brackets]** = Lam or Khoa fill in, or update at v1.0.

## 4.1 Summary

**Summary of completed work, compared with the Proposal**

| Proposal item | Completed | Not completed / changed |
|---|---|---|
| Login and three roles (FR-01) | Yes: session login, BCrypt, role checks on the server | — |
| Table & Reservation, FR-02 to FR-10, UC-01 to UC-06, BR-01 to BR-05 | All FRs, all five rules, each with automated tests | UC-06 "highlight a fitting party" is replaced by a "Seat next waiting" button (see 3.3.3) |
| Order & Kitchen, FR-11 to FR-20, BR-06 to BR-08 | [Lam] | [Lam] |
| Billing & Reporting, FR-21 to FR-29, BR-09 to BR-13 | [Khoa] | [Khoa] |
| 30 documented test cases | 31 for Table & Reservation (TC-RES-01 to TC-RES-31), [+ Order & Kitchen + Billing] | — |
| Release tags v0.1, v0.2, v1.0 | [dates] | — |

**Achievements and outcomes**
- A working web application: Spring Boot, React and MySQL, with a demo profile that needs no MySQL.
- The interview changed two of our rules (CR-01, CR-02) and three of Khoa's (CR-03 to CR-05). The code follows the restaurant's real timings.
- Double booking is prevented even when two staff book at the same second. This is proven by an automated test.
- [Lam: kitchen display, course order …] [Khoa: split bill, shift report …]

**Limitations and future work**
- **Security on the internet:** rate-limit the login, use HTTPS, and require new passwords instead of the README demo passwords.
- **Live updates:** refresh the floor overview and the kitchen display by themselves, with WebSocket or polling.
- **Ideas from the interview:** online booking with a deposit to reduce no-shows, and pre-ordering dishes with the booking (F2).
- **Reminders:** SMS or Zalo reminders to late guests at 30 minutes (F5).
- [Order / Billing future work]

## 4.2 Knowledge and Skills Gained

| Member | Technical skills | Professional skills | Practical experience |
|---|---|---|---|
| Kieu Bao Giang | Layered Spring Boot design; JPA mapping and Flyway migrations; Spring Security sessions and roles; pessimistic locking for concurrency; JUnit and MockMvc tests, including a multi-threaded test; React pages calling a REST API | Leading a team of three: splitting modules, reviewing pull requests, keeping `main` buildable; turning interview notes into requirement changes | Interviewing restaurant staff and learning that real timings (3-hour tables, 45-minute grace) differ from assumptions |
| Tang Thoai Lam | [ ] | [ ] | [ ] |
| Nguyen Nam Khoa | [ ] | [ ] | [ ] |
