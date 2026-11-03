# Chapter 1: Introduction — draft

Draft by Kieu Bao Giang, 03/11/2026, for the Final Project Report (Template_Final-Project-Report.docx).
It updates Chapter 1 of the Proposal to what was actually built, as Guidelines §6.1 asks.
Text in **[brackets]** must be filled in by Lam or Khoa, or checked again at v1.0.

## 1.1 Background and Rationale

**Reasons for selecting the topic.**
Small and medium-sized restaurants often run their front of house on a notebook, a chat app and word of mouth.
The team interviewed staff of a steak and pasta restaurant in Ho Chi Minh City on 18/10/2026.
In this report the restaurant is called "Ember & Oak" to keep it anonymous.
The interview showed what this costs in practice:
- Reservations arrive through social-media messages.
- No-shows are the most common reservation problem.
- Late guests are held for up to 45 minutes.
- The restaurant already uses an ordering app, but the app is not linked to reservations or billing.
- Out-of-stock dishes are passed on by word of mouth.
- Cash and transfers are reconciled by hand at the end of a shift.

**Importance and practical significance.**
Tse and Poon [12] found that no-shows, cancellations and walk-ins make it hard for a busy restaurant to decide how many reservations to accept.
A system that keeps reservations, walk-ins and table status in one place is the first step to managing this.
The same system can pass a seated table on to ordering and billing, so that staff no longer copy information between tools.

## 1.2 Objectives of the Project

**General objective.**
Build a web application with three staff roles that lets a small or medium-sized restaurant manage, in one consistent system:
- table reservations;
- walk-in guests;
- orders and kitchen preparation;
- billing.

**Specific objectives, with status on 03/11/2026:**

| # | Objective (Proposal §1.2) | Status |
|---|---|---|
| 1 | 15 use cases end-to-end for three roles | UC-01 to UC-06 (Table & Reservation) done. [Lam: UC-07 to UC-10] [Khoa: UC-11 to UC-15] |
| 2 | 13 business rules in the service layer, each with at least one automated test | BR-01 to BR-05 done, each tested. [BR-06 to BR-08 Lam] [BR-09 to BR-13 Khoa] |
| 3 | Zero overlapping reservations, also for two bookings at the same moment | Done: row lock and automated concurrent test (TC-RES-27) |
| 4 | At least 30 documented test cases with expected and actual results | 29 for Table & Reservation (TC-RES-01 to TC-RES-29). [Order: TC-ORD-…] [Billing: …] |
| 5 | Measurable NFRs, e.g. search under 2 s | [Not measured yet. Measure the search time at v1.0 with 50 tables and 500 reservations, and enter the figure here and in 3.4.2] |

## 1.3 Scope of the Study

**Research scope.**
The reservation, seating, ordering and billing process of one restaurant.
The process was learned from the interview and from published work on restaurant reservations.

**System scope: in.**
- Login with three roles: Waiter/Host, Kitchen Staff, Manager.
- Tables and sections.
- Reservations: search, create, modify, cancel, check-in, automatic no-show.
- Walk-in waiting list.
- Table lifecycle and floor overview.
- Menu, orders, kitchen display, out-of-stock. [Lam: confirm]
- Bills, splitting, payments, shifts and the shift report. [Khoa: confirm]

**System scope: out.**
- Online booking or ordering by guests.
- Deposits and pre-ordering with a reservation. The restaurant asked for these (interview F2); they are future work.
- SMS and email.
- Real payment gateways.
- Staff account administration in the UI.
- Stock of ingredients.
- More than one branch.

**Organisational and time scope.**
- Three students.
- Ten weeks, from 14/9 to 22/11/2026.
- Each member owns one module:
  - Kieu Bao Giang: Table & Reservation, plus the shared skeleton, login and team lead.
  - Tang Thoai Lam: Order & Kitchen.
  - Nguyen Nam Khoa: Billing & Reporting.

## 1.4 Research Methodologies

- **Data collection: document review.** Course documents [1], [2], requirements guidance [4], [7] and the no-show study [12].
- **Observation and practical investigation.**
  - Semi-structured interview at the restaurant on 18/10/2026, with all three members present.
  - Two staff members answered: a cashier and a waiter.
  - The notes are kept in `docs/interview/restaurant-interview-notes.md`.
- **Data analysis.**
  - Each interview finding (F1–F12) was mapped to the requirement it affects.
  - Five requirement changes followed (CR-01 to CR-05). The main ones are:
    - dining duration 90 → 180 minutes;
    - no-show limit 15 → 45 minutes.
- **Comparative and statistical methods.** The timings of BR-01 and BR-05 were checked against the times the staff gave:
  - 3-hour table use;
  - 15-minute reset;
  - guests 30 minutes early or 45 minutes late.
- **Interviews and surveys.** See above. No survey was made.

## 1.5 Structure of the Report

- **Chapter 1** introduces the problem, the objectives and the scope.
- **Chapter 2** presents:
  - the technologies used: layered architecture, Spring Boot, JPA, React;
  - the restaurant's current process;
  - the full requirement analysis, as changed after the interview.
- **Chapter 3** describes the analysis, design, implementation and testing of each module.
- **Chapter 4** compares the result with the Proposal and lists what each member learned.
- **The appendices** hold:
  - the full test-case log;
  - the AI-use declaration (Appendix B);
  - the contribution table (Appendix C).
