package app.swarsetu.docs;

import app.swarsetu.common.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

/**
 * Lists that do not yet have their own tables (Phase 2): stored as JSON per college,
 * with the same view/edit permissions the web app uses. Links inside must be https.
 */
@RestController
@RequestMapping("/api/docs")
public class DocumentsController {
    /** collection -> {view permission, edit permission} */
    private static final Map<String, String[]> COLLECTIONS = Map.of(
        "classes", new String[] {"viewClasses", "editClasses"},
        "ttSessions", new String[] {"viewTimetable", "editTimetable"},
        "levels", new String[] {"viewCourses", "editCourses"},
        "facultySlots", new String[] {"viewFaculty", "editFaculty"},
        "events", new String[] {"viewEvents", "editEvents"},
        "notifications", new String[] {"sendNotifications", "sendNotifications"},
        "importLog", new String[] {"importData", "importData"});
    private static final int MAX_BYTES = 64 * 1024;
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;
    private final ObjectMapper json;

    public DocumentsController(JdbcClient jdbc, CurrentUser current, Perms perms, ObjectMapper json) {
        this.jdbc = jdbc; this.current = current; this.perms = perms; this.json = json;
    }

    private String[] rule(String collection) {
        String[] r = COLLECTIONS.get(collection);
        if (r == null) throw ApiException.notFound("Unknown list: " + collection);
        return r;
    }

    @GetMapping("/{collection}")
    public List<Object> list(@PathVariable String collection) {
        Me me = current.get();
        perms.need(me, rule(collection)[0]);
        List<Object> out = new ArrayList<>();
        for (String d : jdbc.sql("SELECT data FROM college_documents WHERE college_id = ? AND collection = ? ORDER BY updated_at DESC LIMIT 5000")
                .param(me.collegeId()).param(collection).query(String.class).list()) {
            try { out.add(json.readTree(d)); } catch (Exception ignored) { }
        }
        return out;
    }

    @PutMapping("/{collection}/{id}")
    public Map<String, Object> put(@PathVariable String collection, @PathVariable String id, @RequestBody JsonNode body) throws Exception {
        Me me = current.get();
        perms.need(me, rule(collection)[1]);
        if (!body.isObject()) throw ApiException.bad("Expected a JSON object.");
        if (id.length() > 120 || !id.matches("[A-Za-z0-9_.:-]+")) throw ApiException.bad("Invalid id.");
        httpsOnly(body);
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("id", id);
        String data = json.writeValueAsString(body);
        if (data.length() > MAX_BYTES) throw ApiException.bad("This record is too large.");
        jdbc.sql("INSERT INTO college_documents(college_id, collection, id, data, updated_by) VALUES (?,?,?,?,?) AS n "
                + "ON DUPLICATE KEY UPDATE data = n.data, updated_by = n.updated_by")
            .param(me.collegeId()).param(collection).param(id).param(data).param(me.id()).update();
        return Map.of("id", id);
    }

    @DeleteMapping("/{collection}/{id}")
    public Map<String, Object> delete(@PathVariable String collection, @PathVariable String id) {
        Me me = current.get();
        // Faculty can delete classes by default; other lists need "Delete records".
        perms.need(me, "classes".equals(collection) ? "editClasses" : "deleteRecords");
        rule(collection);
        jdbc.sql("DELETE FROM college_documents WHERE college_id = ? AND collection = ? AND id = ?").param(me.collegeId()).param(collection).param(id).update();
        return Map.of("ok", true);
    }

    private static void httpsOnly(JsonNode n) {
        if (n.isTextual() && n.asText().toLowerCase().startsWith("http://")) throw ApiException.bad("Links must start with https://");
        n.elements().forEachRemaining(DocumentsController::httpsOnly);
    }
}
