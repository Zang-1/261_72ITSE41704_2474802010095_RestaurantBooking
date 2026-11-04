# Appendices B and C — draft

Draft by Kieu Bao Giang, 03/11/2026.
Lam and Khoa add their own rows. The rows below cover Giang's work from Week 5 to Week 10; the Proposal's Appendix B covers Weeks 1–3.
Every row must stay true: before the report is signed, each member checks the "How verified" column of their own rows.

## Appendix B — Declaration of AI-tool use

| Tool | What it was used for | Which part of the work | How the output was verified |
|---|---|---|---|
| Claude (Anthropic), via Claude Code | Generating the backend skeleton, the Table & Reservation code (domain, services, controllers, Flyway V1) and the React screens from the Proposal's requirements and Giang's Week 4 diagrams | `src/backend` and `src/frontend`, Table & Reservation part (Weeks 5–7) | Giang read every class of the module and ran the automated tests (115 passing on 04/11/2026, 54 of them for this module). Giang tested each screen by hand with the H2 demo profile (rows marked "UI" in the test-case table) and recalculated the BR-01 boundary by hand (19:00 + 195 min = 22:15). The commits are from Giang's own account, through pull requests merged by another member |
| Claude (Anthropic), via Claude Code | Writing the automated tests and the documented test cases TC-RES-01 to TC-RES-31 | `src/backend/src/test`, `tests/test-cases-table-reservation.md` | Giang compared each expected result with the requirement and the business rule it tests. Giang ran the tests and checked the "actual" column against the test output, and checked the UI rows in the browser |
| Claude (Anthropic), via Claude Code | Translating the interview notes and anonymising the restaurant and area names | `docs/interview`, `docs/requirements/requirement-changes.md`, seed data | The three members who attended the interview checked the English notes against what the staff said. Giang chose to replace the real names with invented ones |
| Claude (Anthropic), via Claude Code | Drafting the Week 7 self-check, the Week 8 security check, the updated Mermaid diagrams and this report's Chapter 1, Chapter 3 (module part), Chapter 4 and these appendices | `docs/self-check-week7.md`, `docs/security-check.md`, `docs/diagrams/giang/README.md`, `docs/report/` | Giang checked every statement against the code and the test results; diagrams were compared with the classes and the Flyway script. Lam and Khoa reviewed the team-wide parts |
| Claude (Anthropic), via Claude Code; Figma AI image generation | Building the clickable UI design preview (HTML) and its illustrative images | `docs/design/ui-demo.html` (design only, not part of the running application) | The team reviewed the screens against the use cases and used the preview to agree the layout before coding. The images are decorative and generated, not photographs of the real restaurant |
| [Tool] | [Lam's use] | [part] | [how verified] |
| [Tool] | [Khoa's use] | [part] | [how verified] |

We declare that we understand every part of the submitted work and can explain it on request.

**Before signing, Giang checks being able to explain these parts of the code in an online walkthrough:**
- The BR-01 formula and why the boundary is 22:15.
- Why `findByIdForUpdate` stops double booking, and what the second request sees.
- The `switch` in `DiningTable.changeStatus`.
- How `TableStatusScheduler` marks a no-show, and why tests call `refreshStatuses(now)` directly.
- The difference between the 400, 403 and 409 errors in `GlobalExceptionHandler`.

## Appendix C — Individual contribution table

Fill in the commit counts at v1.0 with `git shortlog -sn --all` (merge commits excluded). Percentages must sum to 100%.

| Member | Main responsibilities | Main artefacts produced | Commits | Share of work (%) | Signature |
|---|---|---|---|---|---|
| Kieu Bao Giang (leader) | Team lead; repository and workflow; shared skeleton, login and roles; Table & Reservation module; restaurant interview write-up; report Chapters 1 and 4 and the appendices | Proposal; `docs/diagrams/giang/`; Flyway V1; Table & Reservation backend and frontend; 54 automated tests and TC-RES-01 to TC-RES-31; self-check, security check | [ ] | [ ] | |
| Tang Thoai Lam | Order & Kitchen module; review of Giang's pull requests | `docs/diagrams/lam/`; Flyway V2; menu and order code; [ ] | [ ] | [ ] | |
| Nguyen Nam Khoa | Billing & Reporting module | `docs/diagrams/khoa/`; [ ] | [ ] | [ ] | |
