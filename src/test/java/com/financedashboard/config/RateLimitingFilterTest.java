package com.financedashboard.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.getAuth().setMaxRequests(2);
        rateLimitProperties.getAuth().setWindowSeconds(60);
        rateLimitProperties.getApi().setMaxRequests(1);
        rateLimitProperties.getApi().setWindowSeconds(60);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-03T09:00:00Z"), ZoneOffset.UTC);
        rateLimitingFilter = new RateLimitingFilter(objectMapper, rateLimitProperties, fixedClock);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authRequestsAreRateLimitedPerIp() throws Exception {
        rateLimitingFilter.doFilter(loginRequest("192.168.1.10"), new MockHttpServletResponse(), filterChain);
        rateLimitingFilter.doFilter(loginRequest("192.168.1.10"), new MockHttpServletResponse(), filterChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(loginRequest("192.168.1.10"), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blockedResponse.getContentAsString()).contains("Rate limit exceeded");
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    void optionsRequestsDoNotConsumeRateLimitBudget() throws Exception {
        rateLimitingFilter.doFilter(optionsRequest("192.168.1.10"), new MockHttpServletResponse(), filterChain);
        rateLimitingFilter.doFilter(optionsRequest("192.168.1.10"), new MockHttpServletResponse(), filterChain);
        rateLimitingFilter.doFilter(optionsRequest("192.168.1.10"), new MockHttpServletResponse(), filterChain);

        verify(filterChain, times(3)).doFilter(any(), any());
    }

    @Test
    void apiRequestsUseSeparateBucketsPerAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("analyst.one@example.com", null, "ROLE_ANALYST"));
        rateLimitingFilter.doFilter(apiRequest(), new MockHttpServletResponse(), filterChain);

        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("analyst.two@example.com", null, "ROLE_ANALYST"));
        rateLimitingFilter.doFilter(apiRequest(), new MockHttpServletResponse(), filterChain);

        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("analyst.one@example.com", null, "ROLE_ANALYST"));
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(apiRequest(), blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentAsString()).contains("Too many api requests");
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    private MockHttpServletRequest loginRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private MockHttpServletRequest optionsRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/register");
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/financial-records");
        request.setRemoteAddr("10.10.10.10");
        return request;
    }
}
