PROJECT: Swarsetu — Music College Manager (v9)
Single-file HTML/CSS/vanilla JS app for music college teachers. Shared database via
the artifact db capability (one document per record, diff sync from persistAll()),
localStorage as offline copy. Web link for testing, Android/iOS app later.

MULTI-COLLEGE: one link, many colleges, keyed by college code. Storage: c/{CODE}/{list}/{id},
c/{CODE}/settings/college, public colleges/{CODE}; unique indexes collegeNames/{nameKey},
logins/{sha} (username/email/mobile), refs/{sha} (UTR). Load only the signed-in college
(loadCollege); pushDiff only writes paths that college owns. Enforce RESTRICT/CASCADE on
deletes and keep integrityIssues()/fixIntegrity() passing. Registering a college makes you its
Admin; faculty/students join by college or code and wait for approval. College names unique.

ROLES: Admin (everything, always), Faculty, Student. Faculty and Student rights come from
the Roles & Permissions matrix (college.permissions). Defaults: Faculty can add and edit
everything except College Profile, College Calendar, users and deleting records (they can
delete classes). Student is read-only for their own schedule, attendance, shared classes,
fees, calendar and events. Hide screens and buttons with .p-<perm> classes AND guard every
action with need('<perm>'). Inactive users cannot sign in. Keep at least one active Admin.

SCREENS: Dashboard, Students, Courses, Academic Structure, Faculty Mgmt, Timetable
(From/To date, calendar ≤14 days, list ≤92 days, per-date session edits and extra
sessions), College Calendar (month/year/list; Holiday, Vacation, Event, Other; applies to
all, selected batches or selected students), Attendance > Mark Attendance (status
buttons, draft then Save, audit by/at/was, month or month range) and Summary (month
range), Classes (labelled filters, share levels, expiry, Copy link), Fee Payments
(labelled filters, multi-month, list/month-wise/student-wise views, totals), Notifications,
Events & Recitals, Inbox (reminders, notices, payments, security), My Profile
(edit details, verify new email/mobile, change password, move college), Help & FAQ, Import & Export (upload Excel/CSV/text/PDF/photo → check read →
match columns to fields with defaults → review/fix rows → import; history with undo;
downloads as Excel/CSV, templates, error rows, backup), College Profile (Online seat limit, batch capacity table, App link),
User Management, Roles & Permissions.

RULES
- Batch = course + weekdays + timing + mode for group; private batch per one-to-one student.
  Students per Batch (college.groupSize) limits ONLINE group batches only. Offline group
  batches have no limit. Empty batches are removed automatically.
- Codes: P (stored Attended), L (Late), A, V (Leave), H (Holiday), DH, C.
  Completed = P + L. Pending = Total - DH - H + C - Completed (calendar holidays on class
  days count as H when no code is recorded).
- Dues = Total fee - Paid. Payment needs a batch. Status = Paid / Partially paid / Unpaid.
- Usernames case-insensitive. Mobile = 10 digits. Every filter has a visible label.
- Date ranges: To may not be earlier than From. Show an inline error, never correct it silently.
- Every data screen needs add, edit and delete where it makes sense.
- Any new list or record type: add it to state, snapshot()/persistAll(), loadAll(),
  DB_COLS and applyRemote.
- Give every record a unique id before saving.
- Label anything simulated. Audio, meeting, Drive and app links must be https.
- Passwords: PBKDF2 hashes only (setPassword/verifyPw); never write `password` to the
  database or localStorage (stripSecrets). Lockout 5/15 min, idle sign-out 30 min.
- Sign-up: emailCheck (Gmail typos), mobileCheck (+91: 6-9 start), usernameCheck (name,
  own mobile or own email), unique identifiers, verification codes (simulated).
- Payment details: Admin + reauth(); QR upload must decode to the same UPI ID; audit +
  Admin notices + 14-day student warning. Student payments: unique UTR, staff verify.
- Attendance nav group is collapsed by default.
- Imports reuse the form rules (validation, batches, ids) and never save rows with errors.
  Photos and scanned PDFs are read by Claude (sample capability) and labelled. Excel/PDF
  readers load from cdnjs (SheetJS 0.18.5, pdf.js 3.11.174).
- Inside the claude.ai viewer: no confirm/alert/prompt (use ask()), downloads only via the
  downloads capability (saveFile()), and a
  link can only pass a plain #name (share links use #class-<code>).

ENGINEERING GATES: node --check on the script, every getElementById/gid has a matching id,
every inline handler is defined, open each screen in a browser with a stand-in database
for each role, check phone width (no horizontal page scroll at 390 px).

DELIVERABLES EVERY ROUND: updated index.html, README.md, CHANGELOG.md, docs/requirements.md,
docs/data_model.md, docs/project_prompt.md (this prompt), a shareable link, and a short
summary of what changed, what is pending and what is simulated. Commit to GitHub only
when the push has succeeded.

SERVER (v10, in server/): Java 21 + Spring Boot 3 + MySQL 8 (Flyway), JDBC (JdbcClient),
Spring Security with JWT access tokens (15 min) and rotating hashed refresh tokens, BCrypt.
Every college-owned table is keyed (college_id, id) with composite foreign keys; college_id
always comes from the signed-in user's database row. Email over SMTP and SMS via MSG91/Twilio
through the outbox table (SIMULATED = logged when not configured). Docker Compose (MySQL 8.4,
app, Caddy HTTPS, nightly backup), GitHub Actions CI. Any new table: Flyway migration Vn__,
composite FK to its parents, endpoint with perms.need(), test in ApiFlowTest, check in
db-tests/integrity.sh. Gates for the server: mvn verify, db-tests/integrity.sh.

PENDING: connect the web screens to the API, Phase 2 tables (classes, timetable edits, academic
structure, events), server-side import/export, real email/SMS account tests, payment
gateway or bank statement matching, Android/iOS packaging.
