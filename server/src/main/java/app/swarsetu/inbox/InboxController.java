package app.swarsetu.inbox;

import app.swarsetu.common.CurrentUser;
import app.swarsetu.common.Me;
import app.swarsetu.common.Perms;
import app.swarsetu.common.Rows;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbox")
public class InboxController {
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;

    public InboxController(JdbcClient jdbc, CurrentUser current, Perms perms) { this.jdbc = jdbc; this.current = current; this.perms = perms; }

    /** Audiences this user belongs to: all, admin (if Admin), and every permission they hold. */
    private List<String> audiences(Me me) {
        List<String> a = new ArrayList<>(List.of("all"));
        if (me.isAdmin()) a.add("admin");
        for (String p : Perms.ALL) if (perms.can(me, p)) a.add(p);
        return a;
    }

    private String where(Me me, List<Object> params) {
        List<String> a = audiences(me);
        params.add(me.collegeId());
        params.add(me.id());
        params.addAll(a);
        return "m.college_id = ? AND (m.user_id = ? OR m.audience IN (" + a.stream().map(x -> "?").collect(Collectors.joining(",")) + "))";
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "100") int limit) {
        Me me = current.get();
        List<Object> ps = new ArrayList<>();
        ps.add(me.id());
        String w = where(me, ps);
        ps.add(Math.min(Math.max(limit, 1), 500));
        List<Map<String, Object>> rows = jdbc.sql("SELECT m.id, m.kind, m.title, m.body, m.link, m.created_at, r.read_at IS NOT NULL AS is_read "
                + "FROM inbox_messages m LEFT JOIN inbox_reads r ON r.college_id = m.college_id AND r.message_id = m.id AND r.user_id = ? "
                + "WHERE " + w + " ORDER BY m.created_at DESC LIMIT ?")
            .params(ps).query().listOfRows();
        long unread = rows.stream().filter(r -> !Rows.truthy(r.get("is_read"))).count();
        return Map.of("messages", Rows.camel(rows), "unread", unread);
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> read(@PathVariable String id) {
        Me me = current.get();
        List<Object> ps = new ArrayList<>(List.of(me.id()));
        String w = where(me, ps);
        ps.add(id);
        jdbc.sql("INSERT IGNORE INTO inbox_reads(college_id, message_id, user_id) SELECT m.college_id, m.id, ? FROM inbox_messages m WHERE "
                + w + " AND m.id = ?").params(ps).update();
        return Map.of("ok", true);
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll() {
        Me me = current.get();
        List<Object> ps = new ArrayList<>(List.of(me.id()));
        String w = where(me, ps);
        jdbc.sql("INSERT IGNORE INTO inbox_reads(college_id, message_id, user_id) SELECT m.college_id, m.id, ? FROM inbox_messages m WHERE " + w)
            .params(ps).update();
        return Map.of("ok", true);
    }
}
