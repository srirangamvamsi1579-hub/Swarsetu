# Swarsetu — Requirements (current build, v6)

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

## 8. Rules carried over
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

## 11. Change Log
| Date | Change | Section(s) Affected |
|---|---|---|
| 07 Sep 2026 | Initial requirements document created with all core modules and wireframe sections. | All screens |

> This document is a living document — updated as new requirements are added to the project.
