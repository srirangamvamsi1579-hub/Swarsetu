# Swarsetu — Music College Management App (Prototype)

A click-through prototype of a music college management system, built from
[`docs/requirements.md`](docs/requirements.md). It covers all 10 requirement
areas — student management, courses, timetable, fees, attendance, exams,
faculty, events, and reports — behind a role-based login for Admin, Faculty,
Student, Parent, and Front Desk.

This is a **front-end-only demo**: there is no backend, database, or real
authentication. All data lives in memory in `app.js` and resets on reload.
It exists to validate screens and workflows before building the real backend
and porting the UI into an installable Android app — see
[`docs/summary.md`](docs/summary.md) for that roadmap.

## Run it

No build step or install required.

```bash
git clone <this-repo-url>
cd music-college-app
# then just open index.html in a browser, e.g.:
python3 -m http.server 8000
# visit http://localhost:8000
```

Or open `index.html` directly by double-clicking it.

## Try it live

If GitHub Pages is enabled for this repo (Settings → Pages → Deploy from
branch `main` / root), it will be reachable at:

```
https://srirangamvamsi1579-hub.github.io/Swarsetu/
```

## What to test
- Sign in as each role (top of the login screen) and click through every
  item in the sidebar.
- As **Admin**: add a student, record a fee payment, enter an exam grade.
- As **Faculty**: mark attendance for "my students," enter a grade.
- As **Student/Parent**: check fees, attendance, exam/report card, and
  register for an event.
- As **Front Desk**: record a pending payment.

## Project structure
```
index.html          the app shell + all styling
app.js               sample data, state, and all screen rendering/logic
docs/requirements.md  full requirements doc (source: uploaded PDF)
docs/summary.md        project status + path to the Android app
```

## Status
Prototype v0.1 — see [`docs/summary.md`](docs/summary.md) for what's built,
what's intentionally left out, and the recommended next steps toward a real
backend and an Android build (React Native/Flutter rewrite, or a
Capacitor/Cordova wrapper around this same web app).
