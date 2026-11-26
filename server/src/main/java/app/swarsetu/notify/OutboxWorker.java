package app.swarsetu.notify;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Sends queued email/SMS every 10 seconds; retries with back-off (1, 2, 4 ... minutes) and gives up after 6 tries. */
@Component
@ConditionalOnProperty(name = "swarsetu.jobs-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int MAX_TRIES = 6;
    private final JdbcClient jdbc;
    private final EmailSender email;
    private final SmsSender sms;

    public OutboxWorker(JdbcClient jdbc, EmailSender email, SmsSender sms) { this.jdbc = jdbc; this.email = email; this.sms = sms; }

    @Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
    @Transactional
    public void run() {
        // SKIP LOCKED lets several server instances share the queue without sending twice.
        List<Map<String, Object>> due = jdbc.sql(
            "SELECT id, channel, recipient, subject, body, attempts FROM outbox "
            + "WHERE status = 'queued' AND next_try_at <= NOW(3) ORDER BY id LIMIT 25 FOR UPDATE SKIP LOCKED")
            .query().listOfRows();
        for (Map<String, Object> m : due) {
            long id = ((Number) m.get("id")).longValue();
            int attempts = ((Number) m.get("attempts")).intValue() + 1;
            try {
                if ("email".equals(m.get("channel"))) email.send((String) m.get("recipient"), (String) m.get("subject"), (String) m.get("body"));
                else sms.send((String) m.get("recipient"), (String) m.get("body"));
                jdbc.sql("UPDATE outbox SET status = 'sent', attempts = ?, sent_at = NOW(3), last_error = NULL, body = '[removed after sending]' WHERE id = ?")
                    .param(attempts).param(id).update();
            } catch (Exception e) {
                String err = String.valueOf(e.getMessage());
                log.warn("Outbox {} attempt {} failed: {}", id, attempts, err);
                jdbc.sql("UPDATE outbox SET status = ?, attempts = ?, next_try_at = NOW(3) + INTERVAL ? MINUTE, last_error = ? WHERE id = ?")
                    .param(attempts >= MAX_TRIES ? "failed" : "queued").param(attempts)
                    .param(1 << Math.min(attempts - 1, 6))
                    .param(err.length() > 490 ? err.substring(0, 490) : err).param(id).update();
            }
        }
    }
}
