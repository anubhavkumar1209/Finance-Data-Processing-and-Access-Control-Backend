package com.financedashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import com.financedashboard.entity.User;
import org.junit.jupiter.api.Test;

class UserPrincipalTest {

    @Test
    void analystRoleUsesSpringSecurityRolePrefix() {
        User user = User.builder()
                .id(7L)
                .name("Analyst")
                .email("analyst@example.com")
                .password("encoded-password")
                .role(Role.ANALYST)
                .status(AccountStatus.ACTIVE)
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(user);

        assertThat(userPrincipal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ANALYST");
    }

    @Test
    void inactiveUserIsDisabled() {
        User user = User.builder()
                .id(8L)
                .name("Inactive Viewer")
                .email("viewer@example.com")
                .password("encoded-password")
                .role(Role.VIEWER)
                .status(AccountStatus.INACTIVE)
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(user);

        assertThat(userPrincipal.isEnabled()).isFalse();
    }
}
