package com.caioamorimr.ordermanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    private HttpServletRequest loginRequestFrom(String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn(ip);
        return request;
    }

    @Test
    @DisplayName("should allow requests under the limit to pass through")
    void shouldAllowRequestsUnderTheLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequestFrom("10.0.0.1"), response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(response, never()).setStatus(LoginRateLimitFilter.SC_TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("should return 429 and stop the chain once the same IP exceeds the limit")
    void shouldReturn429_whenLimitExceeded() throws Exception {
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequestFrom("10.0.0.2"), response, filterChain);
        }
        // 6th attempt within the same window must be blocked
        filter.doFilter(loginRequestFrom("10.0.0.2"), response, filterChain);

        verify(filterChain, times(5)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(response).setStatus(LoginRateLimitFilter.SC_TOO_MANY_REQUESTS);
        assertThat(body.toString()).contains("Too many login attempts");
    }

    @Test
    @DisplayName("should track each IP independently")
    void shouldTrackEachIpIndependently() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequestFrom("10.0.0.3"), response, filterChain);
        }
        // a different IP must not be affected by 10.0.0.3 having used up its quota
        filter.doFilter(loginRequestFrom("10.0.0.4"), response, filterChain);

        verify(filterChain, times(6)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(response, never()).setStatus(LoginRateLimitFilter.SC_TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("should not rate-limit requests to other endpoints")
    void shouldNotRateLimitOtherEndpoints() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/products");

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(10)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(response, never()).setStatus(LoginRateLimitFilter.SC_TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("should evict windows that fell outside the rate-limit window")
    void evictExpiredEntries_shouldRemoveStaleWindows() {
        long staleStart = System.currentTimeMillis() - Duration.ofMinutes(2).toMillis();
        filter.seedWindowForTesting("10.0.0.9", new LoginRateLimitFilter.AttemptWindow(staleStart));

        assertThat(filter.trackedIpCount()).isEqualTo(1);

        filter.evictExpiredEntries();

        assertThat(filter.trackedIpCount()).isZero();
    }

    @Test
    @DisplayName("should keep windows that are still within the rate-limit window")
    void evictExpiredEntries_shouldKeepFreshWindows() throws Exception {
        filter.doFilter(loginRequestFrom("10.0.0.10"), response, filterChain);

        assertThat(filter.trackedIpCount()).isEqualTo(1);

        filter.evictExpiredEntries();

        assertThat(filter.trackedIpCount()).isEqualTo(1);
    }
}
