package com.financedashboard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void inactiveUserTokenIsRejected() throws Exception {
        MockHttpServletRequest request = authorizedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-value")).thenReturn("viewer@example.com");
        when(userDetailsService.loadUserByUsername("viewer@example.com"))
                .thenReturn(new User("viewer@example.com", "password", false, true, true, true, Collections.emptyList()));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> exceptionCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(org.mockito.Mockito.eq(request), org.mockito.Mockito.eq(response), exceptionCaptor.capture());
        verify(filterChain, never()).doFilter(request, response);
        assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo("User account is inactive");
    }

    @Test
    void invalidTokenUsesAuthenticationEntryPoint() throws Exception {
        MockHttpServletRequest request = authorizedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-value")).thenThrow(new JwtException("bad token"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> exceptionCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(org.mockito.Mockito.eq(request), org.mockito.Mockito.eq(response), exceptionCaptor.capture());
        verify(filterChain, never()).doFilter(request, response);
        assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo("Invalid or expired JWT token");
    }

    @Test
    void missingUserForTokenUsesAuthenticationEntryPoint() throws Exception {
        MockHttpServletRequest request = authorizedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-value")).thenReturn("deleted@example.com");
        when(userDetailsService.loadUserByUsername("deleted@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> exceptionCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(org.mockito.Mockito.eq(request), org.mockito.Mockito.eq(response), exceptionCaptor.capture());
        verify(filterChain, never()).doFilter(request, response);
        assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo("Invalid or expired JWT token");
    }

    private MockHttpServletRequest authorizedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        return request;
    }
}
