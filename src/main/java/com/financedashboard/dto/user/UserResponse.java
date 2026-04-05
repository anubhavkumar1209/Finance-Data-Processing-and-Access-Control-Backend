package com.financedashboard.dto.user;

import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import com.financedashboard.entity.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        AccountStatus status
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }
}