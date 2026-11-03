# Week 8 — Exception handling and security check

Scope: the shared backend (login, roles, error handling) and the Table & Reservation module.
Checked by Kieu Bao Giang on 03/11/2026 against the code on branch `feature/giang-week7-10`.

## 1. Exception handling (minimum scope item 8)

All errors go through `GlobalExceptionHandler`.
The client always gets JSON `{ message, fieldErrors }`, never a stack trace (NFR-11).

| Failure path | Exception | HTTP | Message the user sees |
|---|---|---|---|
| Invalid field in a request body | `BindException` (Bean Validation) | 400 | Per-field messages, e.g. "Phone number must have 10 digits and start with 0" |
| Rule on one field found in the service | `InvalidInputException` | 400 | e.g. "Table 12 seats only 2 guests" |
| Wrong type in a parameter, e.g. `startTime=7pm` | `MethodArgumentTypeMismatchException` | 400 | "Invalid value" on that field |
| Missing query parameter | `MissingServletRequestParameterException` | 400 | "tableId is required" |
| Malformed JSON | `HttpMessageNotReadableException` | 400 | "The request contains a value in the wrong format" |
| Business rule broken (BR-01 to BR-05) | `BusinessRuleException` | 409 | e.g. "Table 3 is already booked at 19:00; choose another time or table" |
| Record not found | `NotFoundException` | 404 | "Reservation not found" |
| Database unique constraint (data access) | `DataIntegrityViolationException` | 409 | "The change conflicts with existing data. Refresh and try again." |
| Row lock held too long (data access, NFR-06) | `PessimisticLockingFailureException` | 409 | "Someone else is changing this table right now. Please try again." |
| Not logged in | `AuthenticationException` / entry point | 401 | "Please log in" |
| Wrong role | `AccessDeniedException` | 403 | "Access denied" |
| Anything else | `Exception` | 500 | "Something went wrong. Please try again." The details are logged on the server only. |

Evidence: tests TC-RES-09 to TC-RES-11, TC-RES-14 and TC-RES-17 to TC-RES-25.
Screenshot: `docs/screenshots/giang/03-check-in-too-early.jpg`.

## 2. Persistent storage
- MySQL 8 with Flyway migrations: `V1` (Giang) and `V2` (Lam). Data survives a restart.
- Hibernate does not change the schema (`ddl-auto=none`). Flyway owns it.
- Constraints in the database back up the code:
  - unique table number;
  - unique reservation code;
  - capacity between 1 and 20 (CHECK);
  - foreign keys.

## 3. Security checklist

| # | Check | Status | Where |
|---|---|---|---|
| 1 | Passwords are stored as BCrypt hashes, never in plain text | Done | `SecurityConfig.passwordEncoder`, `DataSeeder` |
| 2 | Every `/api` endpoint except login needs a session | Done | `SecurityConfig.securityFilterChain` |
| 3 | Roles are checked on the server, not only hidden in the UI | Done | `@PreAuthorize` on every controller method (`AccessRules`); TC-RES-25: Kitchen Staff gets 403 on check-in |
| 4 | A new session id is issued at login (session fixation) | Done | `AuthController.login` → `request.changeSessionId()` |
| 5 | CSRF | Done, by cookie policy | Session cookie is `SameSite=Strict`, so other sites cannot send it. CSRF tokens are therefore off. |
| 6 | Login failure does not reveal whether the username exists | Done | Always "Invalid username or password" |
| 7 | No secrets in the repository | Done | MySQL password goes in `application-local.properties` (git-ignored) or `DB_PASSWORD` |
| 8 | Input validated at the API boundary | Done | Bean Validation on every DTO, plus checks in the services |
| 9 | SQL injection | Done | Spring Data JPA with parameters only; no SQL built from strings |
| 10 | No stack traces or SQL errors sent to the client | Done | `GlobalExceptionHandler` (section 1) |
| 11 | Two staff booking the same table at the same moment (NFR-06) | Done | `DiningTableRepository.findByIdForUpdate` (SELECT … FOR UPDATE); TC-RES-27 |

### Known limitations (to report honestly in Section 3.4.2)
- **No rate limiting on login.** A password-guessing script is not slowed down.
  - Acceptable for a restaurant LAN.
  - Would be needed on the internet.
- **Demo passwords are printed in the README.**
  - They exist only for marking.
  - A real restaurant must change them on the first start, or turn seeding off with `app.seed.enabled=false`.
- **The app has no HTTPS of its own.** It must run behind HTTPS (for example a reverse proxy) outside the LAN.
- **No audit log** of who changed a reservation.

## 4. Performance basics
- Index `ix_reservation_table_start (table_id, start_time)` serves the overlap check.
- The scheduler reads only CONFIRMED bookings in a window from 1 day before now to 30 minutes after now, every 60 seconds.
- `@EntityGraph` loads the table together with the reservations, so the list has no N+1 queries.
- `open-in-view=false`: no database access while the JSON is being written.
