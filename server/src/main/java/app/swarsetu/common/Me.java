package app.swarsetu.common;

/** The signed-in user, loaded fresh from the database on every request. collegeId scopes every query. */
public record Me(String id, String collegeId, String role, String studentId, String name) {
    public boolean isAdmin() { return "admin".equals(role); }
    public boolean isStudent() { return "student".equals(role); }
}
