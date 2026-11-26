package app.swarsetu.calendar;

import app.swarsetu.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** College Calendar: Holiday, Vacation, Event, Other — for all, selected batches or selected students. */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;
    private final Audit audit;

    public CalendarController(JdbcClient jdbc, CurrentUser current, Perms perms, Audit audit) {
        this.jdbc = jdbc; this.current = current; this.perms = perms; this.audit = audit;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam String from, @RequestParam String to) {
        Me me = current.get();
        perms.need(me, "viewCalendar");
        LocalDate f = Check.date(from, "From"), t = Check.date(to, "To");
        Check.range(f, t);
        List<Map<String, Object>> rows = Rows.camel(jdbc.sql("SELECT id, title, type, start_date, end_date, description, applies_to FROM calendar_events "
            + "WHERE college_id = ? AND start_date <= ? AND end_date >= ? ORDER BY start_date").param(me.collegeId()).param(t).param(f).query().listOfRows());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> e : rows) {
            String id = (String) e.get("id");
            List<String> bs = jdbc.sql("SELECT batch_id FROM calendar_event_batches WHERE college_id = ? AND event_id = ?").param(me.collegeId()).param(id).query(String.class).list();
            List<String> ss = jdbc.sql("SELECT student_id FROM calendar_event_students WHERE college_id = ? AND event_id = ?").param(me.collegeId()).param(id).query(String.class).list();
            if (me.isStudent() && !appliesToStudent(me, (String) e.get("appliesTo"), bs, ss)) continue; // students see only what concerns them
            e.put("batchIds", bs);
            if (!me.isStudent()) e.put("studentIds", ss);
            out.add(e);
        }
        return out;
    }

    private boolean appliesToStudent(Me me, String applies, List<String> bs, List<String> ss) {
        if ("all".equals(applies)) return true;
        if (me.studentId() == null) return false;
        if ("students".equals(applies)) return ss.contains(me.studentId());
        String batch = jdbc.sql("SELECT batch_id FROM students WHERE college_id = ? AND id = ?").param(me.collegeId()).param(me.studentId())
            .query(String.class).optional().orElse(null);
        return batch != null && bs.contains(batch);
    }

    public record EventIn(@NotBlank @Size(max = 150) String title, @NotBlank @Pattern(regexp = "Holiday|Vacation|Event|Other") String type,
                          @NotBlank String startDate, @NotBlank String endDate, @Size(max = 500) String description,
                          @Pattern(regexp = "all|batches|students") String appliesTo, List<String> batchIds, List<String> studentIds) {}

    @PostMapping
    @Transactional
    public Map<String, Object> add(@Valid @RequestBody EventIn e) {
        Me me = current.get();
        perms.need(me, "manageCalendar");
        String id = Crypto.id("cal");
        LocalDate s = Check.date(e.startDate(), "From"), en = Check.date(e.endDate(), "To");
        Check.range(s, en);
        jdbc.sql("INSERT INTO calendar_events(id, college_id, title, type, start_date, end_date, description, applies_to, created_by) VALUES (?,?,?,?,?,?,?,?,?)")
            .param(id).param(me.collegeId()).param(e.title().trim()).param(e.type()).param(s).param(en).param(e.description())
            .param(e.appliesTo() == null ? "all" : e.appliesTo()).param(me.id()).update();
        saveTargets(me, id, e);
        audit.log(me, "calendar.added", "calendar", id, Map.of("title", e.title()));
        return Map.of("id", id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> edit(@PathVariable String id, @Valid @RequestBody EventIn e) {
        Me me = current.get();
        perms.need(me, "manageCalendar");
        LocalDate s = Check.date(e.startDate(), "From"), en = Check.date(e.endDate(), "To");
        Check.range(s, en);
        int n = jdbc.sql("UPDATE calendar_events SET title = ?, type = ?, start_date = ?, end_date = ?, description = ?, applies_to = ?, version = version + 1 "
                + "WHERE college_id = ? AND id = ?")
            .param(e.title().trim()).param(e.type()).param(s).param(en).param(e.description()).param(e.appliesTo() == null ? "all" : e.appliesTo())
            .param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Calendar entry not found.");
        saveTargets(me, id, e);
        audit.log(me, "calendar.edited", "calendar", id, null);
        return Map.of("id", id);
    }

    private void saveTargets(Me me, String id, EventIn e) {
        jdbc.sql("DELETE FROM calendar_event_batches WHERE college_id = ? AND event_id = ?").param(me.collegeId()).param(id).update();
        jdbc.sql("DELETE FROM calendar_event_students WHERE college_id = ? AND event_id = ?").param(me.collegeId()).param(id).update();
        String applies = e.appliesTo() == null ? "all" : e.appliesTo();
        if ("batches".equals(applies)) {
            if (e.batchIds() == null || e.batchIds().isEmpty()) throw ApiException.bad("Choose at least one batch.");
            for (String b : new LinkedHashSet<>(e.batchIds())) {
                jdbc.sql("INSERT INTO calendar_event_batches(college_id, event_id, batch_id) VALUES (?,?,?)").param(me.collegeId()).param(id).param(b).update();
            }
        } else if ("students".equals(applies)) {
            if (e.studentIds() == null || e.studentIds().isEmpty()) throw ApiException.bad("Choose at least one student.");
            for (String s : new LinkedHashSet<>(e.studentIds())) {
                jdbc.sql("INSERT INTO calendar_event_students(college_id, event_id, student_id) VALUES (?,?,?)").param(me.collegeId()).param(id).param(s).update();
            }
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        Me me = current.get();
        perms.need(me, "manageCalendar");
        int n = jdbc.sql("DELETE FROM calendar_events WHERE college_id = ? AND id = ?").param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Calendar entry not found.");
        audit.log(me, "calendar.deleted", "calendar", id, null);
        return Map.of("ok", true);
    }
}
