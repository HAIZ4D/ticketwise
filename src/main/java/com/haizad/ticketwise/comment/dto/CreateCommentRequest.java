package com.haizad.ticketwise.comment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
		@NotBlank @Email @Size(max = 255) String authorEmail,
		@NotBlank @Size(max = 1000) String body) {
}
