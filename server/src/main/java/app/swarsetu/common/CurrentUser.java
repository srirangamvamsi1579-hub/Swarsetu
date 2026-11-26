package app.swarsetu.common;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Resolves the signed-in user. The token only says who you are; role, college and status
 * always come from the database, so a deactivated user or changed role takes effect at once.
 */
@Component
public class CurrentUser {
    private final JdbcClient jdbc;

    public CurrentUser(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Me get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (!(a instanceof JwtAuthenticationToken t)) throw ApiException.unauthorized("Please sign in.");
        String uid = t.getToken().getSubject();
        String col = t.getToken().getClaimAsString("col");
        Me me = jdbc.sql("SELECT id, college_id, role, status, student_id, first_name, last_name FROM users WHERE id = ?")
            .param(uid)
            .query((rs, n) -> "active".equals(rs.getString("status"))
                ? new Me(rs.getString("id"), rs.getString("college_id"), rs.getString("role"), rs.getString("student_id"),
                    (rs.getString("first_name") + " " + (rs.getString("last_name") == null ? "" : rs.getString("last_name"))).trim())
                : null)
            .optional().orElse(null);
        if (me == null || !me.collegeId().equals(col)) throw ApiException.unauthorized("Your session has ended. Please sign in again.");
        return me;
    }
}
