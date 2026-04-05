package com.financedashboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financedashboard.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String AUTH_LOGIN_PATH = "/api/auth/login";
    private static final String AUTH_REGISTER_PATH = "/api/auth/register";
    private static final long CLEANUP_INTERVAL = 500L;

    private final ObjectMapper objectMapper;
    private final RateLimitProperties rateLimitProperties;
    private final Clock clock;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = clock.millis();
        cleanupExpiredCounters(now);

        String identifier = resolveIdentifier(request);
        String counterKey = rule.name() + ":" + identifier;
        WindowCounter counter = counters.computeIfAbsent(counterKey, key -> new WindowCounter(now));
        RateLimitDecision decision = counter.tryConsume(now, rule.windowMillis(), rule.maxRequests());

        if (!decision.allowed()) {
            writeRateLimitExceeded(response, request, rule, decision.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (AUTH_LOGIN_PATH.equals(requestUri) || AUTH_REGISTER_PATH.equals(requestUri)) {
            return RateLimitRule.auth(rateLimitProperties.getAuth());
        }
        if (requestUri.startsWith("/api/")) {
            return RateLimitRule.api(rateLimitProperties.getApi());
        }
        return null;
    }

    private String resolveIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName();
        }
        return "ip:" + extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void writeRateLimitExceeded(HttpServletResponse response,
                                        HttpServletRequest request,
                                        RateLimitRule rule,
                                        long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                LocalDateTime.now(clock.withZone(ZoneId.systemDefault())),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Rate limit exceeded",
                request.getRequestURI(),
                List.of("Too many " + rule.name() + " requests. Try again later.")
        ));
    }

    private void cleanupExpiredCounters(long now) {
        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        long longestWindowMillis = Math.max(
                rateLimitProperties.getAuth().getWindowSeconds(),
                rateLimitProperties.getApi().getWindowSeconds()) * 1_000L;
        long staleBefore = now - (longestWindowMillis * 2);
        counters.entrySet().removeIf(entry -> entry.getValue().isStale(staleBefore));
    }

    private record RateLimitRule(String name, int maxRequests, long windowMillis) {

        private static RateLimitRule auth(RateLimitProperties.Rule rule) {
            return new RateLimitRule("authentication", rule.getMaxRequests(), rule.getWindowSeconds() * 1_000L);
        }

        private static RateLimitRule api(RateLimitProperties.Rule rule) {
            return new RateLimitRule("api", rule.getMaxRequests(), rule.getWindowSeconds() * 1_000L);
        }
    }

    private record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

        private static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0);
        }

        private static RateLimitDecision reject(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }

    private static final class WindowCounter {

        private long windowStartedAt;
        private int requests;

        private WindowCounter(long windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }

        private synchronized RateLimitDecision tryConsume(long now, long windowMillis, int maxRequests) {
            if (now - windowStartedAt >= windowMillis) {
                windowStartedAt = now;
                requests = 0;
            }

            if (requests >= maxRequests) {
                long retryAfterMillis = Math.max(1L, windowMillis - (now - windowStartedAt));
                long retryAfterSeconds = (retryAfterMillis + 999L) / 1_000L;
                return RateLimitDecision.reject(retryAfterSeconds);
            }

            requests++;
            return RateLimitDecision.permit();
        }

        private synchronized boolean isStale(long staleBefore) {
            return windowStartedAt < staleBefore;
        }
    }
}
