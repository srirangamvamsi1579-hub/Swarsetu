/* =======================================================================
   Swarsetu — Music College Management App (prototype)
   Front-end only. All data below is in-memory sample data used to
   demonstrate the workflows described in docs/requirements.md.
   ======================================================================= */

const ROLES = [
  { id: "admin", name: "Admin", desc: "Full control of the college" },
  { id: "faculty", name: "Faculty", desc: "Classes, attendance, grading" },
  { id: "student", name: "Student", desc: "Courses, schedule, fees" },
  { id: "parent", name: "Parent", desc: "Track a child's progress" },
  { id: "frontdesk", name: "Front Desk", desc: "Admissions & payments" },
];

/* ---------------------------- Sample data ---------------------------- */

const COURSES = [
  { id: "c1", name: "Piano — Classical", category: "Instrument", levels: ["Beginner", "Intermediate", "Advanced", "Trinity Gr. 5-8"] },
  { id: "c2", name: "Violin", category: "Instrument", levels: ["Beginner", "Intermediate", "ABRSM Gr. 1-5"] },
  { id: "c3", name: "Classical Guitar", category: "Instrument", levels: ["Beginner", "Intermediate"] },
  { id: "c4", name: "Hindustani Vocals", category: "Vocal", levels: ["Foundation", "Junior", "Senior"] },
  { id: "c5", name: "Western Vocals — Contemporary", category: "Vocal", levels: ["Beginner", "Intermediate"] },
  { id: "c6", name: "Tabla", category: "Instrument", levels: ["Foundation", "Junior"] },
  { id: "c7", name: "Music Theory & Composition", category: "Theory", levels: ["Level 1", "Level 2", "Level 3"] },
  { id: "c8", name: "Flute — Carnatic", category: "Instrument", levels: ["Beginner", "Intermediate"] },
];

const FACULTY = [
  { id: "f1", name: "Anil Menon", specialization: "Piano, Music Theory", qualification: "M.Mus, Trinity ATCL", classesToday: 3, attendancePct: 97 },
  { id: "f2", name: "Priya Raman", specialization: "Hindustani Vocals", qualification: "B.A. Music, Visharad", classesToday: 2, attendancePct: 100 },
  { id: "f3", name: "Marco Silva", specialization: "Classical Guitar, Violin", qualification: "B.Mus (Hons)", classesToday: 4, attendancePct: 94 },
  { id: "f4", name: "Divya Krishnan", specialization: "Western Vocals", qualification: "Rockschool Grade 8", classesToday: 1, attendancePct: 99 },
  { id: "f5", name: "Rahul Verma", specialization: "Tabla, Percussion", qualification: "Prabhakar, Sangeet Visharad", classesToday: 2, attendancePct: 96 },
];

const STUDENTS = [
  { id: "s1", name: "Ananya Iyer", course: "c1", level: "Intermediate", batch: "Sat 10:00 AM", faculty: "Anil Menon", attendancePct: 92, feeStatus: "paid", dueAmount: 0, dueDate: null, grade: "A", parent: "Suresh Iyer" },
  { id: "s2", name: "Kabir Singh", course: "c2", level: "ABRSM Gr. 2", batch: "Mon 4:00 PM", faculty: "Marco Silva", attendancePct: 78, feeStatus: "due", dueAmount: 4500, dueDate: "2026-09-15", grade: "B+", parent: "Neha Singh" },
  { id: "s3", name: "Meera Nair", course: "c4", level: "Junior", batch: "Wed 5:00 PM", faculty: "Priya Raman", attendancePct: 96, feeStatus: "paid", dueAmount: 0, dueDate: null, grade: "A+", parent: "Latha Nair" },
  { id: "s4", name: "Aditya Rao", course: "c3", level: "Beginner", batch: "Tue 6:00 PM", faculty: "Marco Silva", attendancePct: 64, feeStatus: "overdue", dueAmount: 6000, dueDate: "2026-08-28", grade: "C", parent: "Vikram Rao" },
  { id: "s5", name: "Zara Khan", course: "c5", level: "Intermediate", batch: "Thu 5:00 PM", faculty: "Divya Krishnan", attendancePct: 88, feeStatus: "paid", dueAmount: 0, dueDate: null, grade: "A-", parent: "Farhan Khan" },
  { id: "s6", name: "Rohan Das", course: "c6", level: "Foundation", batch: "Fri 4:00 PM", faculty: "Rahul Verma", attendancePct: 100, feeStatus: "due", dueAmount: 3200, dueDate: "2026-09-20", grade: "A", parent: "Ritu Das" },
  { id: "s7", name: "Ishita Gupta", course: "c7", level: "Level 1", batch: "Sat 11:00 AM", faculty: "Anil Menon", attendancePct: 85, feeStatus: "paid", dueAmount: 0, dueDate: null, grade: "B+", parent: "Sanjay Gupta" },
  { id: "s8", name: "Vivaan Pillai", course: "c8", level: "Beginner", batch: "Wed 4:00 PM", faculty: "Rahul Verma", attendancePct: 71, feeStatus: "overdue", dueAmount: 5000, dueDate: "2026-08-20", grade: "B-", parent: "Geeta Pillai" },
];

const TIMETABLE = [
  { day: "Mon", time: "4:00 PM", course: "Violin", batch: "ABRSM Gr. 2", faculty: "Marco Silva", room: "Studio 2" },
  { day: "Mon", time: "5:30 PM", course: "Piano", batch: "Advanced", faculty: "Anil Menon", room: "Studio 1" },
  { day: "Tue", time: "6:00 PM", course: "Classical Guitar", batch: "Beginner", faculty: "Marco Silva", room: "Studio 3" },
  { day: "Wed", time: "4:00 PM", course: "Flute — Carnatic", batch: "Beginner", faculty: "Rahul Verma", room: "Studio 4" },
  { day: "Wed", time: "5:00 PM", course: "Hindustani Vocals", batch: "Junior", faculty: "Priya Raman", room: "Vocal Room" },
  { day: "Thu", time: "5:00 PM", course: "Western Vocals", batch: "Intermediate", faculty: "Divya Krishnan", room: "Vocal Room" },
  { day: "Fri", time: "4:00 PM", course: "Tabla", batch: "Foundation", faculty: "Rahul Verma", room: "Percussion Rm" },
  { day: "Sat", time: "10:00 AM", course: "Piano", batch: "Intermediate", faculty: "Anil Menon", room: "Studio 1" },
  { day: "Sat", time: "11:00 AM", course: "Music Theory", batch: "Level 1", faculty: "Anil Menon", room: "Room 5" },
];
const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const TIMES = ["10:00 AM", "11:00 AM", "4:00 PM", "5:00 PM", "5:30 PM", "6:00 PM"];

const EXAMS = [
  { id: "e1", student: "Ananya Iyer", course: "Piano", type: "Internal Assessment", date: "2026-09-18", marks: 88, grade: "A" },
  { id: "e2", student: "Kabir Singh", course: "Violin", type: "ABRSM Board Exam", date: "2026-10-02", marks: null, grade: null },
  { id: "e3", student: "Meera Nair", course: "Hindustani Vocals", type: "Internal Assessment", date: "2026-09-12", marks: 95, grade: "A+" },
  { id: "e4", student: "Aditya Rao", course: "Classical Guitar", type: "Internal Assessment", date: "2026-09-12", marks: 61, grade: "C" },
  { id: "e5", student: "Zara Khan", course: "Western Vocals", type: "Recital Assessment", date: "2026-09-25", marks: 90, grade: "A-" },
];

const EVENTS = [
  { id: "ev1", name: "Autumn Recital Night", type: "Recital", date: "2026-10-10", venue: "Main Auditorium", registered: 42, capacity: 80 },
  { id: "ev2", name: "Hindustani Vocals Masterclass", type: "Masterclass", date: "2026-09-21", venue: "Vocal Room", registered: 18, capacity: 25 },
  { id: "ev3", name: "Inter-Batch Guitar Competition", type: "Competition", date: "2026-11-02", venue: "Studio 3", registered: 12, capacity: 20 },
  { id: "ev4", name: "Winter Concert Workshop", type: "Workshop", date: "2026-12-05", venue: "Main Auditorium", registered: 5, capacity: 60 },
];

const REVENUE_TREND = [
  { month: "Apr", amount: 412000 },
  { month: "May", amount: 438000 },
  { month: "Jun", amount: 395000 },
  { month: "Jul", amount: 461000 },
  { month: "Aug", amount: 487000 },
  { month: "Sep", amount: 452000 },
];

/* ------------------------------- State -------------------------------- */

const state = {
  role: null,
  userName: "",
  view: null,
  attendanceDraft: {}, // studentId -> P/A/L for the attendance screen
  selectedStudentId: null,
};

function courseName(id) {
  const c = COURSES.find((x) => x.id === id);
  return c ? c.name : id;
}
function initials(name) {
  return name.split(" ").map((p) => p[0]).slice(0, 2).join("").toUpperCase();
}
function money(n) {
  return "₹" + n.toLocaleString("en-IN");
}
function feePill(status) {
  if (status === "paid") return '<span class="pill green">Paid</span>';
  if (status === "due") return '<span class="pill amber">Due soon</span>';
  return '<span class="pill red">Overdue</span>';
}

/* ------------------------------- Login -------------------------------- */

const roleGrid = document.getElementById("role-grid");
let chosenRole = "admin";
ROLES.forEach((r, i) => {
  const btn = document.createElement("button");
  btn.className = "role-btn" + (i === 0 ? " active" : "");
  btn.dataset.role = r.id;
  btn.innerHTML = `<span class="rname">${r.name}</span><span class="rdesc">${r.desc}</span>`;
  btn.addEventListener("click", () => {
    chosenRole = r.id;
    document.querySelectorAll(".role-btn").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
  });
  roleGrid.appendChild(btn);
});

const DEFAULT_NAMES = {
  admin: "Sunita Rao",
  faculty: "Anil Menon",
  student: "Ananya Iyer",
  parent: "Suresh Iyer",
  frontdesk: "Kavya Menon",
};

document.getElementById("loginBtn").addEventListener("click", () => {
  const typed = document.getElementById("loginName").value.trim();
  state.role = chosenRole;
  state.userName = typed || DEFAULT_NAMES[chosenRole];
  document.getElementById("login-screen").style.display = "none";
  document.getElementById("app-screen").style.display = "block";
  bootRole();
});

document.getElementById("signOutBtn").addEventListener("click", () => {
  document.getElementById("app-screen").style.display = "none";
  document.getElementById("login-screen").style.display = "flex";
  document.getElementById("loginName").value = "";
});

/* ------------------------------ Nav / shell ----------------------------- */

const NAV_BY_ROLE = {
  admin: [
    { id: "dashboard", label: "Dashboard", ic: "◆" },
    { id: "students", label: "Students", ic: "◇" },
    { id: "courses", label: "Courses", ic: "♪" },
    { id: "timetable", label: "Timetable", ic: "▤" },
    { id: "fees", label: "Fees & Payments", ic: "$" },
    { id: "attendance", label: "Attendance", ic: "✓" },
    { id: "exams", label: "Exams", ic: "✎" },
    { id: "faculty", label: "Faculty", ic: "☺" },
    { id: "events", label: "Events & Recitals", ic: "★" },
    { id: "reports", label: "Reports & Analytics", ic: "▦" },
  ],
  faculty: [
    { id: "dashboard", label: "Dashboard", ic: "◆" },
    { id: "attendance", label: "Mark Attendance", ic: "✓" },
    { id: "students", label: "My Students", ic: "◇" },
    { id: "timetable", label: "My Timetable", ic: "▤" },
    { id: "exams", label: "Grading", ic: "✎" },
    { id: "events", label: "Events & Recitals", ic: "★" },
  ],
  student: [
    { id: "dashboard", label: "Dashboard", ic: "◆" },
    { id: "courses", label: "My Courses", ic: "♪" },
    { id: "timetable", label: "Timetable", ic: "▤" },
    { id: "fees", label: "Fees", ic: "$" },
    { id: "attendance", label: "My Attendance", ic: "✓" },
    { id: "exams", label: "Exams & Report Card", ic: "✎" },
    { id: "events", label: "Events & Recitals", ic: "★" },
  ],
  parent: [
    { id: "dashboard", label: "Dashboard", ic: "◆" },
    { id: "fees", label: "Fees", ic: "$" },
    { id: "attendance", label: "Attendance", ic: "✓" },
    { id: "exams", label: "Exams & Report Card", ic: "✎" },
  ],
  frontdesk: [
    { id: "dashboard", label: "Dashboard", ic: "◆" },
    { id: "students", label: "Admissions", ic: "◇" },
    { id: "fees", label: "Fees & Payments", ic: "$" },
    { id: "events", label: "Events & Recitals", ic: "★" },
  ],
};

const ROLE_TAG = {
  admin: "ADMIN CONSOLE", faculty: "FACULTY PORTAL", student: "STUDENT PORTAL",
  parent: "PARENT PORTAL", frontdesk: "FRONT DESK",
};

function bootRole() {
  document.getElementById("sidebarRoleTag").textContent = ROLE_TAG[state.role];
  document.getElementById("whoName").textContent = state.userName;
  const nav = document.getElementById("navlist");
  nav.innerHTML = "";
  NAV_BY_ROLE[state.role].forEach((item, idx) => {
    const b = document.createElement("button");
    b.className = "navbtn" + (idx === 0 ? " active" : "");
    b.dataset.view = item.id;
    b.innerHTML = `<span class="ic">${item.ic}</span> ${item.label}`;
    b.addEventListener("click", () => setView(item.id));
    nav.appendChild(b);
  });
  setView("dashboard");
}

function setView(view) {
  state.view = view;
  document.querySelectorAll("#navlist button").forEach((b) => {
    b.classList.toggle("active", b.dataset.view === view);
  });
  render();
}

function render() {
  const main = document.getElementById("main");
  const renderers = {
    dashboard: renderDashboard,
    students: renderStudents,
    courses: renderCourses,
    timetable: renderTimetable,
    fees: renderFees,
    attendance: renderAttendance,
    exams: renderExams,
    faculty: renderFaculty,
    events: renderEvents,
    reports: renderReports,
  };
  const fn = renderers[state.view] || renderDashboard;
  main.innerHTML = fn();
  attachHandlersFor(state.view);
}

function topbar(title, crumb) {
  return `<div class="topbar"><h1>${title}</h1><div class="breadcrumb">${crumb}</div></div>`;
}

/* ------------------------------ Dashboard ------------------------------ */

function renderDashboard() {
  if (state.role === "admin") return dashAdmin();
  if (state.role === "faculty") return dashFaculty();
  if (state.role === "student") return dashStudent();
  if (state.role === "parent") return dashParent();
  return dashFrontDesk();
}

function dashAdmin() {
  const overdue = STUDENTS.filter((s) => s.feeStatus === "overdue").length;
  const avgAtt = Math.round(STUDENTS.reduce((a, s) => a + s.attendancePct, 0) / STUDENTS.length);
  const monthRevenue = REVENUE_TREND[REVENUE_TREND.length - 1].amount;
  return `
    ${topbar("Good morning, " + state.userName.split(" ")[0], "Admin · Dashboard")}
    <div class="grid cols-4" style="margin-bottom:18px;">
      <div class="stat"><div class="num">${STUDENTS.length}</div><div class="label">Active students</div><div class="delta up">+2 this month</div></div>
      <div class="stat"><div class="num">${FACULTY.length}</div><div class="label">Faculty on roll</div><div class="delta up">stable</div></div>
      <div class="stat"><div class="num">${avgAtt}%</div><div class="label">Avg. attendance</div><div class="delta down">-3% vs last month</div></div>
      <div class="stat"><div class="num">${overdue}</div><div class="label">Overdue fee accounts</div><div class="delta down">needs follow-up</div></div>
    </div>
    <div class="grid cols-2">
      <div class="panel">
        <h3>Revenue trend</h3>
        <p class="panel-sub">Last 6 months, all courses</p>
        ${revenueBars()}
      </div>
      <div class="panel">
        <h3>Needs attention</h3>
        <p class="panel-sub">Fee dues and low attendance</p>
        <table><tbody>
          ${STUDENTS.filter((s) => s.feeStatus !== "paid" || s.attendancePct < 75).map((s) => `
            <tr>
              <td><div class="name-cell"><span class="avatar">${initials(s.name)}</span>${s.name}</div></td>
              <td>${feePill(s.feeStatus)}</td>
              <td>${s.attendancePct < 75 ? '<span class="pill red">' + s.attendancePct + '% attend.</span>' : '<span class="sub-text">' + s.attendancePct + '% attend.</span>'}</td>
            </tr>`).join("")}
        </tbody></table>
      </div>
    </div>
    <div class="panel" style="margin-top:16px;">
      <h3>Today's classes</h3>
      <p class="panel-sub">${new Date().toDateString()}</p>
      ${todayScheduleTable()}
    </div>
    ${appFooter()}
  `;
}

function revenueBars() {
  const max = Math.max(...REVENUE_TREND.map((r) => r.amount));
  return REVENUE_TREND.map((r) => `
    <div class="bar-row">
      <div class="blabel">${r.month}</div>
      <div class="bar-track"><div class="bar-fill" style="width:${Math.round((r.amount / max) * 100)}%"></div></div>
      <div class="bar-val">${money(r.amount)}</div>
    </div>`).join("");
}

function todayScheduleTable() {
  const dayIdx = new Date().getDay(); // 0 sun..6 sat
  const dayMap = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const today = dayMap[dayIdx];
  const rows = TIMETABLE.filter((t) => t.day === today);
  const list = rows.length ? rows : TIMETABLE.slice(0, 4);
  return `<table><thead><tr><th>Time</th><th>Course</th><th>Batch</th><th>Faculty</th><th>Room</th></tr></thead><tbody>
    ${list.map((t) => `<tr><td>${t.time}</td><td>${t.course}</td><td>${t.batch}</td><td>${t.faculty}</td><td>${t.room}</td></tr>`).join("")}
  </tbody></table>`;
}

function dashFaculty() {
  const mine = STUDENTS.filter((s) => s.faculty === state.userName);
  const myClasses = TIMETABLE.filter((t) => t.faculty === state.userName);
  return `
    ${topbar("Welcome back, " + state.userName.split(" ")[0], "Faculty · Dashboard")}
    <div class="grid cols-3" style="margin-bottom:18px;">
      <div class="stat"><div class="num">${myClasses.length}</div><div class="label">Classes this week</div></div>
      <div class="stat"><div class="num">${mine.length}</div><div class="label">Students taught</div></div>
      <div class="stat"><div class="num">${mine.length ? Math.round(mine.reduce((a,s)=>a+s.attendancePct,0)/mine.length) : 0}%</div><div class="label">Avg. attendance</div></div>
    </div>
    <div class="panel">
      <h3>Today's classes</h3>
      <p class="panel-sub">Tap "Mark attendance" from the sidebar to take the register</p>
      ${todayScheduleTable()}
    </div>
    <div class="panel" style="margin-top:16px;">
      <h3>My students</h3>
      ${studentsMiniTable(mine)}
    </div>
    ${appFooter()}
  `;
}

function studentsMiniTable(list) {
  if (!list.length) return `<div class="empty">No students assigned yet.</div>`;
  return `<table><thead><tr><th>Student</th><th>Course</th><th>Batch</th><th>Attendance</th><th>Grade</th></tr></thead><tbody>
    ${list.map((s) => `<tr>
      <td><div class="name-cell"><span class="avatar">${initials(s.name)}</span>${s.name}</div></td>
      <td>${courseName(s.course)}</td><td>${s.batch}</td>
      <td>${s.attendancePct}%</td><td>${s.grade}</td>
    </tr>`).join("")}
  </tbody></table>`;
}

function dashStudent() {
  const me = STUDENTS.find((s) => s.name === state.userName) || STUDENTS[0];
  return `
    ${topbar("Hi, " + me.name.split(" ")[0], "Student · Dashboard")}
    <div class="grid cols-4" style="margin-bottom:18px;">
      <div class="stat"><div class="num">${me.attendancePct}%</div><div class="label">Attendance</div></div>
      <div class="stat"><div class="num">${me.grade}</div><div class="label">Current grade</div></div>
      <div class="stat"><div class="num">${money(me.dueAmount)}</div><div class="label">Fees due</div></div>
      <div class="stat"><div class="num">${me.level}</div><div class="label">${courseName(me.course)}</div></div>
    </div>
    <div class="grid cols-2">
      <div class="panel">
        <h3>Fee status</h3>
        <p class="panel-sub">${me.dueDate ? "Due " + me.dueDate : "No pending dues"}</p>
        <div>${feePill(me.feeStatus)}</div>
      </div>
      <div class="panel">
        <h3>Your batch</h3>
        <p class="panel-sub">${me.batch} with ${me.faculty}</p>
      </div>
    </div>
    <div class="panel" style="margin-top:16px;">
      <h3>Upcoming events</h3>
      ${eventsMiniList()}
    </div>
    ${appFooter()}
  `;
}

function dashParent() {
  const child = STUDENTS.find((s) => s.parent === state.userName) || STUDENTS.find((s) => s.parent) || STUDENTS[0];
  return `
    ${topbar(state.userName.split(" ")[0] + "'s dashboard", "Parent · " + child.name)}
    <div class="grid cols-4" style="margin-bottom:18px;">
      <div class="stat"><div class="num">${child.attendancePct}%</div><div class="label">Attendance</div></div>
      <div class="stat"><div class="num">${child.grade}</div><div class="label">Current grade</div></div>
      <div class="stat"><div class="num">${money(child.dueAmount)}</div><div class="label">Fees due</div></div>
      <div class="stat"><div class="num">${courseName(child.course)}</div><div class="label">${child.level}</div></div>
    </div>
    <div class="panel">
      <h3>${child.name}'s progress</h3>
      <p class="panel-sub">Taught by ${child.faculty} · ${child.batch}</p>
      <div class="bar-row"><div class="blabel">Attendance</div><div class="bar-track"><div class="bar-fill" style="width:${child.attendancePct}%"></div></div><div class="bar-val">${child.attendancePct}%</div></div>
    </div>
    <div class="panel" style="margin-top:16px;"><h3>Fee status</h3>${feePill(child.feeStatus)} ${child.dueDate ? '<span class="sub-text">Due ' + child.dueDate + '</span>' : ""}</div>
    ${appFooter()}
  `;
}

function dashFrontDesk() {
  const pending = STUDENTS.filter((s) => s.feeStatus !== "paid");
  return `
    ${topbar("Front Desk", "Admissions & payments overview")}
    <div class="grid cols-3" style="margin-bottom:18px;">
      <div class="stat"><div class="num">${STUDENTS.length}</div><div class="label">Total admissions</div></div>
      <div class="stat"><div class="num">${pending.length}</div><div class="label">Pending payments</div></div>
      <div class="stat"><div class="num">${EVENTS.length}</div><div class="label">Upcoming events</div></div>
    </div>
    <div class="panel">
      <h3>Pending payments</h3>
      ${studentsFeesMiniTable(pending)}
    </div>
    ${appFooter()}
  `;
}

function studentsFeesMiniTable(list) {
  return `<table><thead><tr><th>Student</th><th>Amount due</th><th>Due date</th><th>Status</th><th></th></tr></thead><tbody>
    ${list.map((s) => `<tr>
      <td><div class="name-cell"><span class="avatar">${initials(s.name)}</span>${s.name}</div></td>
      <td>${money(s.dueAmount)}</td><td>${s.dueDate || "—"}</td><td>${feePill(s.feeStatus)}</td>
      <td><button class="btn sm" data-collect="${s.id}">Record payment</button></td>
    </tr>`).join("")}
  </tbody></table>`;
}

function eventsMiniList() {
  return `<table><thead><tr><th>Event</th><th>Date</th><th>Venue</th></tr></thead><tbody>
    ${EVENTS.map((e) => `<tr><td>${e.name}</td><td>${e.date}</td><td>${e.venue}</td></tr>`).join("")}
  </tbody></table>`;
}

function appFooter() {
  return `<footer class="appfoot">Prototype data only — nothing here is saved to a server. Reload to reset.</footer>`;
}

/* ------------------------------ Students -------------------------------- */

function renderStudents() {
  const canAdd = state.role === "admin" || state.role === "frontdesk";
  let list = STUDENTS;
  if (state.role === "faculty") list = STUDENTS.filter((s) => s.faculty === state.userName);

  if (state.selectedStudentId) {
    return renderStudentProfile(state.selectedStudentId);
  }

  return `
    ${topbar(state.role === "faculty" ? "My Students" : "Students", (canAdd ? "Admissions & enrollment · " : "") + list.length + " records")}
    <div class="toolbar">
      <div class="left">
        <input class="search" placeholder="Search students…" id="studentSearch" />
        <select class="select" id="courseFilter">
          <option value="">All courses</option>
          ${COURSES.map((c) => `<option value="${c.id}">${c.name}</option>`).join("")}
        </select>
      </div>
      ${canAdd ? '<button class="btn solid" id="addStudentBtn">+ Add student</button>' : ""}
    </div>
    <div class="panel" style="padding:0;">
      <table>
        <thead><tr><th>Student</th><th>Course / Level</th><th>Batch</th><th>Attendance</th><th>Fee status</th><th>Grade</th><th></th></tr></thead>
        <tbody id="studentsTbody">${studentRows(list)}</tbody>
      </table>
    </div>
    ${appFooter()}
  `;
}

function studentRows(list) {
  if (!list.length) return `<tr><td colspan="7"><div class="empty">No students match.</div></td></tr>`;
  return list.map((s) => `
    <tr data-row-id="${s.id}">
      <td><div class="name-cell"><span class="avatar">${initials(s.name)}</span>${s.name}</div></td>
      <td>${courseName(s.course)}<div class="sub-text">${s.level}</div></td>
      <td>${s.batch}</td>
      <td>${s.attendancePct}%</td>
      <td>${feePill(s.feeStatus)}</td>
      <td>${s.grade}</td>
      <td><button class="btn sm" data-view-student="${s.id}">View profile</button></td>
    </tr>`).join("");
}

function renderStudentProfile(id) {
  const s = STUDENTS.find((x) => x.id === id);
  return `
    ${topbar(s.name, `<a href="#" data-back-students="1">Students</a> / Profile`)}
    <div class="grid cols-2" style="margin-bottom:16px;">
      <div class="panel">
        <h3>Profile</h3>
        <table><tbody>
          <tr><td class="sub-text">Course</td><td>${courseName(s.course)}</td></tr>
          <tr><td class="sub-text">Level</td><td>${s.level}</td></tr>
          <tr><td class="sub-text">Batch</td><td>${s.batch}</td></tr>
          <tr><td class="sub-text">Faculty</td><td>${s.faculty}</td></tr>
          <tr><td class="sub-text">Parent/Guardian</td><td>${s.parent}</td></tr>
        </tbody></table>
      </div>
      <div class="panel">
        <h3>Status</h3>
        <div class="bar-row"><div class="blabel">Attendance</div><div class="bar-track"><div class="bar-fill" style="width:${s.attendancePct}%"></div></div><div class="bar-val">${s.attendancePct}%</div></div>
        <p style="margin:14px 0 6px;" class="sub-text">Fee status</p>
        ${feePill(s.feeStatus)} ${s.dueAmount ? `<span class="sub-text">${money(s.dueAmount)} due ${s.dueDate}</span>` : ""}
        <p style="margin:14px 0 6px;" class="sub-text">Current grade</p>
        <span class="pill amber">${s.grade}</span>
      </div>
    </div>
    <div class="panel">
      <h3>Certificates & ID</h3>
      <p class="panel-sub">Generate documents for this student</p>
      <div class="badge-row">
        <button class="btn sm">Generate ID card</button>
        <button class="btn sm">Generate certificate</button>
        <button class="btn sm">Download attendance report</button>
      </div>
    </div>
    ${appFooter()}
  `;
}

function openAddStudentModal() {
  openModal(`
    <h3>Add student</h3>
    <p class="panel-sub">Admission & enrollment</p>
    <div class="field"><label>Full name</label><input type="text" id="newStudentName" placeholder="e.g. Priyanka Bose" /></div>
    <div class="field"><label>Course</label>
      <select class="select" style="width:100%" id="newStudentCourse">${COURSES.map((c) => `<option value="${c.id}">${c.name}</option>`).join("")}</select>
    </div>
    <div class="field"><label>Level</label><input type="text" id="newStudentLevel" placeholder="e.g. Beginner" /></div>
    <div class="field"><label>Batch (day & time)</label><input type="text" id="newStudentBatch" placeholder="e.g. Sat 9:00 AM" /></div>
    <div class="field"><label>Assigned faculty</label>
      <select class="select" style="width:100%" id="newStudentFaculty">${FACULTY.map((f) => `<option value="${f.name}">${f.name}</option>`).join("")}</select>
    </div>
    <div class="modal-actions">
      <button class="btn ghost" id="cancelModal">Cancel</button>
      <button class="btn solid" id="saveNewStudent">Enroll student</button>
    </div>
  `);
}

/* ------------------------------- Courses -------------------------------- */

function renderCourses() {
  const canAdd = state.role === "admin";
  return `
    ${topbar("Course & Curriculum", COURSES.length + " courses in the catalog")}
    <div class="toolbar">
      <div class="left"><input class="search" placeholder="Search courses…" /></div>
      ${canAdd ? '<button class="btn solid" id="addCourseBtn">+ Add course</button>' : ""}
    </div>
    <div class="grid cols-3">
      ${COURSES.map((c) => `
        <div class="panel">
          <h3>${c.name}</h3>
          <p class="panel-sub">${c.category}</p>
          <div class="badge-row" style="margin-bottom:12px;">
            ${c.levels.map((l) => `<span class="pill gray">${l}</span>`).join("")}
          </div>
          <div class="sub-text">${STUDENTS.filter((s) => s.course === c.id).length} students enrolled</div>
          <div style="margin-top:10px;"><button class="btn sm">View syllabus</button></div>
        </div>
      `).join("")}
    </div>
    ${appFooter()}
  `;
}

/* ------------------------------ Timetable -------------------------------- */

function renderTimetable() {
  let rows = TIMETABLE;
  if (state.role === "faculty") rows = TIMETABLE.filter((t) => t.faculty === state.userName);
  if (state.role === "student") {
    const me = STUDENTS.find((s) => s.name === state.userName) || STUDENTS[0];
    rows = TIMETABLE.filter((t) => t.faculty === me.faculty);
  }
  const byDayTime = {};
  rows.forEach((t) => { byDayTime[t.day + "|" + t.time] = t; });

  return `
    ${topbar("Timetable", "Weekly view · room & batch conflicts auto-checked")}
    <div class="panel">
      <div class="weekgrid">
        <div class="hcell"></div>
        ${DAYS.map((d) => `<div class="hcell">${d}</div>`).join("")}
        ${TIMES.map((t) => `
          <div class="tcell">${t}</div>
          ${DAYS.map((d) => {
            const slot = byDayTime[d + "|" + t];
            return `<div class="slotcell">${slot ? `<div class="slot"><span class="sc">${slot.course}</span><span class="sm">${slot.batch} · ${slot.room}</span></div>` : ""}</div>`;
          }).join("")}
        `).join("")}
      </div>
    </div>
    <div class="panel" style="margin-top:16px;">
      <h3>Room & studio bookings</h3>
      <p class="panel-sub">No conflicts detected for this week</p>
      <span class="pill green">All clear</span>
    </div>
    ${appFooter()}
  `;
}

/* -------------------------------- Fees ----------------------------------- */

function renderFees() {
  if (state.role === "student" || state.role === "parent") {
    const person = state.role === "student"
      ? (STUDENTS.find((s) => s.name === state.userName) || STUDENTS[0])
      : (STUDENTS.find((s) => s.parent === state.userName) || STUDENTS[0]);
    return `
      ${topbar("Fees", state.role === "parent" ? person.name + "'s account" : "Your account")}
      <div class="grid cols-3" style="margin-bottom:16px;">
        <div class="stat"><div class="num">${money(person.dueAmount)}</div><div class="label">Amount due</div></div>
        <div class="stat"><div class="num">${person.dueDate || "—"}</div><div class="label">Due date</div></div>
        <div class="stat">${feePill(person.feeStatus)}<div class="label" style="margin-top:8px;">Status</div></div>
      </div>
      <div class="panel">
        <h3>Payment history</h3>
        <table><thead><tr><th>Date</th><th>Description</th><th>Amount</th><th>Status</th></tr></thead><tbody>
          <tr><td>05 Aug 2026</td><td>Term fee — ${courseName(person.course)}</td><td>${money(18000)}</td><td><span class="pill green">Paid</span></td></tr>
          <tr><td>05 May 2026</td><td>Term fee — ${courseName(person.course)}</td><td>${money(18000)}</td><td><span class="pill green">Paid</span></td></tr>
          ${person.dueAmount ? `<tr><td>${person.dueDate}</td><td>Term fee — ${courseName(person.course)}</td><td>${money(person.dueAmount)}</td><td>${feePill(person.feeStatus)}</td></tr>` : ""}
        </tbody></table>
      </div>
      ${person.dueAmount ? '<div style="margin-top:14px;"><button class="btn solid" id="payNowBtn">Pay now</button></div>' : ""}
      ${appFooter()}
    `;
  }

  // admin / frontdesk view
  const totalDue = STUDENTS.reduce((a, s) => a + s.dueAmount, 0);
  const overdue = STUDENTS.filter((s) => s.feeStatus === "overdue");
  return `
    ${topbar("Fees & Payments", "Dues, due dates, and payment history")}
    <div class="grid cols-3" style="margin-bottom:16px;">
      <div class="stat"><div class="num">${money(totalDue)}</div><div class="label">Total outstanding</div></div>
      <div class="stat"><div class="num">${overdue.length}</div><div class="label">Overdue accounts</div></div>
      <div class="stat"><div class="num">${money(REVENUE_TREND[REVENUE_TREND.length - 1].amount)}</div><div class="label">Collected this month</div></div>
    </div>
    <div class="panel" style="padding:0;">
      <table>
        <thead><tr><th>Student</th><th>Course</th><th>Amount due</th><th>Due date</th><th>Status</th><th></th></tr></thead>
        <tbody>
          ${STUDENTS.map((s) => `<tr>
            <td><div class="name-cell"><span class="avatar">${initials(s.name)}</span>${s.name}</div></td>
            <td>${courseName(s.course)}</td>
            <td>${s.dueAmount ? money(s.dueAmount) : "—"}</td>
            <td>${s.dueDate || "—"}</td>
            <td>${feePill(s.feeStatus)}</td>
            <td>${s.dueAmount ? `<button class="btn sm" data-collect="${s.id}">Record payment</button>` : `<button class="btn sm" data-invoice="${s.id}">Send invoice</button>`}</td>
          </tr>`).join("")}
        </tbody>
      </table>
    </div>
    ${appFooter()}
  `;
}

/* ----------------------------- Attendance --------------------------------- */

function renderAttendance() {
  if (state.role === "faculty") {
    const mine = STUDENTS.filter((s) => s.faculty === state.userName);
    return `
      ${topbar("Mark Attendance", "Today's session · " + (mine[0] ? mine[0].batch : ""))}
      <div class="toolbar">
        <div class="left">
          <select class="select"><option>${mine[0] ? mine[0].batch : "No batch"}</option></select>
        </div>
        <button class="btn solid" id="saveAttendanceBtn">Save attendance</button>
      </div>
      <div class="att-grid">
        ${mine.map((s) => attendanceCard(s)).join("")}
      </div>
      ${appFooter()}
    `;
  }

  if (state.role === "student" || state.role === "parent") {
    const person = state.role === "student"
      ? (STUDENTS.find((s) => s.name === state.userName) || STUDENTS[0])
      : (STUDENTS.find((s) => s.parent === state.userName) || STUDENTS[0]);
    return `
      ${topbar("Attendance", state.role === "parent" ? person.name + "'s record" : "Your record")}
      <div class="panel" style="max-width:480px;">
        <div class="bar-row"><div class="blabel">Overall</div><div class="bar-track"><div class="bar-fill" style="width:${person.attendancePct}%"></div></div><div class="bar-val">${person.attendancePct}%</div></div>
      </div>
      <div class="panel" style="margin-top:16px;">
        <h3>Recent sessions</h3>
        <table><thead><tr><th>Date</th><th>Class</th><th>Status</th></tr></thead><tbody>
          <tr><td>02 Sep 2026</td><td>${courseName(person.course)}</td><td><span class="pill green">Present</span></td></tr>
          <tr><td>26 Aug 2026</td><td>${courseName(person.course)}</td><td><span class="pill green">Present</span></td></tr>
          <tr><td>19 Aug 2026</td><td>${courseName(person.course)}</td><td><span class="pill red">Absent</span></td></tr>
          <tr><td>12 Aug 2026</td><td>${courseName(person.course)}</td><td><span class="pill amber">Late</span></td></tr>
        </tbody></table>
      </div>
      ${appFooter()}
    `;
  }

  // admin roll-up
  return `
    ${topbar("Attendance", "Roll-up across all batches")}
    <div class="panel" style="padding:0;">
      <table>
        <thead><tr><th>Student</th><th>Batch</th><th>Faculty</th><th>Attendance</th></tr></thead>
        <tbody>
          ${STUDENTS.map((s) => `<tr>
            <td><div class="name-cell"><span class="avatar">${initials(s.name)}</span>${s.name}</div></td>
            <td>${s.batch}</td><td>${s.faculty}</td>
            <td><div class="bar-row" style="margin:0;"><div class="bar-track" style="width:100px;"><div class="bar-fill" style="width:${s.attendancePct}%; ${s.attendancePct < 75 ? 'background:var(--red);' : ''}"></div></div><div class="bar-val">${s.attendancePct}%</div></div></td>
          </tr>`).join("")}
        </tbody>
      </table>
    </div>
    ${appFooter()}
  `;
}

function attendanceCard(s) {
  const cur = state.attendanceDraft[s.id] || "p";
  return `
    <div class="att-card">
      <div class="an">${s.name}</div>
      <div class="seg" data-student="${s.id}">
        <button data-mark="p" class="${cur === "p" ? "on p" : ""}">Present</button>
        <button data-mark="a" class="${cur === "a" ? "on a" : ""}">Absent</button>
        <button data-mark="l" class="${cur === "l" ? "on l" : ""}">Late</button>
      </div>
    </div>`;
}

/* -------------------------------- Exams ------------------------------------ */

function renderExams() {
  if (state.role === "student" || state.role === "parent") {
    const person = state.role === "student"
      ? (STUDENTS.find((s) => s.name === state.userName) || STUDENTS[0])
      : (STUDENTS.find((s) => s.parent === state.userName) || STUDENTS[0]);
    const mine = EXAMS.filter((e) => e.student === person.name);
    return `
      ${topbar("Exams & Report Card", person.name)}
      <div class="panel">
        <h3>Overall grade: <span class="pill amber">${person.grade}</span></h3>
        <p class="panel-sub">Based on internal assessments and recitals</p>
        <table><thead><tr><th>Course</th><th>Type</th><th>Date</th><th>Marks</th><th>Grade</th></tr></thead><tbody>
          ${mine.length ? mine.map((e) => `<tr><td>${e.course}</td><td>${e.type}</td><td>${e.date}</td><td>${e.marks ?? "—"}</td><td>${e.grade ? `<span class="pill amber">${e.grade}</span>` : '<span class="pill gray">Upcoming</span>'}</td></tr>`).join("") : `<tr><td colspan="5"><div class="empty">No exams recorded yet.</div></td></tr>`}
        </tbody></table>
      </div>
      ${appFooter()}
    `;
  }

  const canGrade = state.role === "faculty" || state.role === "admin";
  return `
    ${topbar(canGrade && state.role === "faculty" ? "Grading" : "Examination & Assessment", "Internal assessments & board exams")}
    <div class="toolbar"><div class="left"><input class="search" placeholder="Search by student or course…" /></div>
      <button class="btn solid" id="scheduleExamBtn">+ Schedule exam</button>
    </div>
    <div class="panel" style="padding:0;">
      <table>
        <thead><tr><th>Student</th><th>Course</th><th>Type</th><th>Date</th><th>Marks</th><th>Grade</th></tr></thead>
        <tbody>
          ${EXAMS.map((e) => `<tr>
            <td>${e.student}</td><td>${e.course}</td><td>${e.type}</td><td>${e.date}</td>
            <td>${e.marks ?? "—"}</td>
            <td>${e.grade ? `<span class="pill amber">${e.grade}</span>` : (canGrade ? `<button class="btn sm" data-grade="${e.id}">Enter grade</button>` : '<span class="pill gray">Pending</span>')}</td>
          </tr>`).join("")}
        </tbody>
      </table>
    </div>
    ${appFooter()}
  `;
}

/* ------------------------------- Faculty ------------------------------------ */

function renderFaculty() {
  return `
    ${topbar("Faculty Management", FACULTY.length + " faculty on roll")}
    <div class="toolbar"><div class="left"><input class="search" placeholder="Search faculty…" /></div>
      ${state.role === "admin" ? '<button class="btn solid" id="addFacultyBtn">+ Add faculty</button>' : ""}
    </div>
    <div class="grid cols-3">
      ${FACULTY.map((f) => `
        <div class="panel">
          <div class="name-cell" style="margin-bottom:8px;"><span class="avatar">${initials(f.name)}</span><div><div style="font-weight:600;">${f.name}</div><div class="sub-text">${f.qualification}</div></div></div>
          <p class="panel-sub" style="margin-bottom:10px;">${f.specialization}</p>
          <div class="badge-row" style="margin-bottom:10px;">
            <span class="pill gray">${f.classesToday} classes today</span>
            <span class="pill green">${f.attendancePct}% attendance</span>
          </div>
          <button class="btn sm">View timetable</button>
        </div>
      `).join("")}
    </div>
    ${appFooter()}
  `;
}

/* -------------------------------- Events -------------------------------------- */

function renderEvents() {
  return `
    ${topbar("Events & Recitals", "Concerts, workshops, and competitions")}
    <div class="toolbar"><div class="left"><input class="search" placeholder="Search events…" /></div>
      ${state.role === "admin" ? '<button class="btn solid" id="addEventBtn">+ New event</button>' : ""}
    </div>
    <div class="grid cols-2">
      ${EVENTS.map((e) => `
        <div class="panel">
          <div class="topbar" style="margin-bottom:8px;"><h3 style="font-size:1.02rem;">${e.name}</h3><span class="pill amber">${e.type}</span></div>
          <p class="panel-sub">${e.date} · ${e.venue}</p>
          <div class="bar-row"><div class="blabel">${e.registered}/${e.capacity}</div><div class="bar-track"><div class="bar-fill" style="width:${Math.round((e.registered/e.capacity)*100)}%"></div></div></div>
          <div style="margin-top:10px;">
            ${state.role === "student" || state.role === "parent" ? `<button class="btn sm solid" data-register="${e.id}">Register</button>` : `<button class="btn sm">View registrations</button>`}
            <button class="btn sm ghost">Media gallery</button>
          </div>
        </div>
      `).join("")}
    </div>
    ${appFooter()}
  `;
}

/* ------------------------------- Reports -------------------------------------- */

function renderReports() {
  const byCourse = COURSES.map((c) => ({
    name: c.name,
    count: STUDENTS.filter((s) => s.course === c.id).length,
  })).filter((c) => c.count > 0);
  const maxCount = Math.max(...byCourse.map((c) => c.count));

  return `
    ${topbar("Reports & Analytics", "Attendance, financial, and performance reports")}
    <div class="grid cols-2" style="margin-bottom:16px;">
      <div class="panel">
        <h3>Revenue trend</h3>
        <p class="panel-sub">Last 6 months</p>
        ${revenueBars()}
      </div>
      <div class="panel">
        <h3>Enrollment by course</h3>
        <p class="panel-sub">Active students per course</p>
        ${byCourse.map((c) => `
          <div class="bar-row">
            <div class="blabel" style="width:150px;">${c.name}</div>
            <div class="bar-track"><div class="bar-fill" style="width:${Math.round((c.count/maxCount)*100)}%"></div></div>
            <div class="bar-val">${c.count}</div>
          </div>`).join("")}
      </div>
    </div>
    <div class="panel">
      <h3>Downloadable reports</h3>
      <p class="panel-sub">Generated from current data</p>
      <table><thead><tr><th>Report</th><th>Covers</th><th></th></tr></thead><tbody>
        <tr><td>Attendance summary</td><td>All batches, this term</td><td><button class="btn sm">Download</button></td></tr>
        <tr><td>Financial report</td><td>Revenue, dues, expenses</td><td><button class="btn sm">Download</button></td></tr>
        <tr><td>Student performance</td><td>Grades & progress, this term</td><td><button class="btn sm">Download</button></td></tr>
        <tr><td>Faculty workload</td><td>Classes & hours, this term</td><td><button class="btn sm">Download</button></td></tr>
      </tbody></table>
    </div>
    ${appFooter()}
  `;
}

/* --------------------------- Modal + interactions ------------------------------ */

const modalBackdrop = document.getElementById("modalBackdrop");
const modalBody = document.getElementById("modalBody");
function openModal(html) {
  modalBody.innerHTML = html;
  modalBackdrop.style.display = "flex";
  const cancel = document.getElementById("cancelModal");
  if (cancel) cancel.addEventListener("click", closeModal);
}
function closeModal() {
  modalBackdrop.style.display = "none";
  modalBody.innerHTML = "";
}
modalBackdrop.addEventListener("click", (e) => { if (e.target === modalBackdrop) closeModal(); });

function toast(msg) {
  let t = document.getElementById("toastEl");
  if (!t) {
    t = document.createElement("div");
    t.id = "toastEl";
    t.style.cssText = "position:fixed;bottom:24px;left:50%;transform:translateX(-50%);background:#2B2420;color:#fff;padding:11px 18px;border-radius:6px;font-size:0.85rem;z-index:100;box-shadow:0 6px 18px rgba(0,0,0,.2);";
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.style.display = "block";
  clearTimeout(t._h);
  t._h = setTimeout(() => { t.style.display = "none"; }, 2200);
}

function attachHandlersFor(view) {
  const main = document.getElementById("main");

  // Back to student list
  main.querySelectorAll("[data-back-students]").forEach((el) =>
    el.addEventListener("click", (e) => { e.preventDefault(); state.selectedStudentId = null; render(); })
  );
  // View student profile
  main.querySelectorAll("[data-view-student]").forEach((el) =>
    el.addEventListener("click", () => { state.selectedStudentId = el.dataset.viewStudent; render(); })
  );
  // Add student
  const addBtn = document.getElementById("addStudentBtn");
  if (addBtn) addBtn.addEventListener("click", openAddStudentModal);
  const saveNew = document.getElementById("saveNewStudent");
  if (saveNew) saveNew.addEventListener("click", () => {
    const name = document.getElementById("newStudentName").value.trim();
    if (!name) { toast("Enter a student name first."); return; }
    STUDENTS.push({
      id: "s" + (STUDENTS.length + 1),
      name,
      course: document.getElementById("newStudentCourse").value,
      level: document.getElementById("newStudentLevel").value || "Beginner",
      batch: document.getElementById("newStudentBatch").value || "TBD",
      faculty: document.getElementById("newStudentFaculty").value,
      attendancePct: 100,
      feeStatus: "due",
      dueAmount: 5000,
      dueDate: "2026-10-01",
      grade: "—",
      parent: "—",
    });
    closeModal();
    toast("Student enrolled.");
    render();
  });

  // Attendance marking
  main.querySelectorAll(".seg button").forEach((btn) =>
    btn.addEventListener("click", () => {
      const seg = btn.closest(".seg");
      const sid = seg.dataset.student;
      state.attendanceDraft[sid] = btn.dataset.mark;
      render();
    })
  );
  const saveAtt = document.getElementById("saveAttendanceBtn");
  if (saveAtt) saveAtt.addEventListener("click", () => { toast("Attendance saved for today's session."); state.attendanceDraft = {}; render(); });

  // Fee collection / invoice / pay
  main.querySelectorAll("[data-collect]").forEach((el) =>
    el.addEventListener("click", () => {
      const s = STUDENTS.find((x) => x.id === el.dataset.collect);
      if (s) { s.feeStatus = "paid"; s.dueAmount = 0; s.dueDate = null; }
      toast("Payment recorded. Receipt generated.");
      render();
    })
  );
  main.querySelectorAll("[data-invoice]").forEach((el) =>
    el.addEventListener("click", () => toast("Invoice sent."))
  );
  const payNow = document.getElementById("payNowBtn");
  if (payNow) payNow.addEventListener("click", () => toast("Redirecting to payment gateway… (demo)"));

  // Exam grading
  main.querySelectorAll("[data-grade]").forEach((el) =>
    el.addEventListener("click", () => {
      const e = EXAMS.find((x) => x.id === el.dataset.grade);
      openModal(`
        <h3>Enter grade — ${e.student}</h3>
        <p class="panel-sub">${e.course} · ${e.type}</p>
        <div class="field"><label>Marks (out of 100)</label><input type="number" id="gradeMarks" min="0" max="100" /></div>
        <div class="modal-actions">
          <button class="btn ghost" id="cancelModal">Cancel</button>
          <button class="btn solid" id="saveGrade">Save grade</button>
        </div>
      `);
      document.getElementById("saveGrade").addEventListener("click", () => {
        const m = parseInt(document.getElementById("gradeMarks").value, 10);
        if (isNaN(m)) { toast("Enter marks first."); return; }
        e.marks = m;
        e.grade = m >= 90 ? "A+" : m >= 80 ? "A" : m >= 70 ? "B+" : m >= 60 ? "B" : m >= 50 ? "C" : "D";
        closeModal();
        toast("Grade saved.");
        render();
      });
    })
  );
  const scheduleExam = document.getElementById("scheduleExamBtn");
  if (scheduleExam) scheduleExam.addEventListener("click", () => toast("Exam scheduling form (demo) — pick a student, course and date."));

  // Event registration
  main.querySelectorAll("[data-register]").forEach((el) =>
    el.addEventListener("click", () => {
      const ev = EVENTS.find((x) => x.id === el.dataset.register);
      if (ev && ev.registered < ev.capacity) ev.registered += 1;
      toast("You're registered for " + (ev ? ev.name : "the event") + ".");
      render();
    })
  );

  // Misc "coming soon" affordances that are demo-only
  ["addCourseBtn", "addFacultyBtn", "addEventBtn"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.addEventListener("click", () => toast("Form opens here in the full build."));
  });
}
