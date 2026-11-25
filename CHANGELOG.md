# Changelog

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
