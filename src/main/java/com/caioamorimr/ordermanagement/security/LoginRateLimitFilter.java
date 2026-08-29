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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMITED_PATH = "/auth/login";
    private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    static final int SC_TOO_MANY_REQUESTS = 429;

    private final Map<String, AttemptWindow> attemptsByIp = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "login-rate-limit-cleanup");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    protected void initFilterBean() {
        cleanupExecutor.scheduleAtFixedRate(
                this::evictExpiredEntries, WINDOW_MILLIS, WINDOW_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdownNow();
    }

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
                window.count = 0;
            }
            window.count++;
            return window.count > MAX_ATTEMPTS_PER_WINDOW;
        }
    }

    void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        attemptsByIp.entrySet().removeIf(entry -> {
            AttemptWindow window = entry.getValue();
            synchronized (window) {
                return now - window.windowStartMillis > WINDOW_MILLIS;
            }
        });
    }

    int trackedIpCount() {
        return attemptsByIp.size();
    }

    void seedWindowForTesting(String ip, AttemptWindow window) {
        attemptsByIp.put(ip, window);
    }

    static final class AttemptWindow {
        private int count;
        private long windowStartMillis;

        AttemptWindow(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
