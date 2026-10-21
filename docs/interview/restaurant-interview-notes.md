# Restaurant interview — notes

Planned in Proposal Section 1.4 (Week 5). Answers are summarised from the team's notes and translated
from Vietnamese; nothing was added that the interviewees did not say. Requirement changes that follow
from the answers are listed at the end.

| Field | Entry |
|---|---|
| Date and time | Sunday 18/10/2026, 17:00 |
| Restaurant | A steak and pasta restaurant on Phan Van Tri Street, Go Vap District, Ho Chi Minh City (name not recorded) |
| People interviewed | Two staff members: a cashier and a waiter |
| Interviewers | Kieu Bao Giang, Tang Thoai Lam, Nguyen Nam Khoa |
| Method | In person, at the restaurant |

## Questions and answers

### Reservations and tables (Giang)
1. How do you take reservations today (notebook, phone, app)? What goes wrong most often?
   - Answer: Reservations come in through social-media platforms. The most common problem is guests who
     reserve a table, sometimes also ordering dishes in advance, and then do not come.
2. How long does a typical meal take? Does it depend on party size? (We assumed 90 minutes.)
   - Answer: A table is used for up to 3 hours per booking. It does not depend on party size or anything else.
3. How long does it take to clear and reset a table? (We assumed 15 minutes.)
   - Answer: 15 minutes.
4. How long do you hold a table for late guests before giving it away? (We assumed 15 minutes.)
   - Answer: Guests may arrive up to 30 minutes early or late. Between 30 and 45 minutes late the table is
     still held and the staff call the guest. At 45 minutes late the booking is cancelled (treated as a
     no-show) and the table is given to other guests.
5. How early do you keep a table free before a reservation? (We assumed 30 minutes.)
   - Answer: 30 minutes, the same as the time guests may arrive early.
6. How do you handle walk-in guests when the restaurant is full? Is it strictly first come, first served?
   - Answer: Guests with a reservation have priority. Walk-ins can only take tables that are free; otherwise
     they wait or are told there is no table. Waiting walk-ins are served in arrival order.
7. How many tables and seats do you have, and are they in different areas (indoor, terrace, VIP)?
   - Answer (from memory, tables were not counted): four areas — Sweet Zone (8 tables), Love to Love
     (tables 9 to 16), VIP (5 tables) and Mây, a rooftop area (6 tables); about 27 tables in total.
     Seats per table were not recorded.

### Orders and kitchen (Lam)
8. How do orders reach the kitchen? What happens when a dish runs out?
   - Answer: Orders are entered in an ordering app. When a dish runs out, the kitchen tells the waiter,
     who tells the guest.
9. Do guests ever change an order after the kitchen has started cooking? What do you do?
   - Answer: Guests rarely change orders, so the restaurant has not met this situation.

### Billing (Khoa)
10. How often do guests split the bill? Evenly or by dish?
    - Answer: Rarely. Usually one guest pays the whole bill and the group settles between themselves later.
11. Which payment methods do you accept? How do you reconcile at the end of a shift?
    - Answer: All common methods. At the end of a shift the cash in the till is counted; for bank transfers
      a screenshot is taken and posted to the restaurant's staff chat group; cards and other methods are
      checked in a similar way.

## Findings and requirement changes

Status: changes agreed on 21/10/2026 and recorded in `docs/requirements/requirement-changes.md` (CR-01 to CR-05). Lam and Khoa confirm the rows for their modules in the pull-request review.

| # | Finding | Affects | Change (or "no change") |
|---|---|---|---|
| F1 | No-shows are the most common reservation problem (Q1) | FR-06, BR-05 (Giang) | No change — confirms the need for no-show handling |
| F2 | Guests sometimes pre-order dishes with the reservation (Q1) | Scope (Giang, Lam) | No change for now — online ordering and deposits stay out of scope; mention as future work |
| F3 | A table is used for up to 3 hours, regardless of party size (Q2) | BR-01 (Giang) | CR-02: default dining duration 90 → **180 minutes** (still editable by the Manager) |
| F4 | Table reset takes 15 minutes (Q3) | BR-01 (Giang) | No change — turn-time stays 15 minutes |
| F5 | Late guests are held until 45 minutes, with a phone reminder after 30 minutes (Q4) | FR-06, BR-05 (Giang) | CR-01: no-show threshold 15 → **45 minutes** |
| F6 | Guests may arrive up to 30 minutes early; the table is kept free 30 minutes before (Q4, Q5) | BR-05 (Giang) | No change — Reserved from 30 minutes before; early check-in already allowed |
| F7 | Reserved guests have priority; walk-ins take free tables or wait, in arrival order (Q6) | BR-01, BR-04, FR-07 (Giang) | No change. The BR-04 exception (a later party that fits a small free table goes first) was not confirmed by the restaurant |
| F8 | Four areas, about 27 tables (Q7) | Seed data (Giang) | Seed data now has 4 sections and 27 tables (was 3 sections, 16 tables); seats per table are a team assumption |
| F9 | The restaurant already uses an ordering app; out-of-stock is passed on by word of mouth (Q8) | FR-17, BR-07 (Lam); Report Chapter 2 | No FR change. Rewrite the current-situation text: orders are not handwritten here, but the app is not linked to reservations or billing |
| F10 | Guests rarely change orders after cooking starts (Q9) | BR-08 (Lam) | No change — the rule still protects the kitchen |
| F11 | Bills are rarely split at the table (Q10) | FR-23, FR-24 (Khoa) | CR-03: FR-24 (split by item) lowered to Could; FR-23 stays Must |
| F12 | All payment methods; manual end-of-shift reconciliation by till count and screenshots (Q11) | FR-25, FR-28 (Khoa) | CR-04: e-wallet added as a payment method; CR-05: new Could FR-29, counted cash against expected cash in the shift report |
