package app.swarsetu.jobs;

import app.swarsetu.common.Rows;
import app.swarsetu.inbox.Inbox;
import app.swarsetu.notify.Outbox;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every morning: holiday and vacation reminders (college.reminder_days ahead) in the inbox and,
 * if the college allows it, by email to the people concerned. Dedupe keys make re-runs harmless.
 * Also clears expired codes and old refresh tokens.
 */
@Component
@ConditionalOnProperty(name = "swarsetu.jobs-enabled", havingValue = "true", matchIfMissing = true)
public class Reminders {
    private static final Logger log = LoggerFactory.getLogger(Reminders.class);
    private final JdbcClient jdbc;
    private final Inbox inbox;
    private final Outbox outbox;
    private final ZoneId zone;

    public Reminders(JdbcClient jdbc, Inbox inbox, Outbox outbox, app.swarsetu.config.AppProps props) {
        this.jdbc = jdbc; this.inbox = inbox; this.outbox = outbox;
        this.zone = ZoneId.of(props.reminderZone() == null ? "Asia/Kolkata" : props.reminderZone());
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "${swarsetu.reminder-zone:Asia/Kolkata}")
    @Transactional
    public void holidayReminders() {
        LocalDate today = LocalDate.now(zone);
        List<Map<String, Object>> due = jdbc.sql("SELECT e.id, e.college_id, e.title, e.type, e.start_date, e.end_date, e.applies_to, c.name AS college_name, "
                + "c.reminder_external FROM calendar_events e JOIN colleges c ON c.id = e.college_id "
                + "WHERE e.type IN ('Holiday','Vacation') AND c.reminder_days > 0 AND e.start_date = ? + INTERVAL c.reminder_days DAY")
            .param(today).query().listOfRows();
        for (Map<String, Object> e : due) {
            String col = (String) e.get("college_id"), id = (String) e.get("id");
            String when = e.get("start_date").equals(e.get("end_date")) ? "on " + e.get("start_date") : "from " + e.get("start_date") + " to " + e.get("end_date");
            String title = e.get("type") + " reminder: " + e.get("title");
            String body = e.get("college_name") + " is closed " + when + " (" + e.get("title") + "). There are no classes on these days.";
            if ("all".equals(e.get("applies_to"))) inbox.toAudience(col, "all", "holiday", title, body, "calendar", "rem:" + id + ":" + today);
            for (Map<String, Object> u : people(col, id, (String) e.get("applies_to"))) {
                if (!"all".equals(e.get("applies_to"))) inbox.toUser(col, (String) u.get("id"), "holiday", title, body, "calendar", "rem:" + id + ":" + today + ":" + u.get("id"));
                if (Rows.truthy(e.get("reminder_external"))) outbox.email(col, (String) u.get("email"), title, body, "rem:" + id + ":" + today + ":" + u.get("id"));
            }
        }
        if (!due.isEmpty()) log.info("Sent {} holiday/vacation reminder(s)", due.size());
    }

    /** Active staff always; students only if the entry applies to them. */
    private List<Map<String, Object>> people(String col, String eventId, String applies) {
        String students = switch (applies) {
            case "batches" -> "u.student_id IN (SELECT s.id FROM students s JOIN calendar_event_batches b ON b.college_id = s.college_id AND b.batch_id = s.batch_id "
                + "WHERE s.college_id = u.college_id AND b.event_id = ?)";
            case "students" -> "u.student_id IN (SELECT student_id FROM calendar_event_students WHERE college_id = u.college_id AND event_id = ?)";
            default -> "? IS NOT NULL";
        };
        return jdbc.sql("SELECT u.id, u.email FROM users u WHERE u.college_id = ? AND u.status = 'active' AND (u.role <> 'student' OR " + students + ")")
            .param(col).param(eventId).query().listOfRows();
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanup() {
        jdbc.sql("DELETE FROM verifications WHERE expires_at < NOW(3) - INTERVAL 1 DAY").update();
        jdbc.sql("DELETE FROM refresh_tokens WHERE expires_at < NOW(3) - INTERVAL 7 DAY").update();
        jdbc.sql("DELETE FROM outbox WHERE status = 'sent' AND sent_at < NOW(3) - INTERVAL 30 DAY").update();
    }
}
