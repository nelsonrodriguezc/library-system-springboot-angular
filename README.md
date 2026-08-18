**🌐 Language:** **English** · [Español](README.es.md)

# Libris — Library Loan Management System

Replaces a library's loan spreadsheet with a real application: a searchable catalogue,
loans with actual business rules (overdue tracking, account blocks, a waiting list),
e-mail reminders before a loan falls due, and record autofill from Open Library given
just an ISBN.

> **A note on language:** the interface, validation messages and API error codes are in
> **Spanish** — a deliberate choice, since the assessment this project was built for
> targets a Spanish-speaking library. This documentation is in English by default for
> portfolio visibility, with a [full Spanish version](README.es.md) available.

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Spring Boot 3.5" src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F">
  <img alt="Angular 21" src="https://img.shields.io/badge/Angular-21-DD0031">
  <img alt="PostgreSQL 16" src="https://img.shields.io/badge/PostgreSQL-16-336791">
</p>

---

## 1. Run everything

You only need **Docker**. No local JDK, Maven or Node required.

```bash
docker compose up --build
```

| What | Where |
|---|---|
| Application | http://localhost:4200 |
| API + Swagger UI | http://localhost:8080/swagger-ui.html |
| Mailbox (MailHog) | http://localhost:8025 |
| Backend health | http://localhost:8080/actuator/health |

The first run takes a few minutes (it compiles both the backend and the frontend inside
the images). To stop everything and drop the data: `docker compose down -v`.

### Test accounts

Created by the migrations. These are throwaway credentials for a throwaway database;
**there is no real secret anywhere in this repository**.

| E-mail | Password | Role |
|---|---|---|
| `admin@libris.cl` | `Admin123!` | ADMIN |
| `bibliotecario@libris.cl` | `Biblio123!` | BIBLIOTECARIO (librarian) |
| `lector@libris.cl` | `Demo123!` | BIBLIOTECARIO (librarian) |

The `demo` profile (on by default) also loads loans, late returns, reservations and one
blocked account, so the dashboards are not empty on first login.

### Reading the e-mails

Everything the application sends lands in **MailHog**: http://localhost:8025

To avoid waiting for the 08:00 cron, an ADMIN can trigger the scheduled jobs by hand from
**Administración → Resumen**, or through the API:

```bash
curl -X POST http://localhost:8080/api/admin/notifications/due-soon-reminders \
  -H "Authorization: Bearer $TOKEN"
```

---

## 2. Five-minute walkthrough

1. Sign in as **admin@libris.cl / Admin123!**
2. **Catálogo → Agregar libro** → ISBN `9780321356680` → *Buscar información*.
   It fills itself in: *Effective Java*, Joshua Bloch, 2008, with cover and subjects.
3. Confirm. The book is now available.
4. Borrow it → the **confirmation e-mail** lands in MailHog.
5. Open another session as `lector@libris.cl` and **reserve** that same book (it's out now).
6. Return it from the first session → the copy goes to **RESERVADO** and the reader who
   was waiting gets the *book available* notice.
7. In **Administración → Resumen**, click *Enviar recordatorios* and check MailHog.

---

## 3. Architecture

```
backend/    REST API in Spring Boot (Java 21)
frontend/   Angular 21 application (standalone + signals)
docs/       Data model and Postman collection
```

📐 **Entity-relationship diagram, constraints and sample queries:**
[`docs/data-model.md`](docs/data-model.md)

### Backend: packages by feature

```
com.libris
├── auth/           Registration, login, JWT
├── book/           Catalogue, ISBN
│   ├── metadata/   Port toward external catalogues + Open Library adapters
│   └── recommendation/  Content-based recommender
├── loan/           Loans
│   ├── policy/     Due-date calculation and derived status
│   └── rule/       Eligibility rules, one class per rule
├── reservation/    Waiting list
├── user/           Accounts, block policy, administration
├── stats/          Dashboard statistics
├── notification/   Events, templates, delivery and scheduled jobs
└── shared/         Business exceptions and the single error shape
```

### How SOLID is actually applied

Not as a label: each principle solved a concrete problem in this system.

**SRP — one reason to change.**
`DueDateCalculator` only computes `loan date + 14 days`. `OverdueBlockPolicy` only decides
whether a late return costs the block. `LoanStatusResolver` only translates a loan into
the status the interface shows. `LoanService` holds no rule at all: it orchestrates and
opens the transaction. Controllers carry no business logic.

**OCP — open for extension, closed for modification.**
The conditions a loan must pass are beans implementing
[`LoanRule`](backend/src/main/java/com/libris/loan/rule/LoanRule.java); Spring injects
them as an ordered list. Adding a rule means adding a class — `LoanService` never
changes. The proof is in the history: the waiting-list rule
([`ReservationHolderRule`](backend/src/main/java/com/libris/reservation/ReservationHolderRule.java))
is contributed by the reservation module and plugs in without touching the loan module.

**LSP — genuinely substitutable.**
[`BookMetadataSource`](backend/src/main/java/com/libris/book/metadata/BookMetadataSource.java)
has a total contract: it **never throws**. An outage, a timeout, a 429 or an unknown ISBN
all produce the same `Optional.empty()`. That is why the two Open Library implementations
are interchangeable, and why the catalogue keeps working when the third party does not.

**ISP — interfaces sized to whoever uses them.**
Instead of one `NotificationService` with five methods, there are three ports:
[`LoanNotifier`](backend/src/main/java/com/libris/notification/port/LoanNotifier.java),
`AccountNotifier` and `ReservationNotifier`. The scheduled jobs depend only on the first.

**DIP — depend on abstractions.**
The domain depends on `EmailSender`, `TemplateRenderer`, `BookMetadataProvider` and an
injected `Clock`. No rule imports `JavaMailSender`, `RestClient`, or calls
`LocalDate.now()` directly; that is what lets the tests pin time with `Clock.fixed` and
verify the 90-day window without depending on the day they happen to run.

---

## 4. Business rules

| Rule | Lives in | Error returned |
|---|---|---|
| The copy must be on the shelf | `BookAvailableRule` | `409 BOOK_NOT_AVAILABLE` |
| A held copy is only for its holder | `ReservationHolderRule` | `409 BOOK_RESERVED_FOR_ANOTHER_USER` |
| The account must not be blocked | `BorrowerNotBlockedRule` | `409 USER_BLOCKED` |
| At most 3 active loans | `MaxActiveLoansRule` | `409 MAX_ACTIVE_LOANS` |
| ISBN must be unique | `BookService` + unique index | `409 DUPLICATE_ISBN` |
| Only available copies can be deleted | `BookService` | `409 BOOK_NOT_DELETABLE` |

**On loan:** the book moves to `PRESTADO`, the due date is set 14 days out, and an event
is published that sends the confirmation e-mail off the HTTP thread.

**On return:** a late return is recorded as a strike. On the **third strike in 90 days**
the account is blocked for **one week** and notified; an ADMIN can lift it early. If
someone was waiting for that title, the copy goes to `RESERVADO` for the first person in
the queue and they are notified by e-mail, instead of going back to `DISPONIBLE`.

The block is stored as `blocked_until` (a timestamp), not a boolean: it **lapses on its
own**, with no cleanup job required.

---

## 5. E-mail

Five Thymeleaf templates under
[`templates/email/`](backend/src/main/resources/templates/email), built on a shared
layout using tables and inline styles (mail clients still don't support flexbox):

| Template | When |
|---|---|
| `loan-confirmation` | when the loan is registered |
| `due-soon-reminder` | daily job, 2 days before the due date |
| `overdue-notice` | daily job, once the loan is overdue |
| `account-blocked` | on the third strike |
| `book-available` | when a title someone was waiting for is freed up |

Delivery **never blocks the HTTP request**:

```java
@Async(AsyncConfig.NOTIFICATION_EXECUTOR)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onLoanCreated(LoanCreatedEvent event) { ... }
```

`AFTER_COMMIT` guarantees no e-mail can ever describe a loan that ended up rolled back,
and `@Async` on its own pool keeps a slow SMTP server from consuming the web server's
threads.

Both jobs are **idempotent**: they stamp `reminder_sent_at` / `overdue_notice_sent_at` as
they send, so running them twice on the same day never duplicates a notice.

---

## 6. Open Library integration

`GET /api/books/lookup/{isbn}` previews without saving anything, and `POST /api/books`
accepts **just the ISBN** and fills in the rest.

**Two sources behind the same port.** The assessment illustrates the integration with
`/isbn/{isbn}.json`. That endpoint returns the *edition*: title, year and cover, but
**not the author or the subjects**, which live on the *work* and would cost three or four
chained calls. The primary source is therefore `/api/books?jscmd=data`, which answers
everything in one call, with the endpoint from the assessment kept as the fallback. Both
implement `BookMetadataSource` and are queried in order.

**Details that took some digging and are now handled:**

- `/isbn/{isbn}.json` answers with a **302** toward the canonical URL → the client
  follows redirects.
- Open Library **throttles anonymous traffic** → a dedicated `User-Agent` is sent.
- Subjects arrive mixed with **shelving codes** (`Qa76.73.j38`, `005.13/3`), which pollute
  the filters and skew the recommendations →
  [`SubjectSanitizer`](backend/src/main/java/com/libris/book/metadata/SubjectSanitizer.java)
  strips them out.

**Caching.** `@Cacheable` over Caffeine (24 h, 1,000 entries). Empty results are **never
cached**: a momentary outage should not poison the cache for a whole day.

**When it fails.** Registering the book **does not break**: it saves with whatever the
person typed. Manual input always wins over what the external catalogue returns. If there
is neither manual nor external data, the API answers `400 INCOMPLETE_BOOK_DATA` asking
for a title and an author. The preview endpoint does report the failure
(`503 EXTERNAL_LOOKUP_FAILED`), because there the caller explicitly asked for external
data.

> Worst-case latency: 3 s per source, 6 s if neither answers.

---

## 7. API

Errors always share the same shape:

```json
{
  "timestamp": "2026-08-11T20:46:18.114Z",
  "status": 409,
  "code": "DUPLICATE_ISBN",
  "message": "Ya existe un libro registrado con el ISBN 9780132350884",
  "path": "/api/books",
  "fieldErrors": { "isbn": "El ISBN no es válido" }
}
```

`code` is stable and meant for the client to react to a specific rule without parsing the
message — which, matching the rest of the interface, is written in Spanish.

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` · `/api/auth/login` | public |
| GET | `/api/books` · `/api/books/{id}` · `/api/books/subjects` | authenticated |
| GET | `/api/books/lookup/{isbn}` | authenticated |
| GET | `/api/books/recommendations` | authenticated |
| POST | `/api/books` | **ADMIN** |
| DELETE | `/api/books/{id}` | **ADMIN**, only if `DISPONIBLE` |
| POST | `/api/loans` · GET `/api/loans/mine` · PUT `/api/loans/{id}/return` | authenticated |
| POST | `/api/reservations` · GET `/api/reservations/mine` · DELETE `/api/reservations/{id}` | authenticated |
| GET | `/api/admin/stats` | **ADMIN** |
| GET | `/api/admin/users` · PUT `/api/admin/users/{id}/unblock` | **ADMIN** |
| POST | `/api/admin/notifications/{due-soon-reminders,overdue-notices}` | **ADMIN** |

**Postman collection:** [`docs/postman/`](docs/postman) — 30 requests with tests,
including a *Reglas de negocio* (business rules) folder holding the cases that **must
fail**. Import the collection and the environment, run *Login (ADMIN)*, and the token
saves itself.

**Security.** JWT HS256 over Spring Security, stateless, passwords hashed with BCrypt.
`JWT_SECRET` comes from an environment variable; **if it is unset, the application
generates an ephemeral key and logs a WARN about it**. That is what keeps
`docker compose up` a single command with no secret ever committed, at the cost of
invalidating tokens on every restart.

---

## 8. Frontend

Angular 21, *standalone*, **zoneless**, with signals, `@if/@for`, and routes loaded on
demand. No UI or charting library: the doughnut and the trend line are **hand-written
SVG**. Initial bundle: **~95 kB compressed**.

| Screen | What it does |
|---|---|
| Login / Register | mirrors the server's validation, "remember me" |
| Home | own metrics, upcoming due dates, recommendations |
| Catalogue | search, filters by status and subject, sorting, pagination |
| Add book | 3-step wizard with ISBN autofill |
| My loans | tabs, countdown, return action |
| My reservations | queue position, cancel, borrow |
| Administration | overview with charts, accounts and blocks, catalogue management |

An interceptor attaches the token to every call to `/api` (and **only** there: cover
images go to Open Library, and sending them the token would leak it). Another interceptor
turns any failure into a visible notice — there is no silent error. Every screen has its
own **loading** (skeletons shaped like the eventual content), **error** (with a retry
button) and **empty** states.

The catalogue filters live in the URL (`/catalogo?q=clean&estado=DISPONIBLE`), so a search
can be shared as a link and the back button does what you'd expect.

### Recommendations

Content-based filtering, the classic baseline for recommender systems: a subject profile
is built from the reader's loan history (weighted by recency, with an 180-day half-life),
subjects are weighted with **TF-IDF** — "Software engineering," which appears in almost
every book, counts for far less than "Distributed systems" — and results are ranked by
**cosine similarity**.

**No external service, no credentials, no cost.** The interface explains every suggestion
by showing the shared subjects, and an account with no history gets no invented
recommendations: the panel simply does not appear.

---

## 9. Testing

```bash
cd backend && ./mvnw verify
```

**126 tests** (101 unit + 25 integration).

| What it covers | With what |
|---|---|
| Loan, overdue and block rules | JUnit + Mockito with `Clock.fixed` |
| Duplicate ISBN, deletion, enrichment | `BookServiceTest` |
| Open Library: success, 404, 500, 429, malformed JSON, and **timeout** | **MockWebServer** |
| Catalogue and loan endpoints, 401/403 by role | **MockMvc + Testcontainers** (real PostgreSQL) |
| That the e-mail **actually goes out**, with the right subject and recipient | **GreenMail** |

Tests run against real PostgreSQL, not H2, because the schema uses partial unique indexes
and `to_char`: testing against a different engine would say nothing about the migrations
that actually ship.

> **Note on Windows:** the MockWebServer tests need loopback sockets. On some machines
> with strict antivirus software they fail with `Unable to establish loopback
> connection`. They pass without issue on Linux, in Docker and in CI; if this happens to
> you, run the suite inside a container.

---

## 10. Decisions taken

The assessment asks for decisions to be documented rather than left as gaps. Here they are.

### About the model

**The loan links to the account by e-mail.** The specification defines `Loan` with
`borrowerName`/`borrowerEmail` (no relation to `AppUser`), yet also asks for
`/api/loans/mine` and for blocking accounts. Both fields are kept exactly as specified and
resolved against `AppUser` by e-mail: the block applies to that account, and `/mine`
filters by the authenticated e-mail. An ADMIN may register a loan on behalf of another
account; a BIBLIOTECARIO only for themselves.

**`RESERVADO` does not mean "free."** A held copy can only be borrowed by the holder of
the reservation; the reservation moves to `CUMPLIDO` automatically once that loan is
registered.

### Added beyond the specification

**Maximum of 3 active loans.** Not in the specification: it comes from the interface,
which promises "you can have up to 3 active loans." Implemented as a configurable rule
(`LIBRIS_MAX_ACTIVE_LOANS`) rather than removing the text.

**Overdue notice e-mail.** The specification asks for four e-mails, but `Loan` includes
`overdueNoticeSentAt`: that field only makes sense if something actually sends that notice
and records it.

**Three endpoints outside the listed set**, required by the rules themselves and by the
screens: `GET /api/admin/users` and `PUT /api/admin/users/{id}/unblock` (the specification
requires that an ADMIN be able to lift a block), `GET /api/reservations/mine` (without it
"My reservations" cannot exist), and `POST /api/admin/notifications/*` (to demonstrate the
scheduled jobs without waiting for 08:00).

### Corrections to the reference design

The reference design had elements that contradict the rules or that the specification
does not ask for. What was done about each:

| In the design | What was done | Why |
|---|---|---|
| ADMINISTRACIÓN section visible to a librarian | Hidden unless ADMIN | Only ADMIN registers books and sees statistics |
| Global KPIs on "Inicio" | "Inicio" shows the reader's **own** metrics | `/api/admin/stats` is ADMIN-only: a librarian would see nothing but 403s |
| "Marcar como recibido" on a reservation | Replaced with **"Pedir prestado"** | No such endpoint exists; the reservation closes itself once the loan is registered |
| Doughnut with "Por vencer" nested inside "Activos" summing to 100% | Four **mutually exclusive** states | Otherwise the percentages count the same loan twice |
| Total 2,148 ≠ Available 1,258 + Borrowed 138 | Total = available + borrowed + reserved | Arithmetic consistency |
| Google sign-in, password recovery | Removed | Not requested, and would require OAuth credentials committed to the repository |
| Notification bell, "Actividad reciente," "Configuración" | Removed | Would need entities nobody asked for |
| Registration, "Reservar" and "Eliminar libro" were missing | Added | The specification does require those endpoints |

### Technical choices

- **Spring Boot 3.5**, not 4.x: the 3.x line is the mature choice for springdoc and
  Testcontainers, and the assessment asks for "3.3+".
- **No Lombok**: `record` for DTOs and explicit accessors on the entities. Less magic in a
  project meant to be read for evaluation.
- **Explicit nulls in the JSON**: `returnDate` travels as `null` rather than disappearing,
  so the client can tell "not returned yet" apart from "field absent."
- **The health check does not depend on SMTP**: the API is perfectly usable with mail
  down, because delivery is asynchronous.

---

## 11. Environment variables

Every one of them has a default: `docker compose up` works with no file to create. To
customise, copy [`.env.example`](.env.example) to `.env`.

| Variable | Default | What for |
|---|---|---|
| `FRONTEND_PORT` / `BACKEND_PORT` | `4200` / `8080` | published ports |
| `POSTGRES_DB` / `_USER` / `_PASSWORD` | `libris` | database |
| `JWT_SECRET` | *(empty)* | HS256 key; empty ⇒ ephemeral + WARN |
| `JWT_EXPIRATION_MINUTES` | `480` | token lifetime |
| `SPRING_PROFILES_ACTIVE` | `demo` | `default` to start with no sample data |
| `MAIL_HOST` / `MAIL_PORT` | `mailhog` / `1025` | SMTP server |
| `LIBRIS_LOAN_DAYS` | `14` | loan length |
| `LIBRIS_MAX_ACTIVE_LOANS` | `3` | simultaneous loans |
| `LIBRIS_DUE_SOON_DAYS` | `3` | "due soon" window |
| `LIBRIS_REMINDER_DAYS_BEFORE` | `2` | reminder lead time |
| `LIBRIS_OVERDUE_LIMIT` | `3` | strikes that trigger a block |
| `LIBRIS_OVERDUE_WINDOW_DAYS` | `90` | rolling window for strikes |
| `LIBRIS_BLOCK_DAYS` | `7` | how long the block lasts |
| `TZ` | `America/Santiago` | due dates are business dates |

---

## 12. Developing without Docker

```bash
# Database and mailbox
docker compose up -d postgres mailhog

# Backend (requires JDK 21)
cd backend && ./mvnw spring-boot:run

# Frontend (requires Node 20.19+/22.12+/24+)
cd frontend && npm install && npm start
```

`npm start` serves Angular on port 4200 and proxies `/api` to port 8080 via
[`proxy.conf.json`](frontend/proxy.conf.json).

---

## 13. Git

Built with **GitFlow**: `main` ← `release/*` ← `develop` ← `feature/*` and `fix/*`, merged
with `--no-ff` to keep each branch's history intact. Commits are short, in English, and
follow a *conventional commits* style.

```bash
git log --graph --oneline --all
```
