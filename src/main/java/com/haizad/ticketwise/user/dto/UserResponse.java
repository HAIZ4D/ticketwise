package com.haizad.ticketwise.user.dto;

import java.time.Instant;

import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRole;

public record UserResponse(Long id, String email, UserRole role, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
	}

}
