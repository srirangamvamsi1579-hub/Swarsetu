# Swarsetu server (Java 21 + MySQL 8)

This is the production version of Swarsetu: a REST API on a server you control. It
replaces the shared database used by the claude.ai test link. The web screens stay the
same and will call this API (see **Status** below).

| Part | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot 3.3 (web, security, JDBC, validation, mail, actuator) |
| Database | MySQL 8 (InnoDB, utf8mb4); schema managed by Flyway (`src/main/resources/db/migration`) |
| Sign-in | BCrypt password hashes (cost 12); JWT access tokens (15 min); rotating refresh tokens stored as hashes |
| Email / SMS | Any SMTP provider; MSG91 or Twilio for SMS; queued in an outbox table and retried |
| Scanner check | ZXing reads the uploaded UPI QR code on the server |
| API docs | `/swagger-ui.html` (OpenAPI) |
| Deploy | Docker Compose: MySQL 8.4 + app + Caddy (automatic HTTPS) + nightly backup |
| CI | GitHub Actions: builds, runs the tests against MySQL, builds the Docker image |

## How colleges are kept apart

The database enforces this. Checks in the code are a second layer.

- Every college-owned table has `college_id` (the college code, e.g. `RAM-7K2Q`).
- Primary keys are `(college_id, id)`. Every link is a **composite foreign key**
  `(college_id, x_id) → parent(college_id, id)`. MySQL refuses any record that points into
  another college, for example a payment in college B for a student of college A.
- The API takes `college_id` from the signed-in user's database record, never from the
  request. Every query is filtered by it.
- On each request the server loads the user's role, college and status fresh from the
  database. A deactivated user, or a role change, takes effect immediately, even if their
  token has not expired yet.
- Unique keys:
  - college code (primary key)
  - college name (normalised: case, spaces, punctuation and a leading "The" are ignored)
  - username, email and mobile, across all colleges
  - payment reference (UTR), unless the earlier claim was rejected
- `db-tests/integrity.sh` proves these rules directly in MySQL (21 checks).

### Delete rules (same as the web app)

| Delete | Result |
|---|---|
| Course used by students | Refused |
| Unused course | Its academic levels and faculty slots are deleted too |
| Student with payments | Refused ("mark Inactive instead") |
| Student without payments | Attendance, absence notices and payment claims are deleted too (FK cascade); a linked account is unlinked |
| Last student in a batch | The batch is removed; payments keep the batch name for history |
| Last active Admin (deactivate or demote) | Refused |

## Security measures

- Passwords must be 8 or more characters, with a letter and a number. Stored as BCrypt hashes.
- Five wrong passwords lock the account for 15 minutes. Failed sign-ins are recorded in the audit log.
- Sign-in takes the same time whether or not the account exists, so the response does not reveal which accounts exist.
- Registration needs a 6-digit code by email, plus one by SMS when a mobile is given.
  - Codes are stored only as HMAC hashes and expire in 10 minutes.
  - Each code request allows 5 attempts.
  - At most 5 code requests per address per hour.
- **Forgot password** gives the same answer for any input. A successful reset signs the user out everywhere and sends an email alert.
- Refresh tokens rotate on every use. If an old token is reused, the whole session family is ended.
- Sign-in and registration endpoints are rate-limited to 30 requests per minute per IP address.
- Payment details:
  - Only an Admin can change them, and must re-enter their password.
  - Every change is audited, keeping the old UPI ID and the last 4 digits of the old account.
  - All Admins get an inbox message and an email about each change.
  - An uploaded scanner is decoded on the server and must pay the saved UPI ID.
- Students only ever see their own records. A student cannot be given an editing permission.
- HTTP security:
  - HTTPS through Caddy (HSTS) and a strict Content-Security-Policy.
  - No cookies, so no CSRF.
  - Actuator is closed to the internet except the health check.
- The MySQL port is not published; the database is reachable only inside Docker.
- Email and SMS message bodies are deleted after sending, so codes do not stay in the database.

## API overview

| Area | Endpoints |
|---|---|
| Registration & sign-in | `POST /api/auth/colleges` (new college + Admin) · `POST /api/auth/join` (faculty/student) · `POST /api/auth/verify` · `POST /api/auth/login` · `POST /api/auth/refresh` · `POST /api/auth/logout` · `POST /api/auth/forgot` · `POST /api/auth/reset` · `GET /api/public/colleges/lookup?code=` |
| Profile | `GET/PUT /api/me` · `POST /api/me/password` · `POST /api/me/contact` + `/verify` |
| College | `GET/PUT /api/college` · `GET/PUT /api/permissions` · `GET /api/dashboard` · `GET /api/data-health` + `POST /fix` · `GET /api/audit` |
| Users | `GET/POST /api/users` · `PUT /api/users/{id}` · `POST /api/users/{id}/approve` · `POST /api/users/{id}/reject` |
| Payment details | `GET/PUT /api/payment-details` · `POST/GET/DELETE /api/payment-details/qr` · `GET /api/payment-details/audit` |
| Academic | `GET/POST /api/courses`, `PUT/DELETE /api/courses/{id}` · `GET/POST /api/students`, `PUT/DELETE /api/students/{id}` · `GET /api/batches`, `PUT /api/batches/{id}` |
| Attendance | `GET/POST /api/attendance` · `GET /api/attendance/summary?fromMonth=&toMonth=` · `GET/POST /api/absence-notices`, `POST /{id}/seen`, `DELETE /{id}` |
| Fees | `GET/POST /api/payments`, `PUT/DELETE /api/payments/{id}` · `GET/POST /api/claims`, `POST /api/claims/{id}/verify`, `POST /api/claims/{id}/reject` |
| Calendar | `GET/POST /api/calendar`, `PUT/DELETE /api/calendar/{id}` |
| Inbox | `GET /api/inbox` · `POST /api/inbox/{id}/read` · `POST /api/inbox/read-all` |
| Other lists | `GET /api/docs/{collection}`, `PUT/DELETE /api/docs/{collection}/{id}`. Collections: classes, ttSessions, levels, facultySlots, events, notifications, importLog |

Errors always come back as `{"error": "message for the user"}`. A form also gets
`fields` with a message per field.

Background jobs:

| Job | When |
|---|---|
| Send queued email and SMS | Every 10 seconds |
| Holiday and vacation reminders | 07:00 every day, in `REMINDER_ZONE` |
| Clean up expired codes and tokens | 03:30 every day |

## Run it

**On a server (Docker):**

```bash
cd server
cp .env.example .env      # set DOMAIN, passwords, JWT_SECRET (openssl rand -base64 48), SMTP, SMS
docker compose up -d      # MySQL + app + Caddy; HTTPS certificate is obtained automatically
docker compose logs -f app
```

Point your domain's DNS A record at the server first. Back up `./backups` (nightly dumps,
14 days kept) to somewhere off the server.

**On a laptop (MySQL installed locally):**

```bash
mysql -uroot -e "CREATE DATABASE swarsetu; CREATE USER 'swarsetu'@'localhost' IDENTIFIED BY 'devpass'; GRANT ALL ON swarsetu.* TO 'swarsetu'@'localhost';"
JWT_SECRET=$(openssl rand -base64 48) mvn spring-boot:run
```

Without SMTP or SMS settings, codes and messages are written to the server log and the
API marks them `simulated`.

**Tests:** run `mvn verify`. It needs the `swarsetu_test` database. The tests wipe it and
then go through registration, codes, keeping colleges apart, approval, permissions,
attendance, fees and claims, reused UTR, refresh-token reuse, lockout, keeping one Admin,
and forgot password. To prove the database rules on their own, run
`./db-tests/integrity.sh`.

## Status

| Done | Pending |
|---|---|
| MySQL schema with composite foreign keys, tested (21 checks) | **Compile and run the Java tests.** Maven Central is blocked from the build machine; this happens as soon as `repo.maven.apache.org` is allowed |
| Phase 1 API: registration with codes, sign-in, forgot password, profile, users and approval, permissions, courses, students, batches, attendance and summary, absence notices, fees, claims, payment details with QR check, calendar, inbox, reminders, data health, audit | Connect the web screens to this API (sign-in, then each screen) |
| Docker Compose with HTTPS and backups; CI workflow | Phase 2 tables: classes and sharing, timetable session edits, academic structure, events (these use `/api/docs` for now) |
| | Import/export on the server (Apache POI, PDFBox) |
| | Test real email and SMS with your accounts (MSG91 needs a DLT-approved template with one variable, `VAR1`) |
