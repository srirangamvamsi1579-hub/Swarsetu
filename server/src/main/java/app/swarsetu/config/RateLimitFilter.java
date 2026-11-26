package app.swarsetu.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits sign-in, registration and code requests per IP address (swarsetu.auth-rate-limit per minute, default 30).
 * In-memory: fine for one server. For several servers put the limit in Caddy or Redis.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000;
    private final Map<String, long[]> hits = new ConcurrentHashMap<>();
    private final int limit;

    public RateLimitFilter(@org.springframework.beans.factory.annotation.Value("${swarsetu.auth-rate-limit:30}") int limit) { this.limit = limit; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/auth/") && !req.getRequestURI().startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        long[] w = hits.compute(req.getRemoteAddr(), (k, v) -> {
            if (v == null || now - v[0] > WINDOW_MS) return new long[] {now, 1};
            v[1]++;
            return v;
        });
        if (w[1] > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", String.valueOf(Math.max(1, (WINDOW_MS - (now - w[0])) / 1000)));
            res.getWriter().write("{\"error\":\"Too many attempts. Please wait a minute and try again.\"}");
            return;
        }
        if (hits.size() > 50_000) hits.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW_MS);
        chain.doFilter(req, res);
    }
}
