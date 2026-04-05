package com.financedashboard.service;

import com.financedashboard.config.SelfRegistrationProperties;
import com.financedashboard.dto.auth.AuthResponse;
import com.financedashboard.dto.auth.LoginRequest;
import com.financedashboard.dto.auth.RegisterRequest;
import com.financedashboard.dto.user.UserResponse;
import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import com.financedashboard.entity.User;
import com.financedashboard.exception.DuplicateResourceException;
import com.financedashboard.repository.UserRepository;
import com.financedashboard.security.JwtService;
import com.financedashboard.security.UserPrincipal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SelfRegistrationProperties selfRegistrationProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        Role role = resolveRole(request);
        AccountStatus status = resolveStatus(request);

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .status(status)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered user {}", savedUser.getEmail());
        return toAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login credentials"));
        log.info("User logged in: {}", email);
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(token, "Bearer", UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Role resolveRole(RegisterRequest request) {
        Role role = request.role() == null ? selfRegistrationProperties.getDefaultRole() : request.role();
        if (!selfRegistrationProperties.getAllowedRoles().contains(role)) {
            throw new IllegalArgumentException("Self-registration cannot assign role: " + role);
        }
        return role;
    }

    private AccountStatus resolveStatus(RegisterRequest request) {
        AccountStatus status = request.status() == null ? selfRegistrationProperties.getDefaultStatus() : request.status();
        if (!selfRegistrationProperties.getAllowedStatuses().contains(status)) {
            throw new IllegalArgumentException("Self-registration cannot assign status: " + status);
        }
        return status;
    }
}
