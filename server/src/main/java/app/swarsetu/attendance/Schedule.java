package app.swarsetu.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Works out class days and college-calendar holidays for students. */
@Service
public class Schedule {
    private static final Map<String, DayOfWeek> DAY = Map.of("Mon", DayOfWeek.MONDAY, "Tue", DayOfWeek.TUESDAY, "Wed", DayOfWeek.WEDNESDAY,
        "Thu", DayOfWeek.THURSDAY, "Fri", DayOfWeek.FRIDAY, "Sat", DayOfWeek.SATURDAY, "Sun", DayOfWeek.SUNDAY);
    private final JdbcClient jdbc;

    public Schedule(JdbcClient jdbc) { this.jdbc = jdbc; }

    public static Set<DayOfWeek> days(String weekdays) {
        Set<DayOfWeek> s = EnumSet.noneOf(DayOfWeek.class);
        for (String d : weekdays.split(",")) if (DAY.containsKey(d.trim())) s.add(DAY.get(d.trim()));
        return s;
    }

    public static boolean isClassDay(String weekdays, LocalDate date) { return days(weekdays).contains(date.getDayOfWeek()); }

    /**
     * Holiday/Vacation dates per student between from and to (inclusive), honouring "applies to":
     * all students, selected batches or selected students.
     */
    public Map<String, Set<LocalDate>> holidays(String collegeId, LocalDate from, LocalDate to) {
        Map<String, Set<LocalDate>> out = new HashMap<>();
        List<Map<String, Object>> events = jdbc.sql("SELECT id, start_date, end_date, applies_to FROM calendar_events "
                + "WHERE college_id = ? AND type IN ('Holiday','Vacation') AND start_date <= ? AND end_date >= ?")
            .param(collegeId).param(to).param(from).query().listOfRows();
        if (events.isEmpty()) return out;
        List<Map<String, Object>> students = jdbc.sql("SELECT id, batch_id FROM students WHERE college_id = ?").param(collegeId).query().listOfRows();
        for (Map<String, Object> e : events) {
            LocalDate s = ((java.sql.Date) e.get("start_date")).toLocalDate(), en = ((java.sql.Date) e.get("end_date")).toLocalDate();
            if (s.isBefore(from)) s = from;
            if (en.isAfter(to)) en = to;
            Set<String> who = new HashSet<>();
            String applies = (String) e.get("applies_to");
            if ("all".equals(applies)) {
                students.forEach(st -> who.add((String) st.get("id")));
            } else if ("batches".equals(applies)) {
                Set<String> bs = new HashSet<>(jdbc.sql("SELECT batch_id FROM calendar_event_batches WHERE college_id = ? AND event_id = ?")
                    .param(collegeId).param(e.get("id")).query(String.class).list());
                students.stream().filter(st -> bs.contains(st.get("batch_id"))).forEach(st -> who.add((String) st.get("id")));
            } else {
                who.addAll(jdbc.sql("SELECT student_id FROM calendar_event_students WHERE college_id = ? AND event_id = ?")
                    .param(collegeId).param(e.get("id")).query(String.class).list());
            }
            for (LocalDate d = s; !d.isAfter(en); d = d.plusDays(1)) {
                for (String w : who) out.computeIfAbsent(w, k -> new HashSet<>()).add(d);
            }
        }
        return out;
    }
}
