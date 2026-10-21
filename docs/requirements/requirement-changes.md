# Requirement changes after the Project Proposal

The Project Proposal (Week 3) is kept unchanged as submitted. This file lists every change made since then,
using the Proposal's numbering (FR-xx, BR-xx, UC-xx). The Final Project Report uses the requirements
**as changed here**.

Source of the changes: restaurant interview of 18/10/2026 (`docs/interview/restaurant-interview-notes.md`,
findings F1–F12).

## Changed requirements

| Change | Requirement | Before (Proposal) | After | Owner | Reason |
|---|---|---|---|---|---|
| CR-01 | FR-06, BR-05, UC-04 alt. 4b | No-show 15 minutes after the start time | The system shall mark a reservation as No-show when the guests have not checked in **45 minutes** after the start time, and release the table. Between 30 and 45 minutes late the reservation stays Confirmed so that staff can call the guest. | Giang | F5 |
| CR-02 | BR-01, UC-02 step 5 | Default dining duration 90 minutes | Default dining duration **180 minutes**; turn-time stays 15 minutes; both remain editable by the Manager (30–300 and 0–60 minutes). | Giang | F3, F4 |
| CR-03 | FR-24 | Priority Should | Priority **Could**. Cut order if time runs short: FR-19, FR-20, FR-24, then FR-10 and FR-06. FR-23 (even split, BR-10) stays Must. | Khoa | F11 |
| CR-04 | FR-25, BR-11, UC-14 | Payment methods: cash, card, bank transfer | Payment methods: cash, card, bank transfer, **e-wallet**. An e-wallet payment follows the card and transfer rule of BR-11: it cannot exceed the amount remaining. | Khoa | F12 |
| CR-05 | New FR-29 (UC-11, UC-15) | — | (Could) When closing a shift, the system shall let the Manager enter the counted cash; the shift report shall show expected cash, counted cash and the difference. | Khoa | F12 |

## Confirmed without change
- BR-01 turn-time 15 minutes (F4); BR-05 Reserved from 30 minutes before the start time and early check-in (F6).
- BR-01 walk-in restriction, BR-04 waiting-list order and FR-07 waiting list (F7). The BR-04 exception
  (a later party that fits a small free table goes first) is kept; the restaurant did not comment on it.
- FR-17 and BR-07 out-of-stock (F9); BR-08 lock after cooking starts (F10); FR-06 no-show handling itself (F1).

## Not requirements, but affected
- **Seed data:** 4 sections (Sweet Zone, Love to Love, VIP, Mây rooftop) and 27 tables (F8). Seats per table
  are a team assumption because they were not recorded in the interview.
- **Final Report, Chapter 2 (current situation):** reservations arrive through social-media platforms; the
  restaurant already uses an ordering app, but it is not linked to reservations or billing; out-of-stock dishes
  are passed on by word of mouth; end-of-shift reconciliation is manual (counting the till, screenshots of
  transfers). The Proposal's description of handwritten orders and notebook bookings is replaced by this (F1, F9, F12).
- **Out of scope, kept for future work:** pre-ordering dishes with a reservation, and deposits against no-shows (F2).
