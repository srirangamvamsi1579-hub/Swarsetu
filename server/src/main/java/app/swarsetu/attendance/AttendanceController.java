package app.swarsetu.attendance;

import app.swarsetu.common.*;
import app.swarsetu.inbox.Inbox;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Codes: P (attended), L (late), A, V (leave), H (holiday), DH, C.
 * Completed = P + L. Pending = Total - DH - H + C - Completed; a calendar holiday on a class day counts as H when no code is recorded.
 */
@RestController
@RequestMapping("/api")
public class AttendanceController {
    private static final Set<String> CODES = Set.of("P", "L", "A", "V", "H", "DH", "C");
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;
    private final Schedule schedule;
    private final Inbox inbox;
    private final Audit audit;

    public AttendanceController(JdbcClient jdbc, CurrentUser current, Perms perms, Schedule schedule, Inbox inbox, Audit audit) {
        this.jdbc = jdbc; this.current = current; this.perms = perms; this.schedule = schedule; this.inbox = inbox; this.audit = audit;
    }

    private String ownStudent(Me me) {
        if (me.studentId() == null) throw ApiException.forbidden("Your account is not linked to a student record yet. Ask the college office.");
        return me.studentId();
    }

    @GetMapping("/attendance")
    public List<Map<String, Object>> list(@RequestParam String from, @RequestParam String to,
                                          @RequestParam(required = false) String batchId, @RequestParam(required = false) String studentId) {
        Me me = current.get();
        perms.need(me, "viewAttendance");
        LocalDate f = Check.date(from, "From"), t = Check.date(to, "To");
        Check.range(f, t);
        if (f.plusDays(370).isBefore(t)) throw ApiException.bad("Choose at most one year at a time.");
        StringBuilder sql = new StringBuilder("SELECT a.student_id, a.class_date, a.code, a.previous_code, a.saved_at, "
            + "CONCAT(u.first_name, ' ', IFNULL(u.last_name, '')) AS saved_by FROM attendance_marks a "
            + "JOIN students s ON s.college_id = a.college_id AND s.id = a.student_id "
            + "JOIN users u ON u.college_id = a.college_id AND u.id = a.saved_by "
            + "WHERE a.college_id = ? AND a.class_date BETWEEN ? AND ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId(), f, t));
        if (me.isStudent()) { sql.append(" AND a.student_id = ?"); ps.add(ownStudent(me)); }
        else if (studentId != null && !studentId.isBlank()) { sql.append(" AND a.student_id = ?"); ps.add(studentId); }
        if (batchId != null && !batchId.isBlank()) { sql.append(" AND s.batch_id = ?"); ps.add(batchId); }
        sql.append(" ORDER BY a.class_date, a.student_id");
        return Rows.camel(jdbc.sql(sql.toString()).params(ps).query().listOfRows());
    }

    public record Mark(@NotBlank String studentId, String code) {}

    public record SaveIn(@NotBlank String date, @NotEmpty @Size(max = 500) List<@Valid Mark> marks) {}

    /** Saves a whole draft at once. code = null clears a mark. The previous code is kept for the audit trail. */
    @PostMapping("/attendance")
    @Transactional
    public Map<String, Object> save(@Valid @RequestBody SaveIn in) {
        Me me = current.get();
        perms.need(me, "editAttendance");
        LocalDate d = Check.date(in.date(), "Date");
        if (d.isAfter(LocalDate.now().plusDays(1))) throw ApiException.bad("Attendance cannot be marked for future dates.");
        int saved = 0, cleared = 0;
        for (Mark m : in.marks()) {
            if (m.code() == null || m.code().isBlank()) {
                cleared += jdbc.sql("DELETE FROM attendance_marks WHERE college_id = ? AND student_id = ? AND class_date = ?")
                    .param(me.collegeId()).param(m.studentId()).param(d).update();
                continue;
            }
            if (!CODES.contains(m.code())) throw ApiException.bad("Unknown attendance code: " + m.code());
            // previous_code is assigned before code, so it receives the old value.
            jdbc.sql("INSERT INTO attendance_marks(college_id, student_id, class_date, code, saved_by) VALUES (?,?,?,?,?) AS n "
                    + "ON DUPLICATE KEY UPDATE previous_code = IF(attendance_marks.code <> n.code, attendance_marks.code, attendance_marks.previous_code), "
                    + "saved_by = IF(attendance_marks.code <> n.code, n.saved_by, attendance_marks.saved_by), "
                    + "saved_at = IF(attendance_marks.code <> n.code, NOW(3), attendance_marks.saved_at), code = n.code")
                .param(me.collegeId()).param(m.studentId()).param(d).param(m.code()).param(me.id()).update();
            jdbc.sql("UPDATE absence_notices SET status = 'marked', handled_by = ?, handled_at = NOW(3) "
                    + "WHERE college_id = ? AND student_id = ? AND class_date = ? AND status IN ('sent','seen')")
                .param(me.id()).param(me.collegeId()).param(m.studentId()).param(d).update();
            saved++;
        }
        audit.log(me, "attendance.saved", "attendance", in.date(), Map.of("saved", saved, "cleared", cleared));
        return Map.of("saved", saved, "cleared", cleared);
    }

    /** Monthly summary from YYYY-MM to YYYY-MM. */
    @GetMapping("/attendance/summary")
    public List<Map<String, Object>> summary(@RequestParam String fromMonth, @RequestParam String toMonth,
                                             @RequestParam(required = false) String batchId) {
        Me me = current.get();
        perms.need(me, "viewAttendance");
        YearMonth fm = YearMonth.parse(Check.month(fromMonth)), tm = YearMonth.parse(Check.month(toMonth));
        if (tm.isBefore(fm)) throw ApiException.bad("The To month may not be earlier than the From month.");
        if (fm.plusMonths(12).isBefore(tm)) throw ApiException.bad("Choose at most 12 months at a time.");
        LocalDate from = fm.atDay(1), to = tm.atEndOfMonth();
        StringBuilder sql = new StringBuilder("SELECT s.id, s.first_name, s.last_name, s.weekdays, s.joining_date, s.batch_id, s.status, b.name AS batch_name "
            + "FROM students s LEFT JOIN batches b ON b.college_id = s.college_id AND b.id = s.batch_id WHERE s.college_id = ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId()));
        if (me.isStudent()) { sql.append(" AND s.id = ?"); ps.add(ownStudent(me)); }
        if (batchId != null && !batchId.isBlank()) { sql.append(" AND s.batch_id = ?"); ps.add(batchId); }
        List<Map<String, Object>> students = jdbc.sql(sql.append(" ORDER BY s.id").toString()).params(ps).query().listOfRows();
        Map<String, Map<LocalDate, String>> marks = new HashMap<>();
        jdbc.sql("SELECT student_id, class_date, code FROM attendance_marks WHERE college_id = ? AND class_date BETWEEN ? AND ?")
            .param(me.collegeId()).param(from).param(to)
            .query(rs -> { marks.computeIfAbsent(rs.getString(1), k -> new HashMap<>()).put(rs.getDate(2).toLocalDate(), rs.getString(3)); });
        Map<String, Set<LocalDate>> hol = schedule.holidays(me.collegeId(), from, to);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : students) {
            String id = (String) s.get("id");
            LocalDate join = s.get("joining_date") == null ? from : ((java.sql.Date) s.get("joining_date")).toLocalDate();
            Map<LocalDate, String> m = marks.getOrDefault(id, Map.of());
            Set<LocalDate> h = hol.getOrDefault(id, Set.of());
            Map<String, Integer> n = new LinkedHashMap<>();
            for (String c : List.of("P", "L", "A", "V", "H", "DH", "C")) n.put(c, 0);
            int total = 0;
            for (LocalDate d = from.isBefore(join) ? join : from; !d.isAfter(to); d = d.plusDays(1)) {
                if (!Schedule.isClassDay((String) s.get("weekdays"), d)) continue;
                total++;
                String code = m.get(d);
                if (code == null && h.contains(d)) code = "H";
                if (code != null) n.merge(code, 1, Integer::sum);
            }
            // Codes recorded on non-class days (e.g. a compensatory class) still count.
            m.forEach((d, c) -> { if (!Schedule.isClassDay((String) s.get("weekdays"), d)) n.merge(c, 1, Integer::sum); });
            int completed = n.get("P") + n.get("L");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("studentId", id);
            r.put("name", (s.get("first_name") + " " + (s.get("last_name") == null ? "" : s.get("last_name"))).trim());
            r.put("batchName", s.get("batch_name"));
            r.put("status", s.get("status"));
            r.put("total", total);
            r.put("counts", n);
            r.put("completed", completed);
            r.put("pending", total - n.get("DH") - n.get("H") + n.get("C") - completed);
            out.add(r);
        }
        return out;
    }

    // ---------------- absence notices ("I will not attend") ----------------

    public record NoticeIn(@NotBlank String date, @NotBlank @Size(max = 60) String reason, @Size(max = 200) String note) {}

    @PostMapping("/absence-notices")
    @Transactional
    public Map<String, Object> notify(@Valid @RequestBody NoticeIn in) {
        Me me = current.get();
        String sid = ownStudent(me);
        LocalDate d = Check.date(in.date(), "Date");
        if (d.isBefore(LocalDate.now())) throw ApiException.bad("Choose today or a future class date.");
        if (d.isAfter(LocalDate.now().plusDays(60))) throw ApiException.bad("Choose a date within the next 60 days.");
        Map<String, Object> s = jdbc.sql("SELECT first_name, last_name, weekdays FROM students WHERE college_id = ? AND id = ?")
            .param(me.collegeId()).param(sid).query().singleRow();
        if (!Schedule.isClassDay((String) s.get("weekdays"), d)) throw ApiException.bad("You have no class on that day.");
        int dup = jdbc.sql("SELECT COUNT(*) FROM absence_notices WHERE college_id = ? AND student_id = ? AND class_date = ? AND status IN ('sent','seen')")
            .param(me.collegeId()).param(sid).param(d).query(Integer.class).single();
        if (dup > 0) throw ApiException.conflict("You have already told your teacher about this day.");
        String id = Crypto.id("abs");
        jdbc.sql("INSERT INTO absence_notices(id, college_id, student_id, class_date, reason, note, sent_by) VALUES (?,?,?,?,?,?,?)")
            .param(id).param(me.collegeId()).param(sid).param(d).param(in.reason()).param(in.note()).param(me.id()).update();
        String name = (s.get("first_name") + " " + (s.get("last_name") == null ? "" : s.get("last_name"))).trim();
        inbox.toAudience(me.collegeId(), "editAttendance", "absence", name + " will not attend on " + d,
            "Reason: " + in.reason() + (in.note() == null || in.note().isBlank() ? "" : " — " + in.note()), "attendance", "abs:" + id);
        return Map.of("id", id);
    }

    @GetMapping("/absence-notices")
    public List<Map<String, Object>> notices(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        Me me = current.get();
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : Check.date(from, "From");
        LocalDate t = to == null ? LocalDate.now().plusDays(60) : Check.date(to, "To");
        Check.range(f, t);
        StringBuilder sql = new StringBuilder("SELECT n.*, CONCAT(s.first_name, ' ', IFNULL(s.last_name, '')) AS student_name FROM absence_notices n "
            + "JOIN students s ON s.college_id = n.college_id AND s.id = n.student_id WHERE n.college_id = ? AND n.class_date BETWEEN ? AND ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId(), f, t));
        if (me.isStudent()) { sql.append(" AND n.student_id = ?"); ps.add(ownStudent(me)); }
        else perms.need(me, "viewAttendance");
        return Rows.camel(jdbc.sql(sql.append(" ORDER BY n.class_date").toString()).params(ps).query().listOfRows());
    }

    @PostMapping("/absence-notices/{id}/seen")
    public Map<String, Object> seen(@PathVariable String id) {
        Me me = current.get();
        perms.need(me, "editAttendance");
        jdbc.sql("UPDATE absence_notices SET status = 'seen', handled_by = ?, handled_at = NOW(3) WHERE college_id = ? AND id = ? AND status = 'sent'")
            .param(me.id()).param(me.collegeId()).param(id).update();
        return Map.of("ok", true);
    }

    @DeleteMapping("/absence-notices/{id}")
    public Map<String, Object> cancel(@PathVariable String id) {
        Me me = current.get();
        int n = jdbc.sql("UPDATE absence_notices SET status = 'cancelled' WHERE college_id = ? AND id = ? AND student_id = ? AND status IN ('sent','seen')")
            .param(me.collegeId()).param(id).param(ownStudent(me)).update();
        if (n == 0) throw ApiException.notFound("Notice not found or already handled.");
        return Map.of("ok", true);
    }
}
