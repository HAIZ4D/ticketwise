package com.haizad.ticketwise.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReclassifyRequest(@NotBlank @Email @Size(max = 255) String changedBy) {
}
