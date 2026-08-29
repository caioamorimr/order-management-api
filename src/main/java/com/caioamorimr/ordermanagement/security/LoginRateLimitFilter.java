package com.caioamorimr.ordermanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple fixed-window rate limiter for {@code POST /auth/login}, keyed by client IP.
 * Kept as its own filter (rather than folded into {@link JwtAuthenticationFilter}) so
 * each filter has one job.
 * <p>
 * This is intentionally dependency-free (no Bucket4j/Redis): counters live in an
 * in-memory {@link ConcurrentHashMap}. That's a real limitation worth knowing —
 * counters reset on restart and aren't shared across instances, so this only protects
 * a single-instance deployment. A multi-instance deployment would need a shared store
 * (e.g. Redis) instead.
 * <p>
 * Also note: the client IP is read from {@link HttpServletRequest#getRemoteAddr()},
 * which is the direct TCP peer. Behind a reverse proxy/load balancer, that will be the
 * proxy's IP for every request unless {@code X-Forwarded-For} is parsed instead (and
 * trusted only from a known proxy).
 * <p>
 * Excluded under the "test" profile: this bean is a singleton with in-memory state, and
 * every {@code @SpringBootTest} that shares the same context configuration (most of
 * them do — same profile, same auto-config) shares this exact same instance and its
 * accumulated counters across every test method and every test class. A handful of
 * integration tests logging in during {@code @BeforeEach} would eventually push a
 * shared IP over the limit and start failing with an unrelated 429 — not a real bug in
 * whatever the test is checking, just test-state bleeding into a stateful bean. The
 * filter's own logic is exercised directly and thoroughly by {@code LoginRateLimitFilterTest}
 * without needing a Spring context at all, so nothing about the rate limiting logic
 * itself goes untested by turning it off here.
 */
@Component
@Profile("!test")
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMITED_PATH = "/auth/login";
    private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    /**
     * jakarta.servlet.http.HttpServletResponse predates RFC 6585 (2012) and has no
     * SC_TOO_MANY_REQUESTS constant — 429 is simply not part of the Servlet API's
     * status code list. Package-private so LoginRateLimitFilterTest can reference the
     * same constant instead of repeating the magic number.
     */
    static final int SC_TOO_MANY_REQUESTS = 429;

    private final Map<String, AttemptWindow> attemptsByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isLoginRequest(request) || !isRateLimited(request.getRemoteAddr())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(SC_TOO_MANY_REQUESTS);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"message\": \"Too many login attempts. Please try again in a minute.\"}"
        );
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && RATE_LIMITED_PATH.equals(request.getRequestURI());
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        AttemptWindow window = attemptsByIp.computeIfAbsent(clientIp, ip -> new AttemptWindow(now));

        synchronized (window) {
            if (now - window.windowStartMillis > WINDOW_MILLIS) {
                window.windowStartMillis = now;
                window.count.set(0);
            }
            return window.count.incrementAndGet() > MAX_ATTEMPTS_PER_WINDOW;
        }
    }

    private static final class AttemptWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartMillis;

        private AttemptWindow(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
