package com.haizad.ticketwise.security.dto;

import com.haizad.ticketwise.user.dto.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
