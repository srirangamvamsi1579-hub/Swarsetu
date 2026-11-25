# Swarsetu — Music College Manager (v7)

A single-file web app (HTML, CSS and vanilla JavaScript) for running a music college:
students, batches, timetable, college calendar, attendance, classes with shared audio,
fee payments, events, and user roles with configurable permissions.

- **Live test link:** https://claude.ai/artifact/PR1cMZBjS2hc13YcXh9b5m
- **Code:** everything is in [`index.html`](index.html).
- **Docs:** [requirements](docs/requirements.md) · [data model](docs/data_model.md) ·
  [project prompt](docs/project_prompt.md) · [changelog](CHANGELOG.md)

## Sign in (sample data)

| Role | Username | Password |
|---|---|---|
| Admin | `Admin` | `Admin` |
| Faculty | `Faculty` | `Faculty` |
| Student | `Student` | `Student` |

Usernames are not case-sensitive. You can also sign in with a 10-digit mobile number.
Deactivated accounts can't sign in.

## Roles

| Role | Default access |
|---|---|
| Admin | Everything, always. Only an Admin can change Roles & Permissions or restore a backup. |
| Faculty | Add and edit students, courses, timetable, attendance, classes, class share links, fees and events. Faculty can delete classes. By default they can't open the College Profile, manage the College Calendar, manage users or delete other records. |
| Student | Read-only: their own schedule, attendance, classes shared with them, fees, the College Calendar and Events. |

Admins can change Faculty and Student permissions under **Roles & Permissions**. Some
permissions are never available to students, for example viewing other students or managing users.

## Screens

Dashboard · Students · Courses · Academic Structure · Faculty Mgmt · Timetable ·
College Calendar · Attendance (Mark Attendance, Summary) · Classes · Fee Payments ·
Notifications · Events & Recitals · Import & Export · College Profile · User Management · Roles & Permissions

## Import & Export

**Import & Export** in the menu loads existing records and downloads them again.

- **Upload:** Excel (.xlsx, .xls, .ods), CSV or text (.csv, .tsv, .txt), PDF, or a photo
  (.jpg, .png). You can also paste rows.
- **Data types:** students, courses, fee payments, attendance (one row per mark, or a
  register with one column per date), college calendar, and user logins.
- **Steps:**
  1. Choose the data type and the file.
  2. Check what was read (sheet, header row, separator).
  3. Match each app field to a column, or give a default value.
  4. Review every row. Fix values inline, then import.
- Dates, months, times, weekdays, phone numbers, amounts and attendance codes are
  converted automatically. Anything that can't be read is shown as an error, and that
  row is not imported.
- If a record already exists, you choose to skip it, update it or add it as new. Your
  column matching is remembered for next time.
- Import history lists every import, with Undo for the records it added.
- **Text PDFs** are read in the page. **Photos and scanned PDFs** are read by Claude (the
  page's `sample` capability). Values read this way are labelled, so check them before importing.
- **Download:** any list as Excel or CSV, everything in one Excel workbook, a blank Excel
  template for each data type, or the rows with errors to fix and re-upload. Admins can
  download a full JSON backup.
- Import is Admin-only by default; Faculty can download. Change this in Roles & Permissions.

## Data storage

When the page runs as a Claude artifact, data is saved to a shared database (the artifact
`db` capability). Each record is stored as its own document, and `persistAll()` sends only
what changed. Changes from other users arrive live. `localStorage` keeps an offline copy.
Opened as a plain file, the app works from `localStorage` only.

## Run locally

```bash
python3 -m http.server 8000   # then open http://localhost:8000
```

## Simulated or not yet working

- SMS, email and WhatsApp sending (notifications, password reset links, sending class
  share links) is simulated. Messages are recorded in the Sent history.
- Passwords are stored in plain text. This is a prototype; a real deployment needs hashed
  passwords and server-side authorisation.
- Permission checks run in the browser. A production build must check them on the server too.
- Downloads go through the artifact `downloads` capability. The viewer may ask the person
  to confirm each download.
- The Excel and PDF readers load from cdnjs when first used, so they need an internet connection.
- A class share link opens the class only after sign-in, and only for the people it was
  shared with. In claude.ai, the address bar may not pass the link code to the app in
  every view.
