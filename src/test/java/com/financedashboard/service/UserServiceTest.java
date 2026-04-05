package com.financedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financedashboard.dto.user.AdminUserRequest;
import com.financedashboard.dto.user.UserUpdateRequest;
import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import com.financedashboard.entity.User;
import com.financedashboard.exception.DuplicateResourceException;
import com.financedashboard.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserUsesRequestedRoleAndStatus() {
        AdminUserRequest request = new AdminUserRequest(
                "Analyst User",
                "analyst@example.com",
                "password123",
                Role.ANALYST,
                AccountStatus.ACTIVE);

        User savedUser = User.builder()
                .id(2L)
                .name("Analyst User")
                .email("analyst@example.com")
                .password("encoded-password")
                .role(Role.ANALYST)
                .status(AccountStatus.ACTIVE)
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = userService.createUser(request);

        assertThat(response.email()).isEqualTo("analyst@example.com");
        assertThat(response.role()).isEqualTo(Role.ANALYST);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        AdminUserRequest request = new AdminUserRequest(
                "Analyst User",
                "analyst@example.com",
                "password123",
                Role.ANALYST,
                AccountStatus.ACTIVE);

        when(userRepository.findByEmail("analyst@example.com")).thenReturn(Optional.of(User.builder()
                .id(10L)
                .email("analyst@example.com")
                .build()));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    void updateUserCanChangeRoleAndStatus() {
        User existingUser = User.builder()
                .id(2L)
                .name("Viewer User")
                .email("viewer@example.com")
                .password("encoded-password")
                .role(Role.VIEWER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = new UserUpdateRequest(
                "Updated Viewer",
                "updated.viewer@example.com",
                "newpassword123",
                Role.ANALYST,
                AccountStatus.INACTIVE);

        when(passwordEncoder.encode("newpassword123")).thenReturn("new-encoded-password");

        var response = userService.updateUser(2L, request);

        assertThat(response.name()).isEqualTo("Updated Viewer");
        assertThat(response.email()).isEqualTo("updated.viewer@example.com");
        assertThat(response.role()).isEqualTo(Role.ANALYST);
        assertThat(response.status()).isEqualTo(AccountStatus.INACTIVE);
    }
}
