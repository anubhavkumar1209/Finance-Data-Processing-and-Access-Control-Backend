package com.financedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financedashboard.dto.auth.LoginRequest;
import com.financedashboard.dto.auth.RegisterRequest;
import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import com.financedashboard.entity.User;
import com.financedashboard.exception.DuplicateResourceException;
import com.financedashboard.repository.UserRepository;
import com.financedashboard.security.JwtService;
import com.financedashboard.config.SelfRegistrationProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private SelfRegistrationProperties selfRegistrationProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesViewerAccount() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123", null, null);
        User savedUser = User.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .password("encoded-password")
                .role(Role.VIEWER)
                .status(AccountStatus.ACTIVE)
                .build();

        mockSelfRegistrationPolicy(Role.VIEWER, AccountStatus.ACTIVE, List.of(Role.VIEWER), List.of(AccountStatus.ACTIVE));
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token-value");

        var response = authService.register(request);

        assertThat(response.token()).isEqualTo("token-value");
        assertThat(response.user().email()).isEqualTo("jane@example.com");
        assertThat(response.user().role()).isEqualTo(Role.VIEWER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerRejectsDisallowedRoleOrStatusInSelfRegistration() {
        RegisterRequest request = new RegisterRequest("Admin User", "admin@example.com", "password123", Role.ADMIN, AccountStatus.INACTIVE);

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(selfRegistrationProperties.getAllowedRoles()).thenReturn(List.of(Role.VIEWER));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Self-registration cannot assign role: ADMIN");
    }

    @Test
    void registerRejectsDuplicateEmailWithConflictException() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123", null, null);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    void loginReturnsJwtToken() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        User user = User.builder()
                .id(5L)
                .name("John")
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        var response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().name()).isEqualTo("John");
    }

    private void mockSelfRegistrationPolicy(Role defaultRole,
                                            AccountStatus defaultStatus,
                                            List<Role> allowedRoles,
                                            List<AccountStatus> allowedStatuses) {
        when(selfRegistrationProperties.getDefaultRole()).thenReturn(defaultRole);
        when(selfRegistrationProperties.getDefaultStatus()).thenReturn(defaultStatus);
        when(selfRegistrationProperties.getAllowedRoles()).thenReturn(allowedRoles);
        when(selfRegistrationProperties.getAllowedStatuses()).thenReturn(allowedStatuses);
    }
}
