package com.haizad.ticketwise.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
		@NotBlank @Size(min = 5, max = 120) String title,
		@NotBlank @Size(min = 10, max = 2000) String description) {
}
