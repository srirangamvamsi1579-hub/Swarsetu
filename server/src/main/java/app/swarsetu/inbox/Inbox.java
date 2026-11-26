package app.swarsetu.inbox;

import app.swarsetu.common.Crypto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** In-app messages. Audience: "all", "admin", a permission name (e.g. "editAttendance"), or one user. */
@Service
public class Inbox {
    private final JdbcClient jdbc;

    public Inbox(JdbcClient jdbc) { this.jdbc = jdbc; }

    public void toAudience(String collegeId, String audience, String kind, String title, String body, String link, String dedupeKey) {
        jdbc.sql("INSERT IGNORE INTO inbox_messages(id, college_id, audience, kind, title, body, link, dedupe_key) VALUES (?,?,?,?,?,?,?,?)")
            .param(Crypto.id("msg")).param(collegeId).param(audience).param(kind).param(title).param(body).param(link).param(dedupeKey).update();
    }

    public void toUser(String collegeId, String userId, String kind, String title, String body, String link, String dedupeKey) {
        jdbc.sql("INSERT IGNORE INTO inbox_messages(id, college_id, user_id, kind, title, body, link, dedupe_key) VALUES (?,?,?,?,?,?,?,?)")
            .param(Crypto.id("msg")).param(collegeId).param(userId).param(kind).param(title).param(body).param(link).param(dedupeKey).update();
    }
}
