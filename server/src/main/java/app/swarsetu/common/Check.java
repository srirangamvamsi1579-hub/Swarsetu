package app.swarsetu.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Shared input rules (same as the web app). */
public final class Check {
    public static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    public static final Pattern MOBILE = Pattern.compile("^[0-9]{10}$");
    public static final Pattern MONTH = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");
    public static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._@-]{3,60}$");

    private Check() {}

    public static String email(String e, boolean gmailOnly) {
        if (e == null || !EMAIL.matcher(e.trim()).matches()) throw ApiException.bad("Enter a valid email address.");
        String v = e.trim().toLowerCase();
        if (gmailOnly && !v.endsWith("@gmail.com")) throw ApiException.bad("This college asks for a Gmail address (name@gmail.com).");
        return v;
    }

    public static String mobile(String m, boolean required) {
        if (m == null || m.isBlank()) {
            if (required) throw ApiException.bad("Mobile number is required.");
            return null;
        }
        String d = m.replaceAll("[\\s-]", "");
        if (!MOBILE.matcher(d).matches()) throw ApiException.bad("Mobile number must be 10 digits.");
        return d;
    }

    public static void password(String p) {
        if (p == null || p.length() < 8 || p.length() > 128 || !p.matches(".*[A-Za-z].*") || !p.matches(".*[0-9].*")) {
            throw ApiException.bad("Password must be 8 or more characters with at least one letter and one number.");
        }
    }

    public static LocalDate date(String s, String label) {
        try { return LocalDate.parse(s); } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.bad(label + " must be a date like 2026-11-25.");
        }
    }

    public static void range(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw ApiException.bad("The To date may not be earlier than the From date.");
    }

    public static String month(String m) {
        if (m == null || !MONTH.matcher(m).matches()) throw ApiException.bad("Month must look like 2026-11.");
        return m;
    }

    public static void https(String url, String label) {
        if (url != null && !url.isBlank() && !url.startsWith("https://")) throw ApiException.bad(label + " must start with https://");
    }

    public static String required(String s, String label) {
        if (s == null || s.isBlank()) throw ApiException.bad(label + " is required.");
        return s.trim();
    }

    public static String mask(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at <= 1 ? email : email.charAt(0) + "•••" + email.substring(at - 1);
    }
}
