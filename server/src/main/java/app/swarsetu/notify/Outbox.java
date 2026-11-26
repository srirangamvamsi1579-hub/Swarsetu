package app.swarsetu.notify;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Queues email/SMS in the same database transaction as the change that caused them.
 * OutboxWorker sends them with retries, so a slow or failing provider never loses or blocks a save.
 */
@Service
public class Outbox {
    private final JdbcClient jdbc;

    public Outbox(JdbcClient jdbc) { this.jdbc = jdbc; }

    public void email(String collegeId, String to, String subject, String body, String dedupeKey) {
        if (to == null || to.isBlank()) return;
        jdbc.sql("INSERT IGNORE INTO outbox(college_id, channel, recipient, subject, body, dedupe_key) VALUES (?,?,?,?,?,?)")
            .param(collegeId).param("email").param(to).param(subject).param(body).param(dedupeKey).update();
    }

    /** mobile = 10 digits; countryCode like +91. */
    public void sms(String collegeId, String countryCode, String mobile, String body, String dedupeKey) {
        if (mobile == null || mobile.isBlank()) return;
        String cc = countryCode == null || countryCode.isBlank() ? "+91" : countryCode;
        jdbc.sql("INSERT IGNORE INTO outbox(college_id, channel, recipient, body, dedupe_key) VALUES (?,?,?,?,?)")
            .param(collegeId).param("sms").param(cc + mobile).param(body).param(dedupeKey).update();
    }
}
