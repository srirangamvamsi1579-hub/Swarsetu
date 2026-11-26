package app.swarsetu.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Roles & Permissions matrix, identical to the web app: admin always; faculty/student from the college's matrix or defaults. */
@Service
public class Perms {
    public static final List<String> ALL = List.of(
        "viewDashboard", "viewCollege", "editCollege", "viewStudents", "editStudents", "viewCourses", "editCourses",
        "viewFaculty", "editFaculty", "viewTimetable", "editTimetable", "viewCalendar", "manageCalendar",
        "viewAttendance", "editAttendance", "viewClasses", "editClasses", "shareClasses", "viewFees", "editFees",
        "sendNotifications", "viewEvents", "editEvents", "importData", "exportData", "manageUsers", "deleteRecords");
    /** The only permissions a student can ever be given (always limited to their own records). */
    public static final Set<String> STUDENT_OK = Set.of(
        "viewCollege", "viewCourses", "viewTimetable", "viewCalendar", "viewAttendance", "viewClasses", "viewFees", "viewEvents");
    private static final Set<String> FACULTY_OFF = Set.of(
        "viewCollege", "editCollege", "manageCalendar", "manageUsers", "deleteRecords", "importData");
    private static final Set<String> STUDENT_ON = Set.of(
        "viewTimetable", "viewCalendar", "viewAttendance", "viewClasses", "viewFees", "viewEvents");

    private final JdbcClient jdbc;

    public Perms(JdbcClient jdbc) { this.jdbc = jdbc; }

    public static boolean byDefault(String role, String perm) {
        if ("faculty".equals(role)) return !FACULTY_OFF.contains(perm);
        if ("student".equals(role)) return STUDENT_ON.contains(perm);
        return false;
    }

    public boolean can(Me me, String perm) {
        if (me.isAdmin()) return true;
        if (!ALL.contains(perm)) return false;
        if (me.isStudent() && !STUDENT_OK.contains(perm)) return false;
        return jdbc.sql("SELECT allowed FROM role_permissions WHERE college_id = ? AND role = ? AND permission = ?")
            .param(me.collegeId()).param(me.role()).param(perm)
            .query(Boolean.class).optional().orElse(byDefault(me.role(), perm));
    }

    public void need(Me me, String perm) {
        if (!can(me, perm)) throw ApiException.forbidden("Access denied — your role does not have the \"" + perm + "\" permission.");
    }

    public void needAdmin(Me me) {
        if (!me.isAdmin()) throw ApiException.forbidden("Only an Admin can do this.");
    }

    /** Effective matrix for a college: {faculty:{perm:bool}, student:{perm:bool}}. */
    public Map<String, Map<String, Boolean>> matrix(String collegeId) {
        Map<String, Map<String, Boolean>> m = new LinkedHashMap<>();
        for (String role : List.of("faculty", "student")) {
            Map<String, Boolean> r = new LinkedHashMap<>();
            for (String p : ALL) r.put(p, byDefault(role, p) && (!"student".equals(role) || STUDENT_OK.contains(p)));
            m.put(role, r);
        }
        jdbc.sql("SELECT role, permission, allowed FROM role_permissions WHERE college_id = ?").param(collegeId)
            .query(rs -> {
                String role = rs.getString("role"), p = rs.getString("permission");
                if (m.get(role).containsKey(p) && (!"student".equals(role) || STUDENT_OK.contains(p))) {
                    m.get(role).put(p, rs.getBoolean("allowed"));
                }
            });
        return m;
    }
}
