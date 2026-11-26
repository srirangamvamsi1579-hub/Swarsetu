package app.swarsetu.auth;

import app.swarsetu.common.ApiException;
import app.swarsetu.common.Crypto;
import app.swarsetu.config.AppProps;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short-lived access tokens (JWT, 15 min) and rotating refresh tokens (stored hashed).
 * Using an old refresh token again means it was stolen: the whole family is revoked.
 */
@Service
public class Tokens {
    private final JwtEncoder encoder;
    private final JdbcClient jdbc;
    private final AppProps props;

    public Tokens(JwtEncoder encoder, JdbcClient jdbc, AppProps props) { this.encoder = encoder; this.jdbc = jdbc; this.props = props; }

    public Map<String, Object> issue(String userId, String collegeId, String role, String family, String userAgent, String ip) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("swarsetu").subject(userId).issuedAt(now)
            .expiresAt(now.plus(props.accessMinutes(), ChronoUnit.MINUTES))
            .claim("col", collegeId).claim("role", role).build();
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        String refresh = Crypto.token();
        jdbc.sql("INSERT INTO refresh_tokens(user_id, token_hash, family, expires_at, user_agent, ip) VALUES (?,?,?, NOW(3) + INTERVAL ? DAY, ?, ?)")
            .param(userId).param(Crypto.sha256Hex(refresh)).param(family == null ? UUID.randomUUID().toString() : family)
            .param(props.refreshDays()).param(trim(userAgent, 200)).param(ip).update();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("accessToken", access);
        r.put("refreshToken", refresh);
        r.put("expiresIn", props.accessMinutes() * 60);
        return r;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> rotate(String refreshToken, String userAgent, String ip) {
        if (refreshToken == null || refreshToken.isBlank()) throw ApiException.unauthorized("Please sign in again.");
        Map<String, Object> t = jdbc.sql(
                "SELECT rt.id, rt.user_id, rt.family, rt.revoked_at, rt.expires_at < NOW(3) AS expired, u.college_id, u.role, u.status "
                + "FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id WHERE rt.token_hash = ? FOR UPDATE")
            .param(Crypto.sha256Hex(refreshToken)).query().listOfRows().stream().findFirst()
            .orElseThrow(() -> ApiException.unauthorized("Please sign in again."));
        if (t.get("revoked_at") != null) {
            revokeFamily((String) t.get("family"));
            throw ApiException.unauthorized("This session was ended for safety. Please sign in again.");
        }
        if (app.swarsetu.common.Rows.truthy(t.get("expired")) || !"active".equals(t.get("status"))) {
            throw ApiException.unauthorized("Please sign in again.");
        }
        Map<String, Object> issued = issue((String) t.get("user_id"), (String) t.get("college_id"), (String) t.get("role"),
            (String) t.get("family"), userAgent, ip);
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = NOW(3), replaced_by = (SELECT id FROM (SELECT id FROM refresh_tokens WHERE token_hash = ?) x) WHERE id = ?")
            .param(Crypto.sha256Hex((String) issued.get("refreshToken"))).param(t.get("id")).update();
        return issued;
    }

    public void revokeFamilyOf(String refreshToken) {
        if (refreshToken == null) return;
        jdbc.sql("SELECT family FROM refresh_tokens WHERE token_hash = ?").param(Crypto.sha256Hex(refreshToken))
            .query(String.class).optional().ifPresent(this::revokeFamily);
    }

    public void revokeFamily(String family) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = NOW(3) WHERE family = ? AND revoked_at IS NULL").param(family).update();
    }

    public void revokeAllForUser(String userId) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = NOW(3) WHERE user_id = ? AND revoked_at IS NULL").param(userId).update();
    }

    private static String trim(String s, int n) { return s == null ? null : s.length() > n ? s.substring(0, n) : s; }
}
