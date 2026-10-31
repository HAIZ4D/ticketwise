package com.haizad.ticketwise.ticket.dto;

import com.haizad.ticketwise.ticket.TicketStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
		@NotNull TicketStatus status,
		@NotBlank @Email @Size(max = 255) String changedBy) {
}
