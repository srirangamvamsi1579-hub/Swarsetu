# Changelog

## v10 (in progress) — Java + MySQL server — 2026-11-25

A production server in `server/`. The claude.ai test link (v9) keeps working unchanged.

- **MySQL 8 schema** (Flyway `V1__core_schema.sql`): 22 tables.
  - Every college-owned table is keyed `(college_id, id)`, and links use composite foreign
    keys, so the database refuses data that crosses colleges.
  - Unique keys: college name, username/email/mobile across all colleges, and active
    payment reference.
  - Check constraints: mobile number, month format, amounts, calendar dates, https links,
    and that an uploaded scanner pays the saved UPI ID.
  - Dues and fee status are computed columns.
  - `db-tests/integrity.sh`: 21 checks, all passing.
- **Spring Boot 3 API (Java 21), Phase 1:**
  - registration of a new college or a join request, with email and SMS codes
  - sign-in by username, email or mobile; JWT and rotating refresh tokens; lockout
  - forgot password, profile and contact change, user approval, roles and permissions
  - courses, students and batches (online group capacity, automatic batches)
  - attendance with audit and monthly summary; absence notices
  - fees with multi-month entry; claims with staff verification
  - payment details with password re-entry, audit, alerts and server-side QR decoding
  - college calendar; inbox; holiday reminders by inbox and email
  - data health, audit log, dashboard
- **Real email (SMTP) and SMS (MSG91 / Twilio)** through a retrying outbox. Without
  settings it stays SIMULATED (written to the log).
- **Deployment:** Dockerfile; Docker Compose with MySQL 8.4, Caddy (automatic HTTPS) and
  nightly backups; `.env.example`; GitHub Actions CI with a MySQL service.
- **Not yet done:**
  - The Java code has not been compiled, because Maven Central is blocked from the build
    machine.
  - The web screens are not yet connected to the API.
  - Phase 2 tables are not built yet.

## v9 — 2026-11-25

### College code as the key; only your college's data is loaded
- New database layout keyed by college code: `c/{CODE}/…` for each college's records
  (users and Admins included), plus a public directory `colleges/{CODE}`.
- Unique indexes:
  - `collegeNames/{nameKey}`: the college name is a unique alternate key; names are
    normalised, and names in other scripts are supported
  - `logins/{hash}`: usernames, emails and mobiles are unique across all colleges; the
    index stores no personal data
  - `refs/{hash}`: each payment reference (UTR) can be used once
- **Sign-in:** the app looks up the index, loads that single account, checks the password,
  and then loads and listens to that college's records only. Before sign-in it reads only
  the public college list. Join requests are written straight into the chosen college.
- **Sign-out** saves pending changes, stops listening, and clears the college's data from
  memory and the browser.
- Inbox messages for a role ("all Admins", "people who manage users") are stored once,
  with each person's read status.
- **Automatic one-time upgrade:** v7/v8 shared lists and `meta/college` are moved under
  their college code, passwords are hashed, the indexes are built, and the old copies are
  deleted. If the upgrade cannot save, the old data is left untouched.

### Referential integrity
- Deleting a course in use is blocked; deleting an unused one also removes its levels and
  faculty slots.
- Deleting a student with payments is blocked; deleting one without payments also removes
  their attendance, notices and pending submissions, and unlinks related records.
- **Data health** card for Admins in College Profile: finds records pointing to missing
  students, courses or batches, or carrying another college's code, and repairs them
  safely.
- Sign-out now always returns to the sign-in tab.


## v8 — 2026-11-25

### One link for many colleges
- Each college has its own record and a unique **college code** (e.g. `RAM-7K2Q`). Every
  record carries its college's ID, and signed-in users see only their own college.
- **Register a new college:** the person who registers becomes its Admin. Duplicate
  college names are blocked at registration and when renaming in College Profile.
- **Join my college:** Faculty and Students choose their college or enter its code. The
  request goes to that college's Admins, who approve or reject it. On approval the Admin
  can change the role and, for students, link the enrolment; the app suggests a match by
  email or mobile.
- College Profile settings: sign-ups need approval or students are auto-approved,
  optionally require Gmail, the reminder lead time, and SMS/email reminders (simulated).
- Existing single-college data is moved automatically into its own college with a code.
- Student IDs are now unique across all colleges.

### Accounts & security
- Sign in with a username, email or mobile. A username can itself be a name, a mobile or
  a Gmail address. The role dropdown was removed: the role comes from the account.
- **Registration checks:** valid email with Gmail typo detection ("Did you mean …"),
  Gmail naming rules, and an optional Gmail-only rule. Indian mobiles need 10 digits
  starting 6–9, and obviously fake numbers are rejected. Usernames, emails and mobiles
  must be unique across everyone. Email and mobile are verified with codes (sending is
  simulated).
- Passwords are hashed (PBKDF2-SHA256 with a per-user salt). Existing plain-text
  passwords are converted on first load and never written to the database again.
- **Lockout:** 5 wrong passwords lock the account for 15 minutes, and Admins are
  notified. Users are signed out after 30 minutes idle.
- **Forgot password:** a 6-digit code that expires in 10 minutes and allows 5 tries. It
  does not reveal whether an account exists.
- **Admin "Reset password":** needs the Admin's own password, and gives a one-time
  temporary password that must be changed at first sign-in.
- Role changes, payment-detail changes and password resets create security notices in
  the Inbox.
- Signing out closes any open dialogs.

### My Profile
- Every user can edit their name, username, email, mobile, other contact, city and
  country. Changing the email or mobile needs a verification code. For students, the
  linked enrolment is updated too.
- Change password (needs the current one). Shows the security summary and the college
  name and code.
- Request to move to another college, which needs approval there.

### Inbox and automatic reminders
- A new Inbox, with an unread count in the menu.
- **Holiday and vacation reminders:** created automatically 1 day before by default
  (2, 3 or 7 days, or off, in College Profile). They go to the students they apply to and
  to all staff, with no duplicates. Optional SMS/email copies are simulated and logged in
  Notifications.

### Students: "I can't attend"
- My Attendance lets a student notify their teacher before an upcoming class, with a
  reason. They can cancel the notice before the class.
- Faculty see "Absence notices from students" under the register and in their Inbox.
  They can mark a notice as seen, or as leave (V); the student is told either way. The
  register marks noticed classes with **N**.

### Fee payments by UPI and bank
- **College Profile → Fee payment details (Admin):**
  - payee name, UPI ID, the mobile linked to payment apps, and which apps are accepted
  - bank, branch, account number (entered twice) and IFSC (format checked)
  - the QR code: either generated from the UPI ID, or the college's own scanner. An
    uploaded scanner is decoded, and is refused if it is not a UPI code or if it pays a
    different UPI ID.
- **My Fees (Student):**
  - shows dues, the payee name, a QR code with the amount filled in, the UPI ID and bank
    details with Copy buttons, and an "Open in a UPI app" link
  - safety tips, plus a warning if payment details changed in the last 14 days
  - after paying, the student sends the 12-digit UTR or bank reference; duplicate
    references are refused
- **Fee Payments (staff):** "Payments waiting for verification". Verify adds the amount to
  the student's payment for that month (or creates one). Reject records a reason. The
  student is told either way.

### Navigation and help
- The Attendance menu starts collapsed. It opens on click and closes again when you
  leave the attendance screens; it also works on phones.
- **Help & FAQ:** step-by-step guides for each role, a searchable FAQ, and a downloadable
  user guide. The same content is in `docs/USER_GUIDE.md`.
- Backups now cover one college. Restoring needs the Admin's password and never touches
  other colleges, user accounts or payment details.


## v7 — 2026-11-25

### Data Import & Export (new screen)
- Upload Excel (.xlsx, .xls, .ods), CSV or text (.csv, .tsv, .txt), PDF, or a photo
  (.jpg, .png). You can also paste rows.
- Data types: students, courses, fee payments, attendance (row or register layout),
  college calendar, user logins.
- **Check what was read:** choose the sheet and the header row (found automatically, even
  below a title row). For text files you can change the column separator.
- **Match columns to fields:** columns are matched automatically by heading, and the
  matching is remembered for next time. You can set a default value for any field, choose
  day-first or month-first dates, and choose what to do with existing records (skip,
  update or add). For students, you can choose to create missing courses.
- **Review:** each row is shown as New, Update, Skip or Error, with the reason. Values can
  be edited inline. Rows with errors are never imported.
- **Conversions:** Excel date and time cells; dates like 24/11/2026, 2026-11-24 and
  24 Nov 2026; months like Nov 2026; times like 5 PM; weekdays like "Mon, Thu"; Indian
  mobile numbers given with +91; ₹ amounts; attendance words such as Present and Late;
  payment methods such as GPay and NEFT.
- Students are placed in batches, and the Online seat limit is checked, including rows
  earlier in the same file.
- **PDFs:** text PDFs are split into table rows in the page. Scanned PDFs and photos are
  read by Claude and labelled "read by Claude — check every value".
- **Import history:** who imported what and when, with counts, and Undo for the records an
  import added.
- **Downloads:** any list as Excel or CSV, everything in one workbook, blank Excel
  templates with a "How to fill" sheet, rows with errors, and a full JSON backup (Admin).
  These use the downloads capability, so they now work inside claude.ai.
- New permissions: **Import data** (Admin by default) and **Download / export data**
  (Admin and Faculty by default).
- The Students screen's Import button now opens this screen. The old CSV-only dialog has
  been removed. This completes the pending "CSV mapping approval screen".


## v6 — 2026-11-24

### College Profile
- **Students per Batch** now applies only to **Online** group batches. Offline group batches
  have no seat limit.
- Enrolling a student into a full Online batch is blocked with a clear message. The limit
  can be changed at any time. If you lower it, current students stay, and new enrolments
  are blocked until there is room.
- New **Batch capacity** table showing each group batch's mode, student count, limit and
  remaining capacity.
- New field: **App link**, used to build class share links.

### Timetable
- Labelled **From Date / To Date** fields. Clicking either one opens the date picker.
- If To Date is earlier than From Date, an error message is shown (it used to be corrected silently).
- The selected range is shown in full, for example "Mon, 23 Nov 2026 → Sun, 29 Nov 2026 (7 days)".
- **List layout** for ranges up to 92 days (the calendar grid shows up to 14). It shows
  date, start, end, batch, subject, faculty, mode, location or meeting link, students and status.
- Single sessions can be edited within the range: change the time or length, cancel, set a
  location or meeting link, or add a note. **Extra sessions** can be added.
- Batches now store a default location (offline) and meeting link (online, https only).

### Fee Payments
- Labelled filters: Select Student, Select Batch, Select Payment Status, From Month,
  To Month, Select View, and Search.
- Filtering across several months.
- **Totals** at the bottom for the current filters: payments received, total amount
  collected, students who paid, pending amount and total billed.
- **Month-wise** view and **Student-wise history** view (one column per month).
- Payments now store **batch** (required), payment date, method, status
  (Paid / Partially paid / Unpaid, worked out automatically) and remarks.
- Paying more than the total fee is blocked. Saving a second payment for the same
  student, batch and month asks for confirmation first.

### Attendance
- Labelled filters: Select Batch, Select Student, Period (single month or month range),
  Select Month / From Month / To Month, Select Attendance Status, and Search.
- **Status to apply** buttons: Present, Absent, Late, Leave / approved leave, Holiday,
  Faculty on leave, Compensation, Clear, or cycle through codes.
- Changes are kept as a draft until you press **Save attendance**. The page shows how many
  changes are unsaved and offers Discard. Leaving the page with unsaved changes asks first.
- **Audit trail:** each saved entry records who saved it, when, and the previous code. This
  shows in the cell tooltip and in the "Last saved" line.
- Summary covers a month range and shows Present, Late, Absent, Leave, Holiday, DH and C,
  with totals.
- New codes: **L** (Late, counts as a completed class) and **H** (Holiday). College Calendar
  holidays on a student's class days count as H automatically.

### Classes
- Labelled filters: Select Batch, Select Subject, Select Faculty, From/To Date, and Search.
- **Share** dialog: Not shared, Whole batch, or Individual student(s). Optional expiry
  date, access level (view, or view and download), **Copy link**, and **New link** (the
  old link stops working).
- The Sharing column shows "Shared with Batch", "Shared with Student" (or N students),
  "Not Shared" or "Expired".
- Students see only classes shared with them that haven't expired.

### College Calendar (new)
- Month view (similar to Outlook), Year view, and Date-range list.
- Filters: Select Year, Select Month, From/To Date, entry type.
- Entries have a title, start date, end date, type (Holiday, Vacation, Event, Other),
  description, and who it applies to (all batches, selected batches or selected students).
- Exact duplicate holidays and vacations are blocked. Overlapping ones ask for confirmation first.
- Holidays and vacations cancel classes in the Timetable and count as H in Attendance for
  the batches and students they apply to.
- The old single-date holiday list from Faculty Mgmt was moved into the calendar automatically.

### Users & Roles
- **User Management:** add users, assign a role, filter by role or status, and activate or
  deactivate accounts. Deactivated users can't sign in, but their records are kept.
- **Roles & Permissions:** a permission matrix for Faculty and Student, covering view and
  edit for each module, Share Class Links, Manage College Calendar, Manage Users and
  Roles, and Delete records.
- Screens and buttons are shown according to permissions. If a user opens a screen they
  aren't allowed to, they see an "Access denied" page. Blocked actions show an
  "Access denied" message.
- Rules: there must always be at least one active Admin, you can't deactivate yourself,
  and only an Admin can manage Admin accounts.

### Fixes
- Confirmation dialogs are now built into the page. The browser's own `confirm()` is
  blocked inside the claude.ai viewer, so deletes there were not completing before.
- Every notification entry gets an id when it is saved.
- Phone layout: the side menu becomes a horizontal bar, and there is no horizontal page
  scrolling at 390 px.
- Removed unused code left over from earlier versions.

## v5 — earlier
Shared database, batches, weekly timetable, attendance register, classes with audio,
academic structure.
