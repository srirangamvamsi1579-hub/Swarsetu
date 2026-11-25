PROJECT: Swarsetu — Music College Manager (v7)
Single-file HTML/CSS/vanilla JS app for music college teachers. Shared database via
the artifact db capability (one document per record, diff sync from persistAll()),
localStorage as offline copy. Web link for testing, Android/iOS app later.

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
Events & Recitals, Import & Export (upload Excel/CSV/text/PDF/photo → check read →
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

PENDING: mobile login, Gmail and phone validation everywhere, automatic vacation reminders
one day before, server-side auth with hashed passwords, importing classes and events.
