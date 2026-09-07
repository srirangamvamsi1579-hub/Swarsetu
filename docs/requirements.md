# Music College Management App — Requirements & Screen Reference Document

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
