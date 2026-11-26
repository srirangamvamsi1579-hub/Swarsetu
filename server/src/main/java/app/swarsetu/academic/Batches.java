package app.swarsetu.academic;

import app.swarsetu.common.ApiException;
import app.swarsetu.common.Crypto;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Batch = course + weekdays + timing + mode for groups; a private batch per one-to-one student.
 * "Students per batch" (college.group_size) limits ONLINE group batches only. Empty batches are removed.
 */
@Service
public class Batches {
    private final JdbcClient jdbc;

    public Batches(JdbcClient jdbc) { this.jdbc = jdbc; }

    public record Slot(String courseId, String classType, String mode, String weekdays, String startTime, int lengthMin) {}

    static String key(Slot s, String studentId) {
        String base = s.courseId() + "|" + s.weekdays() + "|" + s.startTime() + "|" + s.mode();
        return "One-to-One".equals(s.classType()) ? base + "|1:1|" + studentId : base + "|G";
    }

    /** Finds or creates the batch for this slot and checks capacity. Call inside the student save transaction. */
    public String assign(String collegeId, String studentId, Slot s, String currentBatchId) {
        String key = key(s, studentId);
        Map<String, Object> b = jdbc.sql("SELECT id FROM batches WHERE college_id = ? AND batch_key = ? FOR UPDATE")
            .param(collegeId).param(key).query().listOfRows().stream().findFirst().orElse(null);
        String batchId;
        if (b == null) {
            String course = jdbc.sql("SELECT name FROM courses WHERE college_id = ? AND id = ?").param(collegeId).param(s.courseId())
                .query(String.class).optional().orElseThrow(() -> ApiException.bad("Course not found in your college."));
            batchId = Crypto.id("bat");
            String name = course + " · " + s.weekdays() + " " + s.startTime() + " · " + s.mode() + ("One-to-One".equals(s.classType()) ? " · 1:1" : "");
            jdbc.sql("INSERT INTO batches(id, college_id, course_id, batch_key, name, class_type, mode, weekdays, start_time, length_min) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?)")
                .param(batchId).param(collegeId).param(s.courseId()).param(key).param(name).param(s.classType()).param(s.mode())
                .param(s.weekdays()).param(s.startTime()).param(s.lengthMin()).update();
        } else {
            batchId = (String) b.get("id");
        }
        if (!batchId.equals(currentBatchId) && "Group".equals(s.classType()) && "Online".equals(s.mode())) {
            int size = jdbc.sql("SELECT group_size FROM colleges WHERE id = ?").param(collegeId).query(Integer.class).single();
            int n = jdbc.sql("SELECT COUNT(*) FROM students WHERE college_id = ? AND batch_id = ? AND status = 'Active'")
                .param(collegeId).param(batchId).query(Integer.class).single();
            if (n >= size) throw ApiException.conflict("This online group batch is full (" + size + " students). Choose another time or raise the limit in College Profile.");
        }
        return batchId;
    }

    /** Removes batches with no students. Payments keep the batch name for history. */
    public int removeEmpty(String collegeId) {
        var empty = jdbc.sql("SELECT b.id FROM batches b WHERE b.college_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.college_id = b.college_id AND s.batch_id = b.id)")
            .param(collegeId).query(String.class).list();
        for (String id : empty) {
            jdbc.sql("UPDATE payments SET batch_id = NULL WHERE college_id = ? AND batch_id = ?").param(collegeId).param(id).update();
            jdbc.sql("DELETE FROM college_documents WHERE college_id = ? AND collection = 'ttSessions' AND JSON_UNQUOTE(JSON_EXTRACT(data, '$.batchId')) = ?")
                .param(collegeId).param(id).update();
            jdbc.sql("DELETE FROM batches WHERE college_id = ? AND id = ?").param(collegeId).param(id).update();
        }
        return empty.size();
    }
}
