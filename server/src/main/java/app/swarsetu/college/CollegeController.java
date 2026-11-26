package app.swarsetu.college;

import app.swarsetu.academic.Batches;
import app.swarsetu.auth.AuthService;
import app.swarsetu.common.*;
import app.swarsetu.inbox.Inbox;
import app.swarsetu.notify.Outbox;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** College profile, roles & permissions, user management, payment details, data health, dashboard and audit. */
@RestController
@RequestMapping("/api")
public class CollegeController {
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;
    private final Audit audit;
    private final AuthService auth;
    private final Inbox inbox;
    private final Outbox outbox;
    private final Batches batches;
    private final PasswordEncoder encoder;

    public CollegeController(JdbcClient jdbc, CurrentUser current, Perms perms, Audit audit, AuthService auth, Inbox inbox,
                             Outbox outbox, Batches batches, PasswordEncoder encoder) {
        this.jdbc = jdbc; this.current = current; this.perms = perms; this.audit = audit; this.auth = auth;
        this.inbox = inbox; this.outbox = outbox; this.batches = batches; this.encoder = encoder;
    }

    // ---------------- profile ----------------

    @GetMapping("/college")
    public Map<String, Object> college() {
        Me me = current.get();
        return Rows.camel(jdbc.sql("SELECT * FROM colleges WHERE id = ?").param(me.collegeId()).query().singleRow());
    }

    public record CollegeIn(@NotBlank @Size(max = 150) String name, @Size(max = 200) String affiliation, @Size(max = 80) String city,
                            @Size(max = 80) String country, @Size(max = 300) String address, @Size(max = 30) String phone,
                            @Size(max = 120) String email, @Min(1) @Max(50) Integer groupSize, Boolean approveSignups, Boolean requireGmail,
                            @Min(0) @Max(30) Integer reminderDays, Boolean reminderExternal, @Size(max = 300) String driveLink) {}

    @PutMapping("/college")
    public Map<String, Object> saveCollege(@Valid @RequestBody CollegeIn c) {
        Me me = current.get();
        perms.need(me, "editCollege");
        Check.https(c.driveLink(), "Drive link");
        String key = AuthService.nameKey(c.name());
        if (key.length() < 3) throw ApiException.bad("Enter the full college name.");
        jdbc.sql("UPDATE colleges SET name = ?, name_key = ?, affiliation = ?, city = ?, country = ?, address = ?, phone = ?, email = ?, "
                + "group_size = IFNULL(?, group_size), approve_signups = IFNULL(?, approve_signups), require_gmail = IFNULL(?, require_gmail), "
                + "reminder_days = IFNULL(?, reminder_days), reminder_external = IFNULL(?, reminder_external), drive_link = ?, version = version + 1 WHERE id = ?")
            .param(c.name().trim()).param(key).param(c.affiliation()).param(c.city()).param(c.country()).param(c.address()).param(c.phone())
            .param(c.email() == null || c.email().isBlank() ? null : Check.email(c.email(), false))
            .param(c.groupSize()).param(c.approveSignups()).param(c.requireGmail()).param(c.reminderDays()).param(c.reminderExternal())
            .param(c.driveLink()).param(me.collegeId()).update();
        audit.log(me, "college.edited", "college", me.collegeId(), null);
        return college();
    }

    // ---------------- roles & permissions ----------------

    @GetMapping("/permissions")
    public Map<String, Object> permissions() {
        Me me = current.get();
        return Map.of("all", Perms.ALL, "studentAllowed", Perms.STUDENT_OK, "matrix", perms.matrix(me.collegeId()));
    }

    @PutMapping("/permissions")
    @Transactional
    public Map<String, Object> savePermissions(@RequestBody Map<String, Map<String, Boolean>> matrix) {
        Me me = current.get();
        perms.need(me, "manageUsers");
        jdbc.sql("DELETE FROM role_permissions WHERE college_id = ?").param(me.collegeId()).update();
        for (String role : List.of("faculty", "student")) {
            Map<String, Boolean> m = matrix.getOrDefault(role, Map.of());
            for (String p : Perms.ALL) {
                if (!m.containsKey(p)) continue;
                boolean v = Boolean.TRUE.equals(m.get(p)) && (!"student".equals(role) || Perms.STUDENT_OK.contains(p));
                if (v != Perms.byDefault(role, p)) {
                    jdbc.sql("INSERT INTO role_permissions(college_id, role, permission, allowed) VALUES (?,?,?,?)")
                        .param(me.collegeId()).param(role).param(p).param(v).update();
                }
            }
        }
        audit.log(me, "permissions.saved", "college", me.collegeId(), null);
        return permissions();
    }

    // ---------------- users ----------------

    @GetMapping("/users")
    public List<Map<String, Object>> users(@RequestParam(required = false) String status, @RequestParam(required = false) String role) {
        Me me = current.get();
        perms.need(me, "manageUsers");
        StringBuilder sql = new StringBuilder("SELECT id, username, email, mobile, first_name, last_name, city, country, role, status, student_id, "
            + "last_login_at, created_at, reject_reason, email_verified_at IS NOT NULL AS email_verified, mobile_verified_at IS NOT NULL AS mobile_verified "
            + "FROM users WHERE college_id = ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId()));
        if (status != null && !status.isBlank()) { sql.append(" AND status = ?"); ps.add(status); }
        if (role != null && !role.isBlank()) { sql.append(" AND role = ?"); ps.add(role); }
        return Rows.camel(jdbc.sql(sql.append(" ORDER BY status = 'pending' DESC, first_name").toString()).params(ps).query().listOfRows());
    }

    public record UserEdit(@Pattern(regexp = "admin|faculty|student") String role, @Pattern(regexp = "active|inactive") String status, String studentId) {}

    @PutMapping("/users/{id}")
    @Transactional
    public Map<String, Object> editUser(@PathVariable String id, @Valid @RequestBody UserEdit e) {
        Me me = current.get();
        perms.need(me, "manageUsers");
        Map<String, Object> u = jdbc.sql("SELECT role, status FROM users WHERE college_id = ? AND id = ? FOR UPDATE").param(me.collegeId()).param(id)
            .query().listOfRows().stream().findFirst().orElseThrow(() -> ApiException.notFound("User not found."));
        String role = e.role() == null ? (String) u.get("role") : e.role();
        String status = e.status() == null ? (String) u.get("status") : e.status();
        if ("pending".equals(u.get("status")) || "rejected".equals(u.get("status"))) throw ApiException.bad("Approve or reject this join request first.");
        if (("admin".equals(role) || "admin".equals(u.get("role"))) && !me.isAdmin()) throw ApiException.forbidden("Only an Admin can change Admin accounts.");
        if ("admin".equals(u.get("role")) && (!"admin".equals(role) || !"active".equals(status))) keepOneAdmin(me.collegeId(), id);
        String student = linkStudent(me, role, e.studentId(), id);
        jdbc.sql("UPDATE users SET role = ?, status = ?, student_id = ?, version = version + 1 WHERE college_id = ? AND id = ?")
            .param(role).param(status).param(student).param(me.collegeId()).param(id).update();
        if (!"active".equals(status)) jdbc.sql("UPDATE refresh_tokens SET revoked_at = NOW(3) WHERE user_id = ? AND revoked_at IS NULL").param(id).update();
        audit.log(me, "user.edited", "user", id, Map.of("role", role, "status", status));
        return Map.of("id", id);
    }

    public record Approve(@NotBlank @Pattern(regexp = "admin|faculty|student") String role, String studentId) {}

    @PostMapping("/users/{id}/approve")
    @Transactional
    public Map<String, Object> approve(@PathVariable String id, @Valid @RequestBody Approve a) {
        Me me = current.get();
        perms.need(me, "manageUsers");
        if ("admin".equals(a.role()) && !me.isAdmin()) throw ApiException.forbidden("Only an Admin can make another Admin.");
        Map<String, Object> u = jdbc.sql("SELECT email, first_name, status FROM users WHERE college_id = ? AND id = ? FOR UPDATE")
            .param(me.collegeId()).param(id).query().listOfRows().stream().findFirst().orElseThrow(() -> ApiException.notFound("User not found."));
        if (!"pending".equals(u.get("status"))) throw ApiException.conflict("This request was already handled.");
        String student = linkStudent(me, a.role(), a.studentId(), id);
        jdbc.sql("UPDATE users SET role = ?, status = 'active', student_id = ?, approved_by = ?, approved_at = NOW(3) WHERE college_id = ? AND id = ?")
            .param(a.role()).param(student).param(me.id()).param(me.collegeId()).param(id).update();
        String collegeName = jdbc.sql("SELECT name FROM colleges WHERE id = ?").param(me.collegeId()).query(String.class).single();
        outbox.email(me.collegeId(), (String) u.get("email"), "You can now sign in to " + collegeName,
            "Hello " + u.get("first_name") + ",\n\nYour request to join " + collegeName + " was approved. You can sign in now.", "approved:" + id);
        audit.log(me, "user.approved", "user", id, Map.of("role", a.role()));
        return Map.of("ok", true);
    }

    public record Reject(@Size(max = 300) String reason) {}

    @PostMapping("/users/{id}/reject")
    public Map<String, Object> reject(@PathVariable String id, @Valid @RequestBody Reject r) {
        Me me = current.get();
        perms.need(me, "manageUsers");
        int n = jdbc.sql("UPDATE users SET status = 'rejected', reject_reason = ?, approved_by = ?, approved_at = NOW(3) WHERE college_id = ? AND id = ? AND status = 'pending'")
            .param(r.reason()).param(me.id()).param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.conflict("This request was already handled.");
        audit.log(me, "user.rejected", "user", id, Map.of("reason", String.valueOf(r.reason())));
        return Map.of("ok", true);
    }

    public record NewUser(@NotBlank @Pattern(regexp = "admin|faculty|student") String role, @NotBlank @Size(max = 60) String firstName,
                          @Size(max = 60) String lastName, @NotBlank @Size(max = 120) String email, @Size(max = 15) String mobile,
                          @Size(max = 60) String username, String studentId) {}

    /** Staff adds a user directly; a temporary password is emailed and must be changed at first sign-in. */
    @PostMapping("/users")
    @Transactional
    public Map<String, Object> addUser(@Valid @RequestBody NewUser n) {
        Me me = current.get();
        perms.need(me, "manageUsers");
        if ("admin".equals(n.role()) && !me.isAdmin()) throw ApiException.forbidden("Only an Admin can add another Admin.");
        String email = Check.email(n.email(), false);
        String mobile = Check.mobile(n.mobile(), false);
        String username = n.username() == null || n.username().isBlank() ? email : n.username().trim();
        for (String v : new String[] {username, email, mobile}) {
            if (v == null) continue;
            int c = jdbc.sql("SELECT COUNT(*) FROM users WHERE username_lc = LOWER(?) OR email = LOWER(?) OR mobile = ?").param(v).param(v).param(v).query(Integer.class).single();
            if (c > 0) throw ApiException.conflict(v + " is already registered.");
        }
        String id = Crypto.id("usr");
        String temp = Crypto.chars(4) + "-" + Crypto.chars(4) + "-" + (10 + new java.security.SecureRandom().nextInt(89));
        String student = linkStudent(me, n.role(), n.studentId(), id);
        jdbc.sql("INSERT INTO users(id, college_id, username, email, mobile, first_name, last_name, role, status, password_hash, must_change, student_id, approved_by, approved_at) "
                + "VALUES (?,?,?,?,?,?,?,?, 'active', ?, TRUE, ?, ?, NOW(3))")
            .param(id).param(me.collegeId()).param(username).param(email).param(mobile).param(n.firstName().trim()).param(n.lastName())
            .param(n.role()).param(encoder.encode(temp)).param(student).param(me.id()).update();
        String collegeName = jdbc.sql("SELECT name FROM colleges WHERE id = ?").param(me.collegeId()).query(String.class).single();
        outbox.email(me.collegeId(), email, "Your " + collegeName + " account",
            "Hello " + n.firstName() + ",\n\nAn account was created for you.\nSign-in ID: " + username + "\nTemporary password: " + temp
            + "\n\nYou will be asked to choose a new password when you first sign in.", "newuser:" + id);
        audit.log(me, "user.added", "user", id, Map.of("role", n.role()));
        return Map.of("id", id, "message", "The temporary password was emailed to " + Check.mask(email) + ".");
    }

    private String linkStudent(Me me, String role, String studentId, String userId) {
        if (!"student".equals(role) || studentId == null || studentId.isBlank()) return null;
        int ok = jdbc.sql("SELECT COUNT(*) FROM students WHERE college_id = ? AND id = ?").param(me.collegeId()).param(studentId).query(Integer.class).single();
        if (ok == 0) throw ApiException.bad("Student " + studentId + " was not found in your college.");
        int taken = jdbc.sql("SELECT COUNT(*) FROM users WHERE college_id = ? AND student_id = ? AND id <> ?").param(me.collegeId()).param(studentId).param(userId)
            .query(Integer.class).single();
        if (taken > 0) throw ApiException.conflict("Student " + studentId + " is already linked to another account.");
        return studentId;
    }

    private void keepOneAdmin(String collegeId, String exceptUserId) {
        List<String> others = jdbc.sql("SELECT id FROM users WHERE college_id = ? AND role = 'admin' AND status = 'active' AND id <> ? FOR UPDATE")
            .param(collegeId).param(exceptUserId).query(String.class).list();
        if (others.isEmpty()) throw ApiException.conflict("Keep at least one active Admin. Make someone else Admin first.");
    }

    // ---------------- payment details (Admin only to change; every member can read to pay) ----------------

    @GetMapping("/payment-details")
    public Map<String, Object> paymentDetails() {
        Me me = current.get();
        return jdbc.sql("SELECT college_id, payee, upi_id, upi_mobile, apps, bank, branch, account_no, ifsc, qr_mode, qr_vpa, changed_at "
                + "FROM college_payment_details WHERE college_id = ?").param(me.collegeId()).query().listOfRows()
            .stream().findFirst().map(Rows::camel).orElse(Map.of());
    }

    public record PayIn(@NotBlank @Size(max = 80) String payee, @Size(max = 80) String upiId, @Size(max = 15) String upiMobile,
                        @Size(max = 100) String apps, @Size(max = 80) String bank, @Size(max = 80) String branch,
                        @Size(max = 18) String accountNo, @Size(max = 11) String ifsc, @NotBlank String password) {}

    @PutMapping("/payment-details")
    @Transactional
    public Map<String, Object> savePaymentDetails(@Valid @RequestBody PayIn p) {
        Me me = current.get();
        perms.needAdmin(me);
        auth.reauth(me, p.password());
        Map<String, Object> old = jdbc.sql("SELECT upi_id, account_no, qr_mode, qr_vpa FROM college_payment_details WHERE college_id = ? FOR UPDATE")
            .param(me.collegeId()).query().listOfRows().stream().findFirst().orElse(null);
        String upi = blank(p.upiId()) ? null : p.upiId().trim().toLowerCase();
        String acct = blank(p.accountNo()) ? null : p.accountNo().replaceAll("\\s", "");
        String ifsc = blank(p.ifsc()) ? null : p.ifsc().trim().toUpperCase();
        // An uploaded scanner stays only while it still pays the same UPI ID.
        boolean keepUpload = old != null && "upload".equals(old.get("qr_mode")) && upi != null && upi.equalsIgnoreCase((String) old.get("qr_vpa"));
        jdbc.sql("INSERT INTO college_payment_details(college_id, payee, upi_id, upi_mobile, apps, bank, branch, account_no, ifsc, qr_mode, changed_by) "
                + "VALUES (?,?,?,?,?,?,?,?,?, 'gen', ?) AS n ON DUPLICATE KEY UPDATE payee = n.payee, upi_id = n.upi_id, upi_mobile = n.upi_mobile, "
                + "apps = n.apps, bank = n.bank, branch = n.branch, account_no = n.account_no, ifsc = n.ifsc, changed_by = n.changed_by, changed_at = NOW(3)"
                + (keepUpload ? "" : ", qr_mode = 'gen', qr_image = NULL, qr_vpa = NULL"))
            .param(me.collegeId()).param(p.payee().trim()).param(upi).param(Check.mobile(p.upiMobile(), false)).param(p.apps())
            .param(p.bank()).param(p.branch()).param(acct).param(ifsc).param(me.id()).update();
        List<String> changed = new ArrayList<>();
        if (old == null || !Objects.equals(old.get("upi_id"), upi)) changed.add("UPI ID");
        if (old == null || !Objects.equals(old.get("account_no"), acct)) changed.add("bank account");
        String last4 = old == null || old.get("account_no") == null ? null : ((String) old.get("account_no")).substring(((String) old.get("account_no")).length() - 4);
        jdbc.sql("INSERT INTO payment_detail_audit(college_id, changed_by, fields, old_upi, old_account_last4) VALUES (?,?,?,?,?)")
            .param(me.collegeId()).param(me.id()).param(changed.isEmpty() ? "other details" : String.join(", ", changed))
            .param(old == null ? null : old.get("upi_id")).param(last4).update();
        if (!changed.isEmpty()) alertAdmins(me, "Payment details changed: " + String.join(", ", changed));
        audit.log(me, "paydetails.saved", "college", me.collegeId(), Map.of("changed", changed));
        return paymentDetails();
    }

    /** Upload the college's own UPI scanner. It is decoded on the server and must pay the saved UPI ID. */
    @PostMapping(path = "/payment-details/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> uploadQr(@RequestParam("file") MultipartFile file, @RequestParam("password") String password) throws IOException {
        Me me = current.get();
        perms.needAdmin(me);
        auth.reauth(me, password);
        if (file.getSize() > 2_000_000) throw ApiException.bad("The image is too large (2 MB at most).");
        String upi = jdbc.sql("SELECT upi_id FROM college_payment_details WHERE college_id = ?").param(me.collegeId()).query(String.class).optional().orElse(null);
        if (upi == null) throw ApiException.bad("Save the college UPI ID first, then upload its scanner.");
        String pays = QrReader.upiId(file.getBytes());
        if (!pays.equalsIgnoreCase(upi)) {
            throw ApiException.bad("This scanner pays " + pays + ", not the college UPI ID " + upi + ". Upload the college's own scanner.");
        }
        jdbc.sql("UPDATE college_payment_details SET qr_mode = 'upload', qr_image = ?, qr_vpa = ?, changed_by = ?, changed_at = NOW(3) WHERE college_id = ?")
            .param(file.getBytes()).param(pays).param(me.id()).param(me.collegeId()).update();
        jdbc.sql("INSERT INTO payment_detail_audit(college_id, changed_by, fields) VALUES (?,?, 'uploaded scanner')").param(me.collegeId()).param(me.id()).update();
        alertAdmins(me, "A new payment scanner was uploaded");
        return Map.of("ok", true, "upiId", pays);
    }

    @DeleteMapping("/payment-details/qr")
    public Map<String, Object> removeQr(@RequestParam String password) {
        Me me = current.get();
        perms.needAdmin(me);
        auth.reauth(me, password);
        jdbc.sql("UPDATE college_payment_details SET qr_mode = 'gen', qr_image = NULL, qr_vpa = NULL WHERE college_id = ?").param(me.collegeId()).update();
        return Map.of("ok", true);
    }

    @GetMapping("/payment-details/qr")
    public ResponseEntity<byte[]> qrImage() {
        Me me = current.get();
        byte[] img = jdbc.sql("SELECT qr_image FROM college_payment_details WHERE college_id = ? AND qr_mode = 'upload'").param(me.collegeId())
            .query(byte[].class).optional().orElseThrow(() -> ApiException.notFound("No uploaded scanner."));
        MediaType type = img.length > 3 && (img[0] & 0xFF) == 0xFF && (img[1] & 0xFF) == 0xD8 ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noStore()).body(img);
    }

    @GetMapping("/payment-details/audit")
    public List<Map<String, Object>> paymentAudit() {
        Me me = current.get();
        perms.needAdmin(me);
        return Rows.camel(jdbc.sql("SELECT a.at, a.fields, a.old_upi, a.old_account_last4, CONCAT(u.first_name, ' ', IFNULL(u.last_name, '')) AS changed_by "
            + "FROM payment_detail_audit a LEFT JOIN users u ON u.id = a.changed_by WHERE a.college_id = ? ORDER BY a.at DESC LIMIT 100")
            .param(me.collegeId()).query().listOfRows());
    }

    private void alertAdmins(Me me, String what) {
        String body = what + " by " + me.name() + ". If this was not expected, check College Profile > Payment details now.";
        inbox.toAudience(me.collegeId(), "admin", "security", what, body, "college", null);
        jdbc.sql("SELECT email FROM users WHERE college_id = ? AND role = 'admin' AND status = 'active'").param(me.collegeId()).query(String.class).list()
            .forEach(e -> outbox.email(me.collegeId(), e, "Security alert: " + what, body, null));
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    // ---------------- data health ----------------

    @GetMapping("/data-health")
    public Map<String, Object> dataHealth() {
        Me me = current.get();
        perms.need(me, "editCollege");
        String c = me.collegeId();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("note", "Links between records are enforced by the database, so broken links cannot exist. These are the remaining checks.");
        r.put("emptyBatches", jdbc.sql("SELECT COUNT(*) FROM batches b WHERE b.college_id = ? AND NOT EXISTS "
            + "(SELECT 1 FROM students s WHERE s.college_id = b.college_id AND s.batch_id = b.id)").param(c).query(Integer.class).single());
        r.put("studentsWithoutBatch", jdbc.sql("SELECT COUNT(*) FROM students WHERE college_id = ? AND batch_id IS NULL").param(c).query(Integer.class).single());
        r.put("overfullOnlineBatches", jdbc.sql("SELECT COUNT(*) FROM (SELECT b.id FROM batches b JOIN students s ON s.college_id = b.college_id AND s.batch_id = b.id "
            + "AND s.status = 'Active' WHERE b.college_id = ? AND b.class_type = 'Group' AND b.mode = 'Online' GROUP BY b.id "
            + "HAVING COUNT(*) > (SELECT group_size FROM colleges WHERE id = ?)) x").param(c).param(c).query(Integer.class).single());
        r.put("studentAccountsNotLinked", jdbc.sql("SELECT COUNT(*) FROM users WHERE college_id = ? AND role = 'student' AND status = 'active' AND student_id IS NULL")
            .param(c).query(Integer.class).single());
        r.put("claimsWaitingOver7Days", jdbc.sql("SELECT COUNT(*) FROM payment_claims WHERE college_id = ? AND status = 'pending' AND submitted_at < NOW(3) - INTERVAL 7 DAY")
            .param(c).query(Integer.class).single());
        return r;
    }

    @PostMapping("/data-health/fix")
    @Transactional
    public Map<String, Object> fix() {
        Me me = current.get();
        perms.need(me, "editCollege");
        int removed = batches.removeEmpty(me.collegeId());
        audit.log(me, "datahealth.fixed", "college", me.collegeId(), Map.of("emptyBatchesRemoved", removed));
        return Map.of("emptyBatchesRemoved", removed);
    }

    // ---------------- dashboard and audit ----------------

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Me me = current.get();
        perms.need(me, "viewDashboard");
        String c = me.collegeId();
        String month = LocalDate.now().toString().substring(0, 7);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("activeStudents", jdbc.sql("SELECT COUNT(*) FROM students WHERE college_id = ? AND status = 'Active'").param(c).query(Integer.class).single());
        r.put("courses", jdbc.sql("SELECT COUNT(*) FROM courses WHERE college_id = ? AND status = 'Active'").param(c).query(Integer.class).single());
        r.put("batches", jdbc.sql("SELECT COUNT(*) FROM batches WHERE college_id = ?").param(c).query(Integer.class).single());
        r.put("duesThisMonth", jdbc.sql("SELECT IFNULL(SUM(dues), 0) FROM payments WHERE college_id = ? AND fee_month = ?").param(c).param(month)
            .query(java.math.BigDecimal.class).single());
        r.put("pendingClaims", jdbc.sql("SELECT COUNT(*) FROM payment_claims WHERE college_id = ? AND status = 'pending'").param(c).query(Integer.class).single());
        r.put("pendingUsers", jdbc.sql("SELECT COUNT(*) FROM users WHERE college_id = ? AND status = 'pending'").param(c).query(Integer.class).single());
        r.put("absenceNoticesToday", jdbc.sql("SELECT COUNT(*) FROM absence_notices WHERE college_id = ? AND class_date = CURDATE() AND status IN ('sent','seen')")
            .param(c).query(Integer.class).single());
        return r;
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> auditLog(@RequestParam(defaultValue = "200") int limit) {
        Me me = current.get();
        perms.needAdmin(me);
        return Rows.camel(jdbc.sql("SELECT a.at, a.action, a.entity, a.entity_id, a.detail, CONCAT(u.first_name, ' ', IFNULL(u.last_name, '')) AS by_user "
            + "FROM audit_log a LEFT JOIN users u ON u.id = a.user_id WHERE a.college_id = ? ORDER BY a.at DESC LIMIT ?")
            .param(me.collegeId()).param(Math.min(Math.max(limit, 1), 1000)).query().listOfRows());
    }
}
