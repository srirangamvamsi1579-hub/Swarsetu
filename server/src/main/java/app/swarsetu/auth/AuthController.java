package app.swarsetu.auth;

import app.swarsetu.auth.AuthDtos.*;
import app.swarsetu.common.CurrentUser;
import app.swarsetu.common.Me;
import app.swarsetu.common.Perms;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final AuthService auth;
    private final Tokens tokens;
    private final CurrentUser current;
    private final Perms perms;

    public AuthController(AuthService auth, Tokens tokens, CurrentUser current, Perms perms) {
        this.auth = auth; this.tokens = tokens; this.current = current; this.perms = perms;
    }

    /** Register a new college; its first user becomes the Admin. Step 1 of 2: sends codes. */
    @PostMapping("/api/auth/colleges")
    public Map<String, Object> newCollege(@Valid @RequestBody NewCollege r) { return auth.startCollege(r); }

    /** Faculty or student joins an existing college by code or exact name. Step 1 of 2. */
    @PostMapping("/api/auth/join")
    public Map<String, Object> join(@Valid @RequestBody Join r) { return auth.startJoin(r); }

    /** Step 2: enter the email (and mobile) codes. */
    @PostMapping("/api/auth/verify")
    public Map<String, Object> verify(@Valid @RequestBody Verify v, HttpServletRequest req) {
        return auth.verifyRegistration(v, req.getHeader("User-Agent"), req.getRemoteAddr());
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@Valid @RequestBody Login l, HttpServletRequest req) {
        return auth.login(l, req.getHeader("User-Agent"), req.getRemoteAddr());
    }

    @PostMapping("/api/auth/refresh")
    public Map<String, Object> refresh(@Valid @RequestBody Refresh r, HttpServletRequest req) {
        return tokens.rotate(r.refreshToken(), req.getHeader("User-Agent"), req.getRemoteAddr());
    }

    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(@RequestBody(required = false) Refresh r) {
        if (r != null) tokens.revokeFamilyOf(r.refreshToken());
        return Map.of("ok", true);
    }

    @PostMapping("/api/auth/forgot")
    public Map<String, Object> forgot(@Valid @RequestBody Forgot f) { return auth.forgot(f); }

    @PostMapping("/api/auth/reset")
    public Map<String, Object> reset(@Valid @RequestBody Reset r) {
        auth.reset(r);
        return Map.of("ok", true, "message", "Password changed. Please sign in with your new password.");
    }

    /** Lets the join screen confirm a college before registering. Exact match only — no listing. */
    @GetMapping("/api/public/colleges/lookup")
    public Map<String, Object> lookup(@RequestParam(required = false) String code, @RequestParam(required = false) String name) {
        Map<String, Object> c = auth.findCollege(code, name);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", c.get("id"));
        r.put("name", c.get("name"));
        r.put("city", c.get("city"));
        r.put("requireGmail", c.get("require_gmail"));
        return r;
    }

    // ---------- signed-in user ----------

    @GetMapping("/api/me")
    public Map<String, Object> me() {
        Me me = current.get();
        Map<String, Object> r = new LinkedHashMap<>(auth.userJson(me.id()));
        Map<String, Boolean> can = new LinkedHashMap<>();
        for (String p : Perms.ALL) can.put(p, perms.can(me, p));
        r.put("permissions", can);
        return r;
    }

    @PutMapping("/api/me")
    public Map<String, Object> updateProfile(@Valid @RequestBody Profile p) {
        Me me = current.get();
        auth.updateProfile(me, p);
        return auth.userJson(me.id());
    }

    @PostMapping("/api/me/password")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePassword c) {
        auth.changePassword(current.get(), c);
        return Map.of("ok", true, "message", "Password changed. Please sign in again on your other devices.");
    }

    @PostMapping("/api/me/contact")
    public Map<String, Object> changeContact(@Valid @RequestBody ChangeContact c) { return auth.startContactChange(current.get(), c); }

    @PostMapping("/api/me/contact/verify")
    public Map<String, Object> verifyContact(@Valid @RequestBody Verify v) { return auth.finishContactChange(current.get(), v); }
}
