package com.financedashboard.dto.auth;

import com.financedashboard.dto.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}