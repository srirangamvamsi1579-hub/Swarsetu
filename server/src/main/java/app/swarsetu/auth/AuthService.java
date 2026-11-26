package app.swarsetu.auth;

import app.swarsetu.auth.AuthDtos.*;
import app.swarsetu.common.ApiException;
import app.swarsetu.common.Audit;
import app.swarsetu.common.Check;
import app.swarsetu.common.Crypto;
import app.swarsetu.common.Me;
import app.swarsetu.common.Rows;
import app.swarsetu.config.AppProps;
import app.swarsetu.inbox.Inbox;
import app.swarsetu.notify.Outbox;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Set<String> STOP = Set.of("the", "of", "and", "for", "a", "an", "&");
    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final Verifications verifications;
    private final Tokens tokens;
    private final Audit audit;
    private final Outbox outbox;
    private final Inbox inbox;
    private final AppProps props;
    private final String dummyHash;

    public AuthService(JdbcClient jdbc, PasswordEncoder encoder, Verifications verifications, Tokens tokens,
                       Audit audit, Outbox outbox, Inbox inbox, AppProps props) {
        this.jdbc = jdbc; this.encoder = encoder; this.verifications = verifications; this.tokens = tokens;
        this.audit = audit; this.outbox = outbox; this.inbox = inbox; this.props = props;
        this.dummyHash = encoder.encode("timing-equaliser-" + UUID.randomUUID());
    }

    // ---------- names and codes ----------

    /** "The Swara Music School" -> "swaramusicschool": the unique key that stops duplicate college names. */
    public static String nameKey(String name) {
        String n = name.trim().toLowerCase().replaceFirst("^the\\s+", "");
        return n.replaceAll("[^a-z0-9]", "");
    }

    /** Initials of the significant words, e.g. "Raga Academy of Music" -> "RAM". */
    static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        String[] words = name.trim().split("[^A-Za-z]+");
        for (String w : words) if (!w.isEmpty() && !STOP.contains(w.toLowerCase()) && sb.length() < 3) sb.append(Character.toUpperCase(w.charAt(0)));
        for (String w : words) for (char c : w.toCharArray()) if (sb.length() < 3 && Character.isLetter(c)) sb.append(Character.toUpperCase(c));
        while (sb.length() < 3) sb.append('X');
        return sb.substring(0, 3);
    }

    private String newCollegeCode(String name) {
        String pre = initials(name);
        for (int i = 0; i < 50; i++) {
            String code = pre + "-" + Crypto.chars(4);
            Integer n = jdbc.sql("SELECT COUNT(*) FROM colleges WHERE id = ?").param(code).query(Integer.class).single();
            if (n == 0) return code;
        }
        throw new IllegalStateException("Could not generate a college code");
    }

    // ---------- shared checks ----------

    /** A sign-in ID (username, email or mobile) must not match ANY existing username, email or mobile. */
    private void ensureFree(String value, String label) {
        if (value == null) return;
        Integer n = jdbc.sql("SELECT COUNT(*) FROM users WHERE username_lc = LOWER(?) OR email = LOWER(?) OR mobile = ?")
            .param(value).param(value).param(value).query(Integer.class).single();
        if (n > 0) throw ApiException.conflict(label + " is already registered. Use Forgot password if it is yours.");
    }

    private record Ids(String username, String email, String mobile) {}

    private Ids checkIds(String username, String email, String mobile, boolean gmailOnly) {
        String e = Check.email(email, gmailOnly);
        String m = Check.mobile(mobile, false);
        String u = username == null || username.isBlank() ? e : username.trim();
        if (!Check.USERNAME.matcher(u).matches()) throw ApiException.bad("Username: 3 to 60 letters, digits or . _ @ -");
        if (u.matches("[0-9]+") && !u.equals(m)) throw ApiException.bad("A username made of digits must be your own 10-digit mobile number.");
        if (u.contains("@") && !u.equalsIgnoreCase(e)) throw ApiException.bad("A username that is an email must be your own email address.");
        ensureFree(u, "That username");
        if (!u.equalsIgnoreCase(e)) ensureFree(e, "That email address");
        if (m != null && !m.equals(u)) ensureFree(m, "That mobile number");
        return new Ids(u, e, m);
    }

    // ---------- registration ----------

    public Map<String, Object> startCollege(NewCollege r) {
        String key = nameKey(r.collegeName());
        if (key.length() < 3) throw ApiException.bad("Enter the full college name.");
        Integer dup = jdbc.sql("SELECT COUNT(*) FROM colleges WHERE name_key = ?").param(key).query(Integer.class).single();
        if (dup > 0) throw ApiException.conflict("A college with this name is already registered. Ask its Admin for the college code to join.");
        Check.password(r.password());
        Ids ids = checkIds(r.username(), r.email(), r.mobile(), false);
        Map<String, Object> p = new HashMap<>();
        p.put("kind", "college");
        p.put("collegeName", r.collegeName().trim());
        p.put("affiliation", r.affiliation());
        p.put("city", r.city().trim());
        p.put("country", r.country());
        p.put("firstName", r.firstName().trim());
        p.put("lastName", r.lastName());
        p.put("username", ids.username());
        p.put("countryCode", cc(r.countryCode()));
        p.put("passwordHash", encoder.encode(r.password()));
        return verifications.start("register", null, p, null, ids.email(), cc(r.countryCode()), ids.mobile(), "register your college");
    }

    public Map<String, Object> startJoin(Join r) {
        if (!Set.of("faculty", "student").contains(r.role())) throw ApiException.bad("Choose Faculty or Student.");
        Map<String, Object> col = findCollege(r.collegeCode(), r.collegeName());
        Check.password(r.password());
        Ids ids = checkIds(r.username(), r.email(), r.mobile(), Rows.truthy(col.get("require_gmail")));
        Map<String, Object> p = new HashMap<>();
        p.put("kind", "join");
        p.put("collegeId", col.get("id"));
        p.put("role", r.role());
        p.put("firstName", r.firstName().trim());
        p.put("lastName", r.lastName());
        p.put("username", ids.username());
        p.put("countryCode", cc(r.countryCode()));
        p.put("city", r.city());
        p.put("country", r.country());
        p.put("passwordHash", encoder.encode(r.password()));
        Map<String, Object> res = verifications.start("register", null, p, (String) col.get("id"), ids.email(), cc(r.countryCode()), ids.mobile(),
            "join " + col.get("name"));
        res.put("collegeName", col.get("name"));
        return res;
    }

    /** Exact code, or exact name (case/punctuation-insensitive). Never lists colleges. */
    public Map<String, Object> findCollege(String code, String name) {
        List<Map<String, Object>> rows;
        if (code != null && !code.isBlank()) {
            rows = jdbc.sql("SELECT id, name, city, require_gmail, approve_signups FROM colleges WHERE id = UPPER(?)").param(code.trim()).query().listOfRows();
        } else if (name != null && !name.isBlank()) {
            rows = jdbc.sql("SELECT id, name, city, require_gmail, approve_signups FROM colleges WHERE name_key = ?").param(nameKey(name)).query().listOfRows();
        } else {
            throw ApiException.bad("Enter your college code or the exact college name.");
        }
        return rows.stream().findFirst().orElseThrow(() -> ApiException.notFound("No college matches. Check the code with your college office."));
    }

    @Transactional(noRollbackFor = Verifications.WrongCodeException.class)
    public Map<String, Object> verifyRegistration(Verify v, String ua, String ip) {
        Verifications.Pending p = verifications.consume(v.verificationId(), "register", v.emailCode(), v.mobileCode(), true);
        Map<String, Object> d = p.payload();
        String uid = Crypto.id("usr");
        Map<String, Object> res = new LinkedHashMap<>();
        if ("college".equals(d.get("kind"))) {
            String code = newCollegeCode((String) d.get("collegeName"));
            jdbc.sql("INSERT INTO colleges(id, name, name_key, affiliation, city, country, email, phone) VALUES (?,?,?,?,?,?,?,?)")
                .param(code).param(d.get("collegeName")).param(nameKey((String) d.get("collegeName"))).param(d.get("affiliation"))
                .param(d.get("city")).param(d.get("country")).param(p.email()).param(p.mobile()).update();
            insertUser(uid, code, d, p, "admin", "active");
            audit.log(code, uid, "college.created", "college", code, Map.of("name", d.get("collegeName")));
            outbox.email(code, p.email(), "Your college is registered: " + code,
                "Welcome to Swarsetu.\n\nCollege: " + d.get("collegeName") + "\nCollege code: " + code
                + "\n\nShare this code with your faculty and students so they can join: " + props.appUrl()
                + "\nYou are the Admin. Approve join requests under User Management.", "college-welcome:" + code);
            res.put("collegeCode", code);
            res.put("status", "active");
            res.putAll(tokens.issue(uid, code, "admin", null, ua, ip));
            res.put("user", userJson(uid));
        } else {
            String col = (String) d.get("collegeId");
            Map<String, Object> c = jdbc.sql("SELECT name, approve_signups FROM colleges WHERE id = ?").param(col).query().singleRow();
            boolean approve = Rows.truthy(c.get("approve_signups"));
            String status = approve ? "pending" : "active";
            insertUser(uid, col, d, p, (String) d.get("role"), status);
            audit.log(col, uid, "user.registered", "user", uid, Map.of("role", d.get("role"), "status", status));
            String who = d.get("firstName") + " " + (d.get("lastName") == null ? "" : d.get("lastName"));
            inbox.toAudience(col, "manageUsers", "join", approve ? "New join request" : "New member joined",
                who.trim() + " registered as " + d.get("role") + (approve ? " and is waiting for approval." : "."), "users", "join:" + uid);
            res.put("collegeCode", col);
            res.put("collegeName", c.get("name"));
            res.put("status", status);
            if (!approve) { res.putAll(tokens.issue(uid, col, (String) d.get("role"), null, ua, ip)); res.put("user", userJson(uid)); }
        }
        return res;
    }

    private void insertUser(String uid, String col, Map<String, Object> d, Verifications.Pending p, String role, String status) {
        jdbc.sql("INSERT INTO users(id, college_id, username, email, mobile, country_code, first_name, last_name, city, country, role, status, "
                + "password_hash, password_set_at, email_verified_at, mobile_verified_at, approved_at) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?, NOW(3), NOW(3), IF(? IS NULL, NULL, NOW(3)), IF(? = 'active', NOW(3), NULL))")
            .param(uid).param(col).param(d.get("username")).param(p.email()).param(p.mobile()).param(d.get("countryCode"))
            .param(d.get("firstName")).param(d.get("lastName")).param(d.get("city")).param(d.get("country"))
            .param(role).param(status).param(d.get("passwordHash")).param(p.mobile()).param(status).update();
    }

    // ---------- sign in ----------

    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> login(Login l, String ua, String ip) {
        String id = l.identifier().trim();
        String digits = id.replaceAll("[\\s-]", "");
        Map<String, Object> u = jdbc.sql("SELECT id, college_id, role, status, password_hash, locked_until > NOW(3) AS locked, "
                + "TIMESTAMPDIFF(MINUTE, NOW(3), locked_until) + 1 AS lock_min, reject_reason FROM users "
                + "WHERE username_lc = LOWER(?) OR email = LOWER(?) OR mobile = ? ORDER BY (username_lc = LOWER(?)) DESC LIMIT 1 FOR UPDATE")
            .param(id).param(id).param(digits).param(id).query().listOfRows().stream().findFirst().orElse(null);
        if (u == null) {
            encoder.matches(l.password(), dummyHash); // same time whether or not the account exists
            throw ApiException.unauthorized("Wrong sign-in ID or password.");
        }
        String uid = (String) u.get("id");
        if (Rows.truthy(u.get("locked"))) {
            throw new ApiException(HttpStatus.LOCKED, "Too many wrong passwords. Try again in " + u.get("lock_min") + " minute(s), or use Forgot password.");
        }
        if (!encoder.matches(l.password(), (String) u.get("password_hash"))) {
            jdbc.sql("UPDATE users SET locked_until = IF(failed_count + 1 >= ?, NOW(3) + INTERVAL ? MINUTE, NULL), "
                    + "failed_count = IF(failed_count + 1 >= ?, 0, failed_count + 1) WHERE id = ?")
                .param(props.maxFailures()).param(props.lockMinutes()).param(props.maxFailures()).param(uid).update();
            audit.log((String) u.get("college_id"), uid, "login.failed", "user", uid, Map.of("ip", String.valueOf(ip)));
            throw ApiException.unauthorized("Wrong sign-in ID or password.");
        }
        switch ((String) u.get("status")) {
            case "pending" -> throw ApiException.forbidden("Your account is waiting for approval by the college Admin.");
            case "rejected" -> throw ApiException.forbidden("Your join request was not approved" + (u.get("reject_reason") == null ? "." : ": " + u.get("reject_reason")));
            case "inactive" -> throw ApiException.forbidden("Your account is inactive. Please contact the college office.");
            default -> { }
        }
        jdbc.sql("UPDATE users SET failed_count = 0, locked_until = NULL, prev_login_at = last_login_at, last_login_at = NOW(3) WHERE id = ?")
            .param(uid).update();
        audit.log((String) u.get("college_id"), uid, "login.ok", "user", uid, Map.of("ip", String.valueOf(ip)));
        Map<String, Object> res = new LinkedHashMap<>(tokens.issue(uid, (String) u.get("college_id"), (String) u.get("role"), null, ua, ip));
        res.put("user", userJson(uid));
        return res;
    }

    // ---------- forgot password ----------

    public Map<String, Object> forgot(Forgot f) {
        String id = f.identifier().trim();
        Map<String, Object> u = jdbc.sql("SELECT id, college_id, email, mobile, country_code FROM users "
                + "WHERE (username_lc = LOWER(?) OR email = LOWER(?) OR mobile = ?) AND status IN ('active','pending') LIMIT 1")
            .param(id).param(id).param(id.replaceAll("[\\s-]", "")).query().listOfRows().stream().findFirst().orElse(null);
        Map<String, Object> res = new LinkedHashMap<>();
        if (u != null) {
            Map<String, Object> v = verifications.start("reset", (String) u.get("id"), null, (String) u.get("college_id"),
                (String) u.get("email"), (String) u.get("country_code"), (String) u.get("mobile"), "reset your password");
            res.put("verificationId", v.get("verificationId"));
            if (v.containsKey("simulated")) res.put("simulated", v.get("simulated"));
        } else {
            res.put("verificationId", UUID.randomUUID().toString()); // same answer either way: no account discovery
        }
        res.put("message", "If an account matches, a code has been sent to its email and mobile.");
        return res;
    }

    @Transactional(noRollbackFor = Verifications.WrongCodeException.class)
    public void reset(Reset r) {
        Check.password(r.newPassword());
        Verifications.Pending p = verifications.consume(r.verificationId(), "reset", r.emailCode(), r.mobileCode(), false);
        jdbc.sql("UPDATE users SET password_hash = ?, password_set_at = NOW(3), must_change = FALSE, failed_count = 0, locked_until = NULL WHERE id = ?")
            .param(encoder.encode(r.newPassword())).param(p.userId()).update();
        tokens.revokeAllForUser(p.userId());
        String col = jdbc.sql("SELECT college_id FROM users WHERE id = ?").param(p.userId()).query(String.class).single();
        audit.log(col, p.userId(), "password.reset", "user", p.userId(), null);
        outbox.email(col, p.email(), "Your Swarsetu password was changed",
            "Your password was just reset and you were signed out everywhere.\nIf this was not you, contact your college office at once.", null);
    }

    // ---------- profile ----------

    @Transactional
    public void changePassword(Me me, ChangePassword c) {
        String hash = jdbc.sql("SELECT password_hash FROM users WHERE id = ?").param(me.id()).query(String.class).single();
        if (!encoder.matches(c.currentPassword(), hash)) throw ApiException.bad("Your current password is not correct.");
        Check.password(c.newPassword());
        if (encoder.matches(c.newPassword(), hash)) throw ApiException.bad("Choose a password different from the current one.");
        jdbc.sql("UPDATE users SET password_hash = ?, password_set_at = NOW(3), must_change = FALSE WHERE id = ?")
            .param(encoder.encode(c.newPassword())).param(me.id()).update();
        tokens.revokeAllForUser(me.id());
        audit.log(me, "password.changed", "user", me.id(), null);
    }

    /** Re-enter password before sensitive actions (payment details, contact change). */
    public void reauth(Me me, String password) {
        String hash = jdbc.sql("SELECT password_hash FROM users WHERE id = ?").param(me.id()).query(String.class).single();
        if (password == null || !encoder.matches(password, hash)) throw ApiException.forbidden("Password is not correct.");
    }

    public Map<String, Object> startContactChange(Me me, ChangeContact c) {
        reauth(me, c.password());
        Map<String, Object> cur = jdbc.sql("SELECT email, mobile, country_code FROM users WHERE id = ?").param(me.id()).query().singleRow();
        boolean gmail = Rows.truthy(jdbc.sql("SELECT require_gmail FROM colleges WHERE id = ?").param(me.collegeId()).query(Boolean.class).single());
        String email = c.email() == null || c.email().isBlank() ? null : Check.email(c.email(), gmail);
        String mobile = Check.mobile(c.mobile(), false);
        if (email != null && email.equals(cur.get("email"))) email = null;
        if (mobile != null && mobile.equals(cur.get("mobile"))) mobile = null;
        if (email == null && mobile == null) throw ApiException.bad("Enter a new email address or mobile number.");
        ensureFree(email, "That email address");
        ensureFree(mobile, "That mobile number");
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("mobile", mobile);
        return verifications.start("change_contact", me.id(), payload, me.collegeId(), email, (String) cur.get("country_code"), mobile, "confirm your new contact details");
    }

    @Transactional(noRollbackFor = Verifications.WrongCodeException.class)
    public Map<String, Object> finishContactChange(Me me, Verify v) {
        Verifications.Pending p = verifications.consume(v.verificationId(), "change_contact", v.emailCode(), v.mobileCode(), true);
        if (!me.id().equals(p.userId())) throw ApiException.forbidden("This code was not issued to you.");
        Map<String, Object> old = jdbc.sql("SELECT email FROM users WHERE id = ?").param(me.id()).query().singleRow();
        if (p.email() != null) jdbc.sql("UPDATE users SET email = ?, email_verified_at = NOW(3) WHERE id = ?").param(p.email()).param(me.id()).update();
        if (p.mobile() != null) jdbc.sql("UPDATE users SET mobile = ?, mobile_verified_at = NOW(3) WHERE id = ?").param(p.mobile()).param(me.id()).update();
        audit.log(me, "contact.changed", "user", me.id(), Map.of("email", p.email() != null, "mobile", p.mobile() != null));
        outbox.email(me.collegeId(), (String) old.get("email"), "Your Swarsetu contact details changed",
            "The email or mobile number on your account was changed. If this was not you, contact your college office at once.", null);
        return userJson(me.id());
    }

    public void updateProfile(Me me, Profile p) {
        jdbc.sql("UPDATE users SET first_name = ?, last_name = ?, alt_contact = ?, city = ?, country = ? WHERE id = ?")
            .param(p.firstName().trim()).param(p.lastName()).param(p.altContact()).param(p.city()).param(p.country()).param(me.id()).update();
        audit.log(me, "profile.updated", "user", me.id(), null);
    }

    public Map<String, Object> userJson(String uid) {
        Map<String, Object> u = Rows.camel(jdbc.sql(
            "SELECT u.id, u.college_id, c.name AS college_name, u.username, u.email, u.mobile, u.country_code, u.first_name, u.last_name, "
            + "u.alt_contact, u.city, u.country, u.role, u.status, u.student_id, u.must_change, u.last_login_at, u.prev_login_at, "
            + "u.email_verified_at IS NOT NULL AS email_verified, u.mobile_verified_at IS NOT NULL AS mobile_verified "
            + "FROM users u JOIN colleges c ON c.id = u.college_id WHERE u.id = ?").param(uid).query().singleRow());
        return u;
    }

    private static String cc(String c) { return c == null || c.isBlank() ? "+91" : c.trim(); }
}
