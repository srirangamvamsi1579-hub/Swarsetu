package app.swarsetu.academic;

import app.swarsetu.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Courses, students and batches. Every query is limited to the signed-in user's college. */
@RestController
@RequestMapping("/api")
public class AcademicController {
    private static final Set<String> DAYS = Set.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;
    private final Batches batches;
    private final Audit audit;

    public AcademicController(JdbcClient jdbc, CurrentUser current, Perms perms, Batches batches, Audit audit) {
        this.jdbc = jdbc; this.current = current; this.perms = perms; this.batches = batches; this.audit = audit;
    }

    // ---------------- courses ----------------

    public record CourseIn(@NotBlank @Size(max = 120) String name, @Size(max = 500) String description, @Size(max = 40) String level,
                           @Size(max = 120) String instructor, @Size(max = 60) String duration, @Pattern(regexp = "Active|Inactive") String status) {}

    @GetMapping("/courses")
    public List<Map<String, Object>> courses() {
        Me me = current.get();
        perms.need(me, "viewCourses");
        return Rows.camel(jdbc.sql("SELECT c.*, (SELECT COUNT(*) FROM students s WHERE s.college_id = c.college_id AND s.course_id = c.id) AS students "
            + "FROM courses c WHERE c.college_id = ? ORDER BY c.name").param(me.collegeId()).query().listOfRows());
    }

    @PostMapping("/courses")
    public Map<String, Object> addCourse(@Valid @RequestBody CourseIn c) {
        Me me = current.get();
        perms.need(me, "editCourses");
        String id = Crypto.id("crs");
        jdbc.sql("INSERT INTO courses(id, college_id, name, description, level, instructor, duration, status) VALUES (?,?,?,?,?,?,?,?)")
            .param(id).param(me.collegeId()).param(c.name().trim()).param(c.description()).param(c.level()).param(c.instructor())
            .param(c.duration()).param(c.status() == null ? "Active" : c.status()).update();
        audit.log(me, "course.added", "course", id, Map.of("name", c.name()));
        return Map.of("id", id);
    }

    @PutMapping("/courses/{id}")
    public Map<String, Object> editCourse(@PathVariable String id, @Valid @RequestBody CourseIn c) {
        Me me = current.get();
        perms.need(me, "editCourses");
        int n = jdbc.sql("UPDATE courses SET name = ?, description = ?, level = ?, instructor = ?, duration = ?, status = ?, version = version + 1 "
                + "WHERE college_id = ? AND id = ?")
            .param(c.name().trim()).param(c.description()).param(c.level()).param(c.instructor()).param(c.duration())
            .param(c.status() == null ? "Active" : c.status()).param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Course not found.");
        audit.log(me, "course.edited", "course", id, null);
        return Map.of("id", id);
    }

    /** RESTRICT if students or batches use it; otherwise its academic levels and faculty slots go with it. */
    @DeleteMapping("/courses/{id}")
    @Transactional
    public Map<String, Object> deleteCourse(@PathVariable String id) {
        Me me = current.get();
        perms.need(me, "deleteRecords");
        int used = jdbc.sql("SELECT COUNT(*) FROM students WHERE college_id = ? AND course_id = ?").param(me.collegeId()).param(id).query(Integer.class).single();
        if (used > 0) throw ApiException.conflict("This course has " + used + " student(s). Move them to another course first.");
        jdbc.sql("DELETE FROM college_documents WHERE college_id = ? AND collection IN ('levels','facultySlots') "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(data, '$.courseId')) = ?").param(me.collegeId()).param(id).update();
        int n = jdbc.sql("DELETE FROM courses WHERE college_id = ? AND id = ?").param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Course not found.");
        audit.log(me, "course.deleted", "course", id, null);
        return Map.of("ok", true);
    }

    // ---------------- students ----------------

    public record StudentIn(
        @NotBlank @Size(max = 60) String firstName, @Size(max = 60) String lastName,
        @Size(max = 120) String email, @Size(max = 15) String mobile, @Size(max = 120) String parent,
        @Size(max = 20) String ageGroup, @Size(max = 80) String city, @Size(max = 80) String country,
        @NotBlank String courseId, @Size(max = 40) String level,
        @NotBlank @Pattern(regexp = "Group|One-to-One") String classType,
        @NotBlank @Pattern(regexp = "Online|Offline") String mode,
        @NotBlank String weekdays,
        @NotBlank @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]") String startTime,
        @Min(15) @Max(240) Integer lengthMin,
        @DecimalMin("0") BigDecimal monthlyFee, @DecimalMin("0") BigDecimal joiningFee,
        String joiningDate, @Pattern(regexp = "Active|Inactive") String status) {}

    @GetMapping("/students")
    public List<Map<String, Object>> students(@RequestParam(required = false) String q, @RequestParam(required = false) String courseId,
                                              @RequestParam(required = false) String batchId, @RequestParam(required = false) String status) {
        Me me = current.get();
        StringBuilder sql = new StringBuilder("SELECT s.*, c.name AS course_name, b.name AS batch_name FROM students s "
            + "JOIN courses c ON c.college_id = s.college_id AND c.id = s.course_id "
            + "LEFT JOIN batches b ON b.college_id = s.college_id AND b.id = s.batch_id WHERE s.college_id = ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId()));
        if (me.isStudent()) {                       // a student only ever sees their own record
            sql.append(" AND s.id = ?");
            ps.add(me.studentId() == null ? "-" : me.studentId());
        } else {
            perms.need(me, "viewStudents");
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (s.id LIKE ? OR CONCAT(s.first_name, ' ', IFNULL(s.last_name, '')) LIKE ? OR s.email LIKE ? OR s.mobile LIKE ?)");
            String like = "%" + q.trim() + "%";
            ps.addAll(List.of(like, like, like, like));
        }
        if (courseId != null && !courseId.isBlank()) { sql.append(" AND s.course_id = ?"); ps.add(courseId); }
        if (batchId != null && !batchId.isBlank()) { sql.append(" AND s.batch_id = ?"); ps.add(batchId); }
        if (status != null && !status.isBlank()) { sql.append(" AND s.status = ?"); ps.add(status); }
        sql.append(" ORDER BY s.id");
        return Rows.camel(jdbc.sql(sql.toString()).params(ps).query().listOfRows());
    }

    private String weekdays(String w) {
        List<String> days = Arrays.stream(w.split("[,\\s]+")).filter(s -> !s.isBlank()).map(String::trim).distinct().toList();
        if (days.isEmpty() || !DAYS.containsAll(days)) throw ApiException.bad("Weekdays must be from Mon, Tue, Wed, Thu, Fri, Sat, Sun.");
        List<String> order = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        return String.join(",", days.stream().sorted(Comparator.comparingInt(order::indexOf)).toList());
    }

    /** STU0001-HIN: next number for the college + first three letters of the course. */
    private String nextStudentId(String collegeId, String courseId) {
        jdbc.sql("SELECT id FROM colleges WHERE id = ? FOR UPDATE").param(collegeId).query(String.class).single(); // serialise numbering
        int next = jdbc.sql("SELECT IFNULL(MAX(CAST(SUBSTRING(id, 4, 4) AS UNSIGNED)), 0) + 1 FROM students WHERE college_id = ? AND id REGEXP '^STU[0-9]{4}'")
            .param(collegeId).query(Integer.class).single();
        String course = jdbc.sql("SELECT name FROM courses WHERE college_id = ? AND id = ?").param(collegeId).param(courseId)
            .query(String.class).optional().orElseThrow(() -> ApiException.bad("Course not found in your college."));
        String abbr = (course.replaceAll("[^A-Za-z]", "") + "XXX").substring(0, 3).toUpperCase();
        return String.format("STU%04d-%s", next, abbr);
    }

    @PostMapping("/students")
    @Transactional
    public Map<String, Object> addStudent(@Valid @RequestBody StudentIn s) {
        Me me = current.get();
        perms.need(me, "editStudents");
        String id = nextStudentId(me.collegeId(), s.courseId());
        String wd = weekdays(s.weekdays());
        // Insert first without a batch so the batch check sees this student's id, then link.
        jdbc.sql("INSERT INTO students(id, college_id, first_name, last_name, email, mobile, parent, age_group, city, country, course_id, level, "
                + "class_type, mode, weekdays, start_time, length_min, monthly_fee, joining_fee, joining_date, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
            .param(id).param(me.collegeId()).param(s.firstName().trim()).param(s.lastName())
            .param(s.email() == null || s.email().isBlank() ? null : Check.email(s.email(), false)).param(Check.mobile(s.mobile(), false))
            .param(s.parent()).param(s.ageGroup()).param(s.city()).param(s.country()).param(s.courseId()).param(s.level())
            .param(s.classType()).param(s.mode()).param(wd).param(s.startTime()).param(s.lengthMin() == null ? 30 : s.lengthMin())
            .param(s.monthlyFee()).param(s.joiningFee()).param(s.joiningDate() == null || s.joiningDate().isBlank() ? null : Check.date(s.joiningDate(), "Joining date"))
            .param(s.status() == null ? "Active" : s.status()).update();
        String batch = batches.assign(me.collegeId(), id,
            new Batches.Slot(s.courseId(), s.classType(), s.mode(), wd, s.startTime(), s.lengthMin() == null ? 30 : s.lengthMin()), null);
        jdbc.sql("UPDATE students SET batch_id = ? WHERE college_id = ? AND id = ?").param(batch).param(me.collegeId()).param(id).update();
        audit.log(me, "student.added", "student", id, Map.of("batch", batch));
        return Map.of("id", id, "batchId", batch);
    }

    @PutMapping("/students/{id}")
    @Transactional
    public Map<String, Object> editStudent(@PathVariable String id, @Valid @RequestBody StudentIn s) {
        Me me = current.get();
        perms.need(me, "editStudents");
        String cur = jdbc.sql("SELECT IFNULL(batch_id, '') FROM students WHERE college_id = ? AND id = ? FOR UPDATE").param(me.collegeId()).param(id)
            .query(String.class).optional().orElseThrow(() -> ApiException.notFound("Student not found."));
        String wd = weekdays(s.weekdays());
        int len = s.lengthMin() == null ? 30 : s.lengthMin();
        String batch = batches.assign(me.collegeId(), id, new Batches.Slot(s.courseId(), s.classType(), s.mode(), wd, s.startTime(), len), cur);
        jdbc.sql("UPDATE students SET first_name = ?, last_name = ?, email = ?, mobile = ?, parent = ?, age_group = ?, city = ?, country = ?, "
                + "course_id = ?, batch_id = ?, level = ?, class_type = ?, mode = ?, weekdays = ?, start_time = ?, length_min = ?, monthly_fee = ?, "
                + "joining_fee = ?, joining_date = ?, status = ?, version = version + 1 WHERE college_id = ? AND id = ?")
            .param(s.firstName().trim()).param(s.lastName())
            .param(s.email() == null || s.email().isBlank() ? null : Check.email(s.email(), false)).param(Check.mobile(s.mobile(), false))
            .param(s.parent()).param(s.ageGroup()).param(s.city()).param(s.country()).param(s.courseId()).param(batch).param(s.level())
            .param(s.classType()).param(s.mode()).param(wd).param(s.startTime()).param(len).param(s.monthlyFee()).param(s.joiningFee())
            .param(s.joiningDate() == null || s.joiningDate().isBlank() ? null : Check.date(s.joiningDate(), "Joining date"))
            .param(s.status() == null ? "Active" : s.status()).param(me.collegeId()).param(id).update();
        batches.removeEmpty(me.collegeId());
        audit.log(me, "student.edited", "student", id, Map.of("batch", batch));
        return Map.of("id", id, "batchId", batch);
    }

    /** RESTRICT if the student has payments; otherwise attendance, notices and claims go with them (FK cascade). */
    @DeleteMapping("/students/{id}")
    @Transactional
    public Map<String, Object> deleteStudent(@PathVariable String id) {
        Me me = current.get();
        perms.need(me, "deleteRecords");
        int pays = jdbc.sql("SELECT COUNT(*) FROM payments WHERE college_id = ? AND student_id = ?").param(me.collegeId()).param(id).query(Integer.class).single();
        if (pays > 0) throw ApiException.conflict("This student has " + pays + " fee payment(s). Mark the student Inactive instead, so fee history is kept.");
        jdbc.sql("UPDATE users SET student_id = NULL WHERE college_id = ? AND student_id = ?").param(me.collegeId()).param(id).update();
        int n = jdbc.sql("DELETE FROM students WHERE college_id = ? AND id = ?").param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Student not found.");
        batches.removeEmpty(me.collegeId());
        audit.log(me, "student.deleted", "student", id, null);
        return Map.of("ok", true);
    }

    // ---------------- batches ----------------

    @GetMapping("/batches")
    public List<Map<String, Object>> listBatches() {
        Me me = current.get();
        if (me.isStudent()) {
            return Rows.camel(jdbc.sql("SELECT b.* FROM batches b JOIN students s ON s.college_id = b.college_id AND s.batch_id = b.id "
                + "WHERE b.college_id = ? AND s.id = ?").param(me.collegeId()).param(me.studentId() == null ? "-" : me.studentId()).query().listOfRows());
        }
        perms.need(me, "viewTimetable");
        return Rows.camel(jdbc.sql("SELECT b.*, c.name AS course_name, (SELECT COUNT(*) FROM students s WHERE s.college_id = b.college_id AND s.batch_id = b.id) AS students "
            + "FROM batches b JOIN courses c ON c.college_id = b.college_id AND c.id = b.course_id WHERE b.college_id = ? ORDER BY c.name, b.weekdays, b.start_time")
            .param(me.collegeId()).query().listOfRows());
    }

    public record BatchIn(@Size(max = 150) String location, @Size(max = 300) String meetLink) {}

    @PutMapping("/batches/{id}")
    public Map<String, Object> editBatch(@PathVariable String id, @Valid @RequestBody BatchIn b) {
        Me me = current.get();
        perms.need(me, "editTimetable");
        Check.https(b.meetLink(), "Meeting link");
        int n = jdbc.sql("UPDATE batches SET location = ?, meet_link = ?, version = version + 1 WHERE college_id = ? AND id = ?")
            .param(b.location()).param(b.meetLink() == null || b.meetLink().isBlank() ? null : b.meetLink().trim()).param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Batch not found.");
        return Map.of("id", id);
    }
}
