package app.swarsetu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class Audit {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public Audit(JdbcClient jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    public void log(String collegeId, String userId, String action, String entity, String entityId, Map<String, ?> detail) {
        String d;
        try { d = detail == null ? null : json.writeValueAsString(detail); } catch (Exception e) { d = null; }
        jdbc.sql("INSERT INTO audit_log(college_id, user_id, action, entity, entity_id, detail) VALUES (?,?,?,?,?,?)")
            .param(collegeId).param(userId).param(action).param(entity).param(entityId).param(d).update();
    }

    public void log(Me me, String action, String entity, String entityId, Map<String, ?> detail) {
        log(me.collegeId(), me.id(), action, entity, entityId, detail);
    }
}
