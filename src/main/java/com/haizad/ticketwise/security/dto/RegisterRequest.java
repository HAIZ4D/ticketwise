package com.haizad.ticketwise.security.dto;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {

	private static final int BCRYPT_MAX_BYTES = 72;

	@JsonIgnore
	@AssertTrue(message = "password must be at most 72 bytes")
	public boolean isPasswordShortEnoughForBcrypt() {
		return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
	}

}
