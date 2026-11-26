package app.swarsetu.fees;

import app.swarsetu.common.*;
import app.swarsetu.inbox.Inbox;
import app.swarsetu.notify.Outbox;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Fee payments (Dues = Total - Paid; status computed by MySQL) and student payment claims verified by staff. */
@RestController
@RequestMapping("/api")
public class FeesController {
    private final JdbcClient jdbc;
    private final CurrentUser current;
    private final Perms perms;
    private final Audit audit;
    private final Inbox inbox;
    private final Outbox outbox;

    public FeesController(JdbcClient jdbc, CurrentUser current, Perms perms, Audit audit, Inbox inbox, Outbox outbox) {
        this.jdbc = jdbc; this.current = current; this.perms = perms; this.audit = audit; this.inbox = inbox; this.outbox = outbox;
    }

    @GetMapping("/payments")
    public Map<String, Object> list(@RequestParam String fromMonth, @RequestParam String toMonth,
                                    @RequestParam(required = false) String studentId, @RequestParam(required = false) String batchId,
                                    @RequestParam(required = false) String status, @RequestParam(required = false) String courseId) {
        Me me = current.get();
        perms.need(me, "viewFees");
        Check.month(fromMonth);
        Check.month(toMonth);
        if (toMonth.compareTo(fromMonth) < 0) throw ApiException.bad("The To month may not be earlier than the From month.");
        StringBuilder sql = new StringBuilder("SELECT p.*, CONCAT(s.first_name, ' ', IFNULL(s.last_name, '')) AS student_name, s.course_id, "
            + "IFNULL(b.name, p.batch_name) AS batch_label FROM payments p JOIN students s ON s.college_id = p.college_id AND s.id = p.student_id "
            + "LEFT JOIN batches b ON b.college_id = p.college_id AND b.id = p.batch_id WHERE p.college_id = ? AND p.fee_month BETWEEN ? AND ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId(), fromMonth, toMonth));
        if (me.isStudent()) {
            if (me.studentId() == null) throw ApiException.forbidden("Your account is not linked to a student record yet.");
            sql.append(" AND p.student_id = ?"); ps.add(me.studentId());
        } else if (studentId != null && !studentId.isBlank()) { sql.append(" AND p.student_id = ?"); ps.add(studentId); }
        if (batchId != null && !batchId.isBlank()) { sql.append(" AND p.batch_id = ?"); ps.add(batchId); }
        if (courseId != null && !courseId.isBlank()) { sql.append(" AND s.course_id = ?"); ps.add(courseId); }
        if (status != null && !status.isBlank()) { sql.append(" AND p.status = ?"); ps.add(status); }
        List<Map<String, Object>> rows = jdbc.sql(sql.append(" ORDER BY p.fee_month, p.student_id").toString()).params(ps).query().listOfRows();
        BigDecimal total = BigDecimal.ZERO, paid = BigDecimal.ZERO, dues = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            total = total.add((BigDecimal) r.get("total_fee"));
            paid = paid.add((BigDecimal) r.get("paid"));
            dues = dues.add((BigDecimal) r.get("dues"));
        }
        return Map.of("rows", Rows.camel(rows), "totals", Map.of("total", total, "paid", paid, "dues", dues, "count", rows.size()));
    }

    public record PaymentIn(@NotBlank String studentId, @NotBlank String batchId, @NotEmpty @Size(max = 24) List<String> months,
                            String paymentDate, @NotNull @DecimalMin("0") BigDecimal totalFee, @NotNull @DecimalMin("0") BigDecimal paid,
                            @Pattern(regexp = "UPI|Cash|Bank Transfer|Card|Cheque") String method, @Size(max = 300) String remarks) {}

    /** One row per month. A payment needs a batch. */
    @PostMapping("/payments")
    @Transactional
    public Map<String, Object> add(@Valid @RequestBody PaymentIn p) {
        Me me = current.get();
        perms.need(me, "editFees");
        String batchName = batchName(me, p.batchId());
        LocalDate date = p.paymentDate() == null || p.paymentDate().isBlank() ? null : Check.date(p.paymentDate(), "Payment date");
        List<String> ids = new ArrayList<>();
        for (String m : new TreeSet<>(p.months())) {
            Check.month(m);
            String id = Crypto.id("pay");
            jdbc.sql("INSERT INTO payments(id, college_id, student_id, batch_id, batch_name, fee_month, payment_date, total_fee, paid, method, remarks, updated_by) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
                .param(id).param(me.collegeId()).param(p.studentId()).param(p.batchId()).param(batchName).param(m).param(date)
                .param(p.totalFee()).param(p.paid()).param(p.method() == null ? "UPI" : p.method()).param(p.remarks()).param(me.id()).update();
            ids.add(id);
        }
        audit.log(me, "payment.added", "payment", String.join(",", ids), Map.of("student", p.studentId(), "months", p.months()));
        return Map.of("ids", ids);
    }

    @PutMapping("/payments/{id}")
    @Transactional
    public Map<String, Object> edit(@PathVariable String id, @Valid @RequestBody PaymentIn p) {
        Me me = current.get();
        perms.need(me, "editFees");
        if (p.months().size() != 1) throw ApiException.bad("Edit one month at a time.");
        String batchName = batchName(me, p.batchId());
        int n = jdbc.sql("UPDATE payments SET student_id = ?, batch_id = ?, batch_name = ?, fee_month = ?, payment_date = ?, total_fee = ?, paid = ?, "
                + "method = ?, remarks = ?, updated_by = ?, version = version + 1 WHERE college_id = ? AND id = ?")
            .param(p.studentId()).param(p.batchId()).param(batchName).param(Check.month(p.months().get(0)))
            .param(p.paymentDate() == null || p.paymentDate().isBlank() ? null : Check.date(p.paymentDate(), "Payment date"))
            .param(p.totalFee()).param(p.paid()).param(p.method() == null ? "UPI" : p.method()).param(p.remarks()).param(me.id())
            .param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Payment not found.");
        audit.log(me, "payment.edited", "payment", id, null);
        return Map.of("id", id);
    }

    @DeleteMapping("/payments/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        Me me = current.get();
        perms.need(me, "deleteRecords");
        int n = jdbc.sql("DELETE FROM payments WHERE college_id = ? AND id = ?").param(me.collegeId()).param(id).update();
        if (n == 0) throw ApiException.notFound("Payment not found.");
        audit.log(me, "payment.deleted", "payment", id, null);
        return Map.of("ok", true);
    }

    private String batchName(Me me, String batchId) {
        return jdbc.sql("SELECT name FROM batches WHERE college_id = ? AND id = ?").param(me.collegeId()).param(batchId)
            .query(String.class).optional().orElseThrow(() -> ApiException.bad("Choose a batch for this payment."));
    }

    // ---------------- claims: "I have paid" ----------------

    public record ClaimIn(@NotBlank String month, @NotNull @DecimalMin("1") @DecimalMax("200000") BigDecimal amount,
                          @NotBlank @Pattern(regexp = "UPI|Bank transfer") String method, @NotBlank @Size(max = 22) String reference,
                          @NotBlank String paidOn) {}

    @PostMapping("/claims")
    @Transactional
    public Map<String, Object> claim(@Valid @RequestBody ClaimIn c) {
        Me me = current.get();
        if (!me.isStudent() || me.studentId() == null) throw ApiException.forbidden("Only a student linked to a student record can report a payment.");
        Check.month(c.month());
        LocalDate paid = Check.date(c.paidOn(), "Paid on");
        if (paid.isAfter(LocalDate.now())) throw ApiException.bad("The payment date cannot be in the future.");
        if (paid.isBefore(LocalDate.now().minusDays(90))) throw ApiException.bad("Report payments within 90 days. Contact the office for older ones.");
        String ref = c.reference().trim().toUpperCase().replaceAll("\\s", "");
        String id = Crypto.id("clm");
        jdbc.sql("INSERT INTO payment_claims(id, college_id, student_id, fee_month, amount, method, reference, paid_on, submitted_by) VALUES (?,?,?,?,?,?,?,?,?)")
            .param(id).param(me.collegeId()).param(me.studentId()).param(c.month()).param(c.amount()).param(c.method()).param(ref).param(paid).param(me.id()).update();
        inbox.toAudience(me.collegeId(), "editFees", "payment", "Payment reported by " + me.name(),
            "₹" + c.amount() + " for " + c.month() + " by " + c.method() + ", reference " + ref + ". Check your bank statement, then verify or reject.", "fees", "clm:" + id);
        audit.log(me, "claim.submitted", "claim", id, Map.of("amount", c.amount(), "ref", ref));
        return Map.of("id", id, "status", "pending");
    }

    @GetMapping("/claims")
    public List<Map<String, Object>> claims(@RequestParam(required = false) String status) {
        Me me = current.get();
        StringBuilder sql = new StringBuilder("SELECT c.*, CONCAT(s.first_name, ' ', IFNULL(s.last_name, '')) AS student_name FROM payment_claims c "
            + "JOIN students s ON s.college_id = c.college_id AND s.id = c.student_id WHERE c.college_id = ?");
        List<Object> ps = new ArrayList<>(List.of(me.collegeId()));
        if (me.isStudent()) { sql.append(" AND c.student_id = ?"); ps.add(me.studentId() == null ? "-" : me.studentId()); }
        else perms.need(me, "editFees");
        if (status != null && !status.isBlank()) { sql.append(" AND c.status = ?"); ps.add(status); }
        return Rows.camel(jdbc.sql(sql.append(" ORDER BY c.submitted_at DESC LIMIT 500").toString()).params(ps).query().listOfRows());
    }

    public record Review(@Size(max = 300) String note) {}

    /** Staff confirmed the money arrived: adds it to the month's payment (creating it if needed). */
    @PostMapping("/claims/{id}/verify")
    @Transactional
    public Map<String, Object> verify(@PathVariable String id, @RequestBody(required = false) Review r) {
        Me me = current.get();
        perms.need(me, "editFees");
        Map<String, Object> c = jdbc.sql("SELECT * FROM payment_claims WHERE college_id = ? AND id = ? FOR UPDATE").param(me.collegeId()).param(id)
            .query().listOfRows().stream().findFirst().orElseThrow(() -> ApiException.notFound("Claim not found."));
        if (!"pending".equals(c.get("status"))) throw ApiException.conflict("This claim was already " + c.get("status") + ".");
        if (me.id().equals(c.get("submitted_by"))) throw ApiException.forbidden("You cannot verify your own payment.");
        String sid = (String) c.get("student_id"), month = (String) c.get("fee_month");
        BigDecimal amount = (BigDecimal) c.get("amount");
        Map<String, Object> pay = jdbc.sql("SELECT id, total_fee, paid FROM payments WHERE college_id = ? AND student_id = ? AND fee_month = ? ORDER BY created_at LIMIT 1 FOR UPDATE")
            .param(me.collegeId()).param(sid).param(month).query().listOfRows().stream().findFirst().orElse(null);
        String payId;
        if (pay != null) {
            BigDecimal due = ((BigDecimal) pay.get("total_fee")).subtract((BigDecimal) pay.get("paid"));
            if (amount.compareTo(due) > 0) throw ApiException.conflict("The amount (₹" + amount + ") is more than the dues for " + month + " (₹" + due + "). Correct the fee first.");
            payId = (String) pay.get("id");
            jdbc.sql("UPDATE payments SET paid = paid + ?, payment_date = ?, method = ?, updated_by = ?, remarks = CONCAT(IFNULL(remarks, ''), ?), version = version + 1 "
                    + "WHERE college_id = ? AND id = ?")
                .param(amount).param(c.get("paid_on")).param("UPI".equals(c.get("method")) ? "UPI" : "Bank Transfer").param(me.id())
                .param(" Ref " + c.get("reference") + ".").param(me.collegeId()).param(payId).update();
        } else {
            Map<String, Object> s = jdbc.sql("SELECT s.batch_id, s.monthly_fee, b.name FROM students s LEFT JOIN batches b ON b.college_id = s.college_id AND b.id = s.batch_id "
                    + "WHERE s.college_id = ? AND s.id = ?").param(me.collegeId()).param(sid).query().singleRow();
            if (s.get("batch_id") == null) throw ApiException.conflict("The student has no batch. Assign a batch, then verify.");
            BigDecimal fee = s.get("monthly_fee") == null ? amount : (BigDecimal) s.get("monthly_fee");
            if (amount.compareTo(fee) > 0) throw ApiException.conflict("The amount is more than the monthly fee (₹" + fee + "). Add the payment manually.");
            payId = Crypto.id("pay");
            jdbc.sql("INSERT INTO payments(id, college_id, student_id, batch_id, batch_name, fee_month, payment_date, total_fee, paid, method, remarks, updated_by) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
                .param(payId).param(me.collegeId()).param(sid).param(s.get("batch_id")).param(s.get("name")).param(month).param(c.get("paid_on"))
                .param(fee).param(amount).param("UPI".equals(c.get("method")) ? "UPI" : "Bank Transfer").param("Ref " + c.get("reference") + ".").param(me.id()).update();
        }
        jdbc.sql("UPDATE payment_claims SET status = 'verified', reviewed_by = ?, reviewed_at = NOW(3), review_note = ?, payment_id = ? WHERE college_id = ? AND id = ?")
            .param(me.id()).param(r == null ? null : r.note()).param(payId).param(me.collegeId()).param(id).update();
        inbox.toUser(me.collegeId(), (String) c.get("submitted_by"), "payment", "Payment verified",
            "Your payment of ₹" + amount + " for " + month + " (ref " + c.get("reference") + ") was received.", "fees", "clmok:" + id);
        audit.log(me, "claim.verified", "claim", id, Map.of("payment", payId));
        return Map.of("ok", true, "paymentId", payId);
    }

    @PostMapping("/claims/{id}/reject")
    @Transactional
    public Map<String, Object> reject(@PathVariable String id, @Valid @RequestBody Review r) {
        Me me = current.get();
        perms.need(me, "editFees");
        if (r.note() == null || r.note().isBlank()) throw ApiException.bad("Give a reason so the student knows what to do.");
        Map<String, Object> c = jdbc.sql("SELECT submitted_by, fee_month, reference, status FROM payment_claims WHERE college_id = ? AND id = ? FOR UPDATE")
            .param(me.collegeId()).param(id).query().listOfRows().stream().findFirst().orElseThrow(() -> ApiException.notFound("Claim not found."));
        if (!"pending".equals(c.get("status"))) throw ApiException.conflict("This claim was already " + c.get("status") + ".");
        jdbc.sql("UPDATE payment_claims SET status = 'rejected', reviewed_by = ?, reviewed_at = NOW(3), review_note = ? WHERE college_id = ? AND id = ?")
            .param(me.id()).param(r.note()).param(me.collegeId()).param(id).update();
        inbox.toUser(me.collegeId(), (String) c.get("submitted_by"), "payment", "Payment not confirmed",
            "Your payment report for " + c.get("fee_month") + " (ref " + c.get("reference") + ") was not confirmed: " + r.note(), "fees", "clmno:" + id);
        audit.log(me, "claim.rejected", "claim", id, Map.of("note", r.note()));
        return Map.of("ok", true);
    }
}
