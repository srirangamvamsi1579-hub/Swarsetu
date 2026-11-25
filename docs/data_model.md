# Swarsetu — Data model (v7)

The app keeps one list per record type. Each list is a collection in the shared database
(the artifact `db` capability). Each record is a document with the same id as the record.
College settings are a single document, `meta/college`. `localStorage` key
`swarsetu_data_v4` keeps an offline copy of everything.

To add a new list, add it to: the state variables, `snapshot()` (used by
`persistAll()` and `writeStore`), `loadAll()`, `DB_COLS` and `applyRemote()`.

## meta/college
| Field | Type | Notes |
|---|---|---|
| name, affiliation, location, country, address, phone, email | string | |
| groupSize | number 1–50 | Students per **Online** group batch |
| driveLink, appUrl | https string | appUrl is the base for class share links |
| youtube, instagram, facebook | string | |
| workingHours {start,end}, workingDays [Mon…Sun] | | |
| permissions {faculty:{perm:bool}, student:{perm:bool}} | object | **v6.** Admin always has every permission |
| importMaps {target:{normalisedHeading:fieldKey}} | object | **v7.** Remembered column matching |
| calMigrated, v5seeded | bool | migration flags |
| holidays | [] | legacy; moved into calendarEvents in v6 |

## users
`id, firstName, lastName, username, password, role (admin|faculty|student), email, mobile,
countryCode, fullMobile, altContact, city, country, collegeName, affiliation,`
**`active` (bool, v6)**, `createdBy, createdAt, updatedBy, updatedAt`.

## students
`id (STU0001-XXX), firstName, lastName, email, mobile, countryCode, parent, ageGroup, city,
country, level, levelOther, courseId, courseName, classType (Group|One-to-One), frequency,
weekdays[], timing, interval ('30'|'60'), mode (Online|Offline), monthlyFee, joiningFee,
joiningDate, totalClassesPerMonth, pendingClasses, status, batchId, batchName`.

## batches
`id` = `bg_<course>_<days>_<time>_<mode>` for group batches, or `b1_<studentId>` for
one-to-one. Fields: `name, courseId, courseName, classType, weekdays[], timing, interval,
mode, capacity` (groupSize for Online group batches, `null` = no limit for Offline group
batches, 1 for one-to-one), **`location`, `meetLink` (v6)**.

## ttSessions (new in v6)
Changes to a single date, and extra sessions.
| Field | Notes |
|---|---|
| id | `ts_<batchId>_<date>` for a change to a regular class; `tsx_…` for an extra session |
| batchId, date | |
| extra | bool |
| timing, interval | start time and length in minutes (30/45/60/90) |
| status | Scheduled / Cancelled |
| location, meetLink, note | meetLink must be https |
| updatedBy, updatedAt | |

## calendarEvents (new in v6)
`id, title, type (Holiday|Vacation|Event|Other), start, end, desc, appliesTo
(all|batches|students), batchIds[], studentIds[], createdBy, createdAt, updatedBy, updatedAt`.
Holiday and Vacation entries cancel classes and count as H for the batches and students they apply to.

## attendance
One record per student per month:
`id, studentId, yearMonth, batch, courseId, sessions[], updatedBy, updatedAt (v6)`.
Session: `{date, status, by, at, was}`, where status is
`Attended | Late | Absent | V | H | DH | C`. `by`, `at` and `was` (the previous code) are new in v6.
Codes on screen: P, L, A, V, H, DH, C.
Completed = Attended + Late. Pending = Total − DH − H + C − Completed.

## payments
`id, studentId, student, batchId (v6, required), batchName, course, month (YYYY-MM),
paymentDate (v6), totalFee, paid, dues (= totalFee − paid), mode (method),
status (v6: Paid | Partially paid | Unpaid), remarks (v6), updatedBy, updatedAt`.

## classLogs
`id, date, batchId, batchName, courseId, topic, notes, audio[{title,url https}],
students[]` (attendees when there's no batch), and sharing (v6): `shareLevel
(none|batch|students), shareStudents[], shareExpiry, shareAccess (view|download),
shareToken, sharedWith[]` (a snapshot of the audience at save time). Share link:
`<appUrl>#class-<shareToken>`.

## importLog (new in v7)
`id, at, by, target (students|courses|payments|attendance|calendar|users), file, kind,
added, updated, skipped, errors, addedIds[] ("collection:id", or "attendance:studentId|date"),
newCourses[], undone, undoneBy, undoneAt`. The newest 100 entries are kept.
Records created by an import get `createdBy` or `updatedBy` of "<name> (import)".

## Other lists (unchanged)
- **courses:** `id, name, description, level, instructor, duration, status`.
  The instructor is shown as the class's faculty.
- **academic:** `id, courseId, courseName, level, title, duration, outcome, topics[], order`.
- **facultySlots:** `id, courseId, courseName, slotType, mode, maxStudents, days[], timing,
  currentStudents, available`.
- **timetable:** legacy mapping kept for data compatibility.
- **notifLog:** `id, time, course, type, recipients[], message, link`. Every entry gets an id.
- **announcements:** `id, title, date, youtube, desc`.

## Migration (runs automatically on load: `ensureV6`)
- `college.holidays` becomes `calendarEvents` (one single-day entry each, applying to all batches).
- Payments get `batchId` and `batchName` from the student, plus `status`, `paymentDate`
  and `remarks`.
- Class logs get `shareLevel` from their old `sharedWith` list, plus a `shareToken`.
- Users get `active: true`. Notifications get ids. Default permissions are written once.
