package com.financedashboard.dto.user;

import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 100) String name,
        @Email @Size(max = 150) String email,
        @Size(min = 8, max = 100) String password,
        Role role,
        AccountStatus status
) {
}