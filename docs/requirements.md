# Swarsetu — Requirements (current build, v9)

Last updated: 24 Nov 2026. The original v1.0 requirement list (Sept 2026) is kept below
this section for reference.

## 1. Roles and permissions
- Roles: **Admin**, **Faculty**, **Student**. Admin always has every permission.
- Admin sets Faculty and Student permissions under **Roles & Permissions**:
  View Dashboard; View / Edit College Profile; View / Add-Edit Students; View / Add-Edit
  Courses & Academic Structure; View / Edit Faculty Management; View / Create-Edit
  Timetable; View / Manage College Calendar; View / Mark-Edit Attendance; View /
  Create-Edit Classes; Share Class Links; View / Add-Edit Fee Payments; Send
  Notifications; View / Add-Edit Events; Manage Users and Roles; Delete records.
- Students can never be given access to other students' data, the dashboard, faculty
  management, notifications, user management or edit permissions.
- A screen the role can't open is hidden from the menu. Opening it directly shows an
  "Access denied" page. Blocked actions show an "Access denied" message.
- Users are created by an Admin (or by a user with Manage Users, who can't create or edit
  Admins). Each user has a role and a status (Active or Inactive). Inactive users can't
  sign in, and their records are kept.
- There is always at least one active Admin. A user can't deactivate or delete their own account.
- Usernames are not case-sensitive and must be unique. Emails must be valid and unique.
  Mobile numbers must be 10 digits. New passwords need 8+ characters with upper case,
  lower case, a number and a symbol.

## 2. College Profile
- **Students per Batch** applies only to **Online** group batches (1–50). Offline group
  batches have no limit. One-to-one batches always hold one student.
- Enrolling into a full Online batch, or moving students into one, is blocked with a message.
- The limit can be changed later. If it is lowered below the current count, existing
  students stay, new enrolments are blocked, and the capacity table shows "Over by N".
- The capacity table lists each group batch's mode, student count, limit and remaining places.
- Drive link and App link must start with https://.

## 3. Timetable
- From Date and To Date fields open a date picker when clicked. To Date earlier than
  From Date shows an error. Ranges can be up to 92 days.
- The selected range is shown as text. The calendar grid shows up to 14 days, and the list
  layout shows any valid range.
- Each class shows batch, subject, faculty (the course instructor), start, end, mode,
  location (offline) or meeting link (online), students and status.
- Users with Create/Edit Timetable can edit a single date (time, length, cancel,
  location or link, note), reset it to the batch default, or add extra sessions inside the
  selected range. Meeting links must be https.
- College Calendar holidays and vacations that apply to a batch cancel its classes on those dates.

## 4. Fee Payments
- Filters, each with a label: Select Student, Select Batch, Select Payment Status
  (Paid, Partially paid, Unpaid, Has dues), From Month, To Month, Select View, Search.
- Views: Payment list, Month-wise (one row per month), and Student-wise history (one
  column per month).
- Totals for the current filters: payments received, total amount collected, students
  who paid, pending amount, total billed.
- Each payment stores: student, **batch** (required), course, fee month, payment date,
  total fee, amount paid, dues (= total − paid), method, status and remarks.
- Paid amounts above the total fee are blocked, and payment dates can't be in the future.
  A second payment for the same student, batch and month asks for confirmation (instalments).
- Assumption: "collected" means the sum of amounts paid.

## 5. Attendance
- Filters, each with a label: Select Batch, Select Student, Period (single month or month
  range, up to 12 months), Select Month / From Month / To Month, Select Attendance Status, Search.
- Codes: **P** Present, **L** Late, **A** Absent, **V** Leave / approved leave, **H**
  Holiday, **DH** Faculty on leave, **C** Compensation.
- The user picks the status to apply, then taps cells. Changes stay unsaved until **Save
  attendance**. Discard is available, and leaving the page with unsaved changes asks first.
- Saved entries can be corrected by users with Mark/Edit Attendance. Each entry records
  who saved it, when, and the previous code.
- Completed = P + L. **Pending = Total − DH − H + C − Completed.** H counts manual H
  codes plus calendar holidays on scheduled days that have no code.
- The Summary covers a month range, with totals per code.

## 6. Classes
- Filters, each with a label: Select Batch, Select Subject, Select Faculty, From/To Date, Search.
- Sharing levels: **Not shared**, **Whole batch** (every student currently in the batch), and
  **Individual student(s)**. There is an optional expiry date and an access level (view, or
  view and download).
- Status badges: Shared with Batch / Shared with Student(s) / Not Shared / Expired.
- Copy link copies `<App link>#class-<code>`. New link replaces the code, so the old link
  stops working. Signed-in users can open a link only if the class is shared with them and
  not expired; staff need View Classes.
- New batch classes are shared with the batch by default when the user has Share Class Links.
- Audio links must be https.

## 7. College Calendar
- Views: Month (like Outlook), Year (12 mini months), and Date-range list. Filters: Select
  Year, Select Month, From/To Date, entry type.
- Entries: title, start date, end date (not before the start), type (Holiday, Vacation,
  Event, Other), description, and applies to (all batches, selected batches or selected students).
- Holidays, vacations, events, regular working days and weekly off days have different colours.
- Exact duplicate holidays or vacations (same type and dates, overlapping audience) are
  blocked. Overlaps ask for confirmation.
- Add, edit and delete need Manage College Calendar (Admin by default).

## 8. Data Import & Export
- Accepted files: Excel (.xlsx, .xls, .ods), CSV, TSV and TXT, PDF, and JPG, PNG or WEBP
  images, up to 25 MB. Pasted rows are also accepted.
- Data types: Students, Courses, Fee payments, Attendance (row or register layout),
  College calendar, User logins. A type is offered only if the user has the matching edit
  permission. Import also needs **Import data**.
- Flow:
  1. Choose the type and the file.
  2. Check what was read (sheet, header row, separator, layout).
  3. Match each field to a column or a default value.
  4. Review rows (New / Update / Skip / Error, inline edit), then confirm the import.
- Required fields must be matched or given a default before import is allowed. Rows with
  errors are not imported and can be downloaded to fix.
- Existing records are found by: student email, mobile or full name; course name; payment
  student + month + batch; attendance student + date; calendar title + start date;
  username. The choice is skip, update or add.
- Validation uses the same rules as the forms: email format, 10-digit mobile, Online batch
  limit, paid ≤ total, no future payment dates, attendance on class days only (except C),
  end date not before start, password strength, and only Admins import Admin accounts.
- Photos and scanned PDFs are read by Claude. The values are labelled and must be checked
  before importing.
- Each import is recorded with who, when, file, type and counts. Undo removes the records
  it added; updates are not reverted.
- Downloads: any list as Excel or CSV, all lists in one workbook, templates, rows with
  errors, and a JSON backup (Admin only). They need **Download / export data**.

## 9. Colleges, accounts and security (v8)
- **One link, many colleges.** Each college has a unique code (3 letters, a dash, and 4
  characters). Names are unique after normalising (ignoring case, punctuation and a leading
  "The").
- **Admins.** The first Admin is whoever registers the college. More Admins can be
  approved or promoted only by an Admin. Each college always keeps at least one active Admin.
- **Joining.** Faculty and students join by choosing the college or entering its code.
  Requests stay pending until an Admin (or a user with Manage Users) approves them,
  unless the college auto-approves students. Rejected requests show the reason at sign-in.
- **Identifiers.** Sign-in accepts a username, email or mobile. A username may be a name
  (letters, numbers, `.`, `_`, `-`), the user's own email, or the user's own mobile.
  Usernames, emails and mobiles are unique across all colleges.
- **Registration checks.**
  - email format, Gmail naming rules and common-typo suggestions; optional Gmail-only per college
  - mobile: 10 digits starting 6–9 for +91 (7–12 digits otherwise), rejecting repeated or
    sequential numbers
  - email and mobile verified by 6-digit codes, valid 10 minutes, 5 tries
- **Passwords.** PBKDF2-SHA256, 100,000 iterations, 16-byte salt. They need 8+
  characters with upper and lower case, a number and a symbol. Accounts lock for 15
  minutes after 5 failures. Users are signed out after 30 minutes idle.
- **Forgot password.** A code goes to the account's email and mobile. The response does
  not reveal whether an account exists.
- **Admin password reset.** The Admin re-enters their password, the user gets a one-time
  temporary password, and must change it at next sign-in.
- **Profile.** Users edit their own details. A new email or mobile must be verified. Users
  can change their password and request to move college (which needs approval there).
- **Reminders.** Holiday and vacation reminders go out N days before (default 1; 0 turns
  them off) to affected students and all staff, once per user per event. Optional
  SMS/email copies are simulated.
- **Absence notices.** Students notify their teacher for an upcoming scheduled class
  before it starts. Staff with Mark/Edit Attendance are notified and can mark it seen or as
  leave (V). Students can cancel before the class.
- **Fee payment details** (Admin only, password re-entry required):
  - payee name, UPI ID (`name@handle`), payment-app mobile, accepted apps, bank, branch,
    account number (9–18 digits, entered twice) and IFSC (`AAAA0XXXXXX`)
  - QR code generated from the UPI ID, or the college's uploaded scanner, which must decode
    to the same UPI ID
  - every change is logged, all Admins are notified, and students are warned for 14 days
- **Student payment.** The student pays in their own UPI or bank app, then submits the
  amount, method, reference (12-digit UTR for UPI; 6–22 characters for bank) and date.
  References are unique across all colleges. Staff with Add/Edit Fee Payments verify or
  reject. Verified amounts go into the month's payment record.
- **Navigation.** The Attendance group is collapsed by default.
- **Help.** In-app guides and FAQ, filtered by role, with search and a download.
- **Limits.** Separation between colleges and permission checks run in the browser, and
  the shared database can be read by every signed-in viewer of the link. Production
  needs a server.

## 10. Keys, loading and integrity (v9)
- **Keys.** The college code is the primary key and is stored on every record. The
  college name is a unique alternate key. Usernames, emails, mobiles and payment
  references are unique keys enforced by database indexes.
- **Loading.** Signed-out visitors read only the public directory. After sign-in the app
  loads only `c/{CODE}/…` for the user's college. Admins see only their own college's
  users and requests.
- **Deletes.** RESTRICT: courses used by students or batches, and students with
  payments. CASCADE: an unused course → its levels and slots; a student without
  payments → their attendance, notices, pending submissions and links; an emptied batch →
  its timetable changes.
- **Data health.** Admins can list and repair broken links. Payments are reported, never
  removed.

## 11. Rules carried over
- Batch = course + weekdays + timing + mode for group classes, with a private batch for
  each one-to-one student. Empty batches are removed automatically.
- Dues = Total fee − Paid. Every record gets a unique id before saving.
- Every data screen has add, edit and delete where it makes sense.
- Anything simulated is labelled (SMS, email and link sending).

---

# Original requirements (v1.0, Sept 2026)


Version 1.0 | Last updated: 07 Sep 2026

## 1. User Roles & Access
- **Admin**: full control over courses, faculty, students, fees, and reports.
- **Faculty/Instructors**: manage classes, attendance, grades, and schedules.
- **Students**: view courses, schedules, fees, grades, and practice materials.
- **Parents/Guardians (optional)**: view student progress and fee status.
- **Front Desk/Receptionist**: handle admissions, inquiries, and payments.
- Role-based login with secure authentication.

**Associated screen:** Login screen with role-based access (Admin / Faculty / Student).

## 2. Student Management
- Admission & enrollment with online forms and document upload.
- Student profiles: contact info, instrument/course, batch, and level.
- Attendance tracking per student and per batch.
- Progress and grade tracking (exams, recitals, skill assessments).
- Certificate and ID card generation.

**Associated screen:** Student profile view with fee status, attendance %, and grade progress.

## 3. Course & Curriculum Management
- Course catalog covering instruments, vocals, theory, and grade levels (e.g., Trinity/ABRSM).
- Batch and class scheduling per course.
- Syllabus and learning material uploads.
- Instrument allocation for college-provided instruments.

**Associated screen:** Course catalog listing with quick view and add-course action.

## 4. Scheduling & Timetable
- Weekly/monthly class timetables for individual and group lessons.
- Practice room and studio booking system.
- Automatic conflict detection for room, faculty, and student double-booking.
- Calendar sync (e.g., Google Calendar) for students and faculty.

**Associated screen:** Weekly timetable view with room and batch details per slot.

## 5. Fees & Payments
- Fee structure setup per course, instrument, or level.
- Online payment gateway integration.
- Invoice and receipt generation.
- Automated due-date reminders and late-fee handling.
- Refund and discount/scholarship management.

**Associated screen:** Fees dashboard showing dues, due date, and payment history.

## 6. Attendance
- Faculty can mark attendance per batch/class session.
- Present/Absent/Late status with timestamped records.
- Attendance percentage rollups visible to students, parents, and admin.
- Offline mode support with sync-on-reconnect (optional).

**Associated screen:** Faculty attendance-marking screen for a batch session.

## 7. Examination & Assessment
- Exam scheduling for internal assessments and external board exams.
- Grading system with report card generation.
- Recital and performance event scheduling tied to assessments.

**Associated screen:** Exams & report card screen with grade breakdown.

## 8. Faculty Management
- Faculty profiles with specialization and qualifications.
- Class assignment and personal timetable view.
- Faculty attendance and payroll tracking.
- Performance and feedback reviews.

**Associated screen:** Faculty home screen showing today's classes and quick attendance action.

## 9. Events & Recitals
- Event calendar for concerts, workshops, and masterclasses.
- Registration flow for events and competitions.
- Media gallery for photos/videos of performances.

**Associated screen:** Events listing with registration action per event.

## 10. Reports & Analytics
- Attendance summary reports across batches and students.
- Financial reports covering revenue, dues, and expenses.
- Student performance and progress reports.
- Faculty workload reports.

**Associated screen:** Admin analytics screen with revenue trend chart and report list.

## Non-Functional Requirements
- Mobile app (iOS/Android) plus a web portal for admin/faculty.
- Role-based access control and data security/encryption.
- Cloud-based data storage with regular backups.
- Scalability to support multiple branches/campuses.
- Multi-language support (optional).

## 12. Production server (v10)
- Java 21 / Spring Boot 3 REST API with MySQL 8. The current screens will call it.
- Separate colleges in the database (composite foreign keys), not only in the app.
- Real email (SMTP) and SMS (MSG91/Twilio) for registration codes, password reset,
  approvals, security alerts and holiday reminders. Manual UTR verification stays.
- BCrypt passwords, JWT with rotating refresh tokens, lockout, rate limits, audit log,
  HTTPS.
- Deploy with Docker Compose on any Linux server or VPS; nightly backups; CI on GitHub
  Actions.

## 11. Change Log
| Date | Change | Section(s) Affected |
|---|---|---|
| 07 Sep 2026 | Initial requirements document created with all core modules and wireframe sections. | All screens |

> This document is a living document — updated as new requirements are added to the project.
