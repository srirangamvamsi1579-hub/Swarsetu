package app.swarsetu.auth;

import app.swarsetu.common.ApiException;
import app.swarsetu.common.Crypto;
import app.swarsetu.config.AppProps;
import app.swarsetu.notify.EmailSender;
import app.swarsetu.notify.Outbox;
import app.swarsetu.notify.SmsSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * One-time codes sent by email and SMS. Codes are stored only as HMAC hashes, expire after
 * swarsetu.code-minutes, allow 5 attempts, and are limited to 5 requests per address per hour.
 */
@Service
public class Verifications {
    public static final int MAX_ATTEMPTS = 5;
    private final JdbcClient jdbc;
    private final Outbox outbox;
    private final AppProps props;
    private final ObjectMapper json;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public Verifications(JdbcClient jdbc, Outbox outbox, AppProps props, ObjectMapper json, EmailSender emailSender, SmsSender smsSender) {
        this.jdbc = jdbc; this.outbox = outbox; this.props = props; this.json = json;
        this.emailSender = emailSender; this.smsSender = smsSender;
    }

    public record Pending(String id, String purpose, String userId, Map<String, Object> payload, String email, String mobile) {}

    /** Creates a verification and queues the codes. Returns details for the client (never the codes). */
    public Map<String, Object> start(String purpose, String userId, Map<String, Object> payload, String collegeId,
                                     String email, String countryCode, String mobile, String what) {
        Integer recent = jdbc.sql("SELECT COUNT(*) FROM verifications WHERE (email = ? OR (mobile IS NOT NULL AND mobile = ?)) "
                + "AND created_at > NOW(3) - INTERVAL 1 HOUR")
            .param(email).param(mobile).query(Integer.class).single();
        if (recent >= 5) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many codes requested for this address. Try again in an hour.");
        String id = UUID.randomUUID().toString();
        String ec = email == null ? null : Crypto.code6();
        String mc = mobile == null ? null : Crypto.code6();
        String pj;
        try { pj = payload == null ? null : json.writeValueAsString(payload); } catch (Exception e) { throw new IllegalStateException(e); }
        jdbc.sql("INSERT INTO verifications(id, purpose, user_id, payload, email, mobile, email_code_hash, mobile_code_hash, expires_at) "
                + "VALUES (?,?,?,?,?,?,?,?, NOW(3) + INTERVAL ? MINUTE)")
            .param(id).param(purpose).param(userId).param(pj).param(email).param(mobile)
            .param(ec == null ? null : hash(id, ec)).param(mc == null ? null : hash(id, mc)).param(props.codeMinutes()).update();
        String text = "Your Swarsetu code to " + what + " is %s. It expires in " + props.codeMinutes()
            + " minutes. Do not share it with anyone — college staff will never ask for it.";
        if (ec != null) outbox.email(collegeId, email, "Swarsetu code: " + ec, String.format(text, ec), null);
        if (mc != null) outbox.sms(collegeId, countryCode, mobile, String.format(text, mc), null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("verificationId", id);
        r.put("email", app.swarsetu.common.Check.mask(email));
        if (mobile != null) r.put("mobile", "••••••" + mobile.substring(6));
        r.put("expiresInMinutes", props.codeMinutes());
        if (emailSender.simulated() || (mobile != null && smsSender.simulated())) {
            r.put("simulated", "Email or SMS is not configured on this server; codes are written to the server log.");
        }
        return r;
    }

    /**
     * Checks the codes and consumes the verification.
     * requireBoth: email code and (if a mobile was given) mobile code must both match.
     * Otherwise either code is enough (used for password reset).
     */
    public Pending consume(String id, String purpose, String emailCode, String mobileCode, boolean requireBoth) {
        Map<String, Object> v = jdbc.sql("SELECT * FROM verifications WHERE id = ? AND purpose = ? FOR UPDATE")
            .param(id).param(purpose).query().listOfRows().stream().findFirst()
            .orElseThrow(() -> ApiException.bad("This code request was not found. Please request a new code."));
        if (v.get("consumed_at") != null) throw ApiException.bad("This code has already been used. Please request a new code.");
        Boolean expired = jdbc.sql("SELECT expires_at < NOW(3) FROM verifications WHERE id = ?").param(id).query(Boolean.class).single();
        if (Boolean.TRUE.equals(expired)) throw ApiException.bad("The code has expired. Please request a new code.");
        int attempts = ((Number) v.get("attempts")).intValue();
        if (attempts >= MAX_ATTEMPTS) throw ApiException.bad("Too many wrong codes. Please request a new code.");
        boolean emailOk = v.get("email_code_hash") != null && emailCode != null && Crypto.equalsConstantTime(hash(id, emailCode.trim()), (String) v.get("email_code_hash"));
        boolean mobileNeeded = v.get("mobile_code_hash") != null;
        boolean mobileOk = mobileNeeded && mobileCode != null && Crypto.equalsConstantTime(hash(id, mobileCode.trim()), (String) v.get("mobile_code_hash"));
        boolean ok = requireBoth ? emailOk && (!mobileNeeded || mobileOk) : emailOk || mobileOk;
        if (!ok) {
            // Separate statement so the attempt is counted even though the caller's transaction will roll back.
            jdbc.sql("UPDATE verifications SET attempts = attempts + 1 WHERE id = ?").param(id).update();
            int left = MAX_ATTEMPTS - attempts - 1;
            throw new WrongCodeException(left > 0 ? "The code is not correct. " + left + " attempt(s) left." : "Too many wrong codes. Please request a new code.");
        }
        jdbc.sql("UPDATE verifications SET consumed_at = NOW(3) WHERE id = ?").param(id).update();
        Map<String, Object> payload = null;
        Object pj = v.get("payload");
        if (pj != null) {
            try {
                @SuppressWarnings("unchecked") Map<String, Object> m = json.readValue(pj.toString(), Map.class);
                payload = m;
            } catch (Exception e) { throw new IllegalStateException(e); }
        }
        return new Pending(id, purpose, (String) v.get("user_id"), payload, (String) v.get("email"), (String) v.get("mobile"));
    }

    private String hash(String id, String code) { return Crypto.hmacHex(props.jwtSecret(), "otp:" + id + ":" + code); }

    /** Wrong code: the attempt counter must survive, so callers must not roll it back (see noRollbackFor). */
    public static class WrongCodeException extends ApiException {
        public WrongCodeException(String m) { super(HttpStatus.BAD_REQUEST, m); }
    }
}
