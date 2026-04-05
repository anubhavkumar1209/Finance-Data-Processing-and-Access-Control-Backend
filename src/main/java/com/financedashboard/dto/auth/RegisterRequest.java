package com.financedashboard.dto.auth;

import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        Role role,
        AccountStatus status
) {
}
