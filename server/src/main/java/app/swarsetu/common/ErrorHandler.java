package app.swarsetu.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Turns errors into {"error": "..."} with a message the user can act on. Database rules become friendly text. */
@RestControllerAdvice
public class ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
    private static final Pattern DUP_KEY = Pattern.compile("for key '(?:[a-z_]+\\.)?([a-z_]+)'");
    private static final Pattern CHECK = Pattern.compile("Check constraint '([a-z_]+)'");
    private static final Map<String, String> MESSAGES = Map.ofEntries(
        Map.entry("uq_colleges_name_key", "A college with this name is already registered."),
        Map.entry("PRIMARY", "A record with this ID already exists."),
        Map.entry("uq_users_username", "That username is already taken."),
        Map.entry("uq_users_email", "That email address is already registered."),
        Map.entry("uq_users_mobile", "That mobile number is already registered."),
        Map.entry("uq_users_student", "That student record is already linked to another account."),
        Map.entry("uq_courses_name", "A course with this name already exists."),
        Map.entry("uq_batches_key", "This batch already exists."),
        Map.entry("uq_claim_ref", "This payment reference (UTR) has already been submitted."),
        Map.entry("ck_users_mobile", "Mobile number must be 10 digits."),
        Map.entry("ck_pay_amounts", "Amount paid cannot be more than the total fee, and amounts cannot be negative."),
        Map.entry("ck_pay_month", "Fee month must look like 2026-11."),
        Map.entry("ck_claim_ref", "UPI reference must be 12 digits; a bank reference is 6 to 22 letters or digits."),
        Map.entry("ck_claim_amount", "Amount must be more than 0 and at most 2,00,000."),
        Map.entry("ck_cal_dates", "The To date may not be earlier than the From date."),
        Map.entry("ck_paydet_upi", "UPI ID should look like name@bank."),
        Map.entry("ck_paydet_ifsc", "IFSC must be 11 characters, like SBIN0001234."),
        Map.entry("ck_paydet_acct", "Account number must be 9 to 18 digits."),
        Map.entry("ck_paydet_some", "Enter a UPI ID or a bank account."),
        Map.entry("ck_paydet_qr", "The uploaded scanner must pay the same UPI ID."),
        Map.entry("ck_batches_link", "Meeting links must start with https://"));

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException e) {
        return body(e.status(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        Map<String, Object> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("error", "Please correct the highlighted fields.");
        b.put("fields", fields);
        return ResponseEntity.badRequest().body(b);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "The request could not be read.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> tooBig(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too large (2 MB at most).");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException e) {
        String m = String.valueOf(e.getMostSpecificCause().getMessage());
        Matcher d = DUP_KEY.matcher(m);
        if (m.contains("Duplicate entry") && d.find()) {
            return body(HttpStatus.CONFLICT, MESSAGES.getOrDefault(d.group(1), "This record already exists."));
        }
        Matcher c = CHECK.matcher(m);
        if (c.find()) return body(HttpStatus.BAD_REQUEST, MESSAGES.getOrDefault(c.group(1), "A value is not allowed (" + c.group(1) + ")."));
        if (m.contains("Cannot delete or update a parent row")) {
            return body(HttpStatus.CONFLICT, "This record is still used by other records, so it cannot be deleted. Remove or move those first.");
        }
        if (m.contains("Cannot add or update a child row")) {
            return body(HttpStatus.BAD_REQUEST, "A linked record was not found in your college.");
        }
        log.warn("Unmapped integrity error: {}", m);
        return body(HttpStatus.CONFLICT, "The change breaks a data rule and was not saved.");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> denied(Exception e) {
        return body(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> other(Exception e) {
        log.error("Unexpected error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server. Please try again.");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus s, String msg) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("error", msg);
        return ResponseEntity.status(s).body(b);
    }
}
